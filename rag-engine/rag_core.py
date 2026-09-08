import os
import io
import time
import pickle
import threading
import zipfile
import logging
import json
from datetime import datetime
from typing import Any, Dict, List

import faiss
import numpy as np
import jieba
from sentence_transformers import SentenceTransformer, CrossEncoder
from openai import OpenAI
from rank_bm25 import BM25Okapi
from langchain_text_splitters import RecursiveCharacterTextSplitter
import fitz

# 导入配置文件
from rag_config import *

# 配置日志系统
logging.basicConfig(
    level=LOG_LEVEL,
    format=LOG_FORMAT,
    handlers=[
        logging.FileHandler(os.path.join(os.path.dirname(os.path.abspath(__file__)), "rag.log"), encoding='utf-8'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# RAG追踪日志
_rag_traces = []  # 存储完整的RAG执行追踪

# ========== 路径处理（基于本文件所在目录，避免工作目录依赖） ==========
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
# 索引数据目录：默认在 BASE_DIR；Docker 下用 RAG_DATA_DIR 指向挂载卷持久化
DATA_DIR = os.environ.get("RAG_DATA_DIR", BASE_DIR)
INDEX_PATH = os.path.join(DATA_DIR, "knowledge.index")
CHUNKS_PATH = os.path.join(DATA_DIR, "chunks.pkl")
META_PATH = os.path.join(DATA_DIR, "meta.pkl")
TOKENIZED_CORPUS_PATH = os.path.join(DATA_DIR, "tokenized_corpus.pkl")
KNOWLEDGE_DIR = os.path.join(BASE_DIR, "knowledge")

# ========== 大模型配置（从配置文件读取） ==========
VLLM_BASE_URL = LLM_BASE_URL
VLLM_MODEL = LLM_MODEL
VLLM_API_KEY = LLM_API_KEY

# ========== 文本切分参数（从配置文件读取） ==========
CHUNK_SIZE = CHUNK_SIZE
CHUNK_OVERLAP = CHUNK_OVERLAP
SMALL_CHUNK_SIZE = int(os.environ.get("SMALL_CHUNK_SIZE", "512"))
LARGE_CHUNK_SIZE = int(os.environ.get("LARGE_CHUNK_SIZE", "2048"))

# RAG追踪函数
def start_rag_trace(query: str, kb_id=None, conversation_id: str = "default") -> dict:
    """开始RAG执行追踪"""
    trace_id = f"rag_{datetime.now().strftime('%Y%m%d_%H%M%S_%f')}"
    trace = {
        "trace_id": trace_id,
        "timestamp": datetime.now().isoformat(),
        "query": query,
        "kb_id": kb_id,
        "conversation_id": conversation_id,
        "stages": {},
        "performance": {}
    }
    _rag_traces.append(trace)
    logger.info(f"[RAG_TRACE] {trace_id}: Started for query: {query}")
    return trace

def log_rag_stage(trace_id: str, stage_name: str, data: Any, performance: dict = None):
    """记录RAG执行阶段"""
    # 查找对应的trace
    trace = next((t for t in _rag_traces if t["trace_id"] == trace_id), None)
    if trace:
        trace["stages"][stage_name] = data
        if performance:
            trace["performance"][stage_name] = performance
        logger.info(f"[RAG_TRACE] {trace_id}: Completed stage {stage_name}")

def finish_rag_trace(trace_id: str, answer: str, total_time: float, success: bool = True):
    """结束RAG执行追踪"""
    trace = next((t for t in _rag_traces if t["trace_id"] == trace_id), None)
    if trace:
        trace["answer"] = answer
        trace["total_time"] = total_time
        trace["success"] = success
        trace["end_time"] = datetime.now().isoformat()
        logger.info(f"[RAG_TRACE] {trace_id}: Finished in {total_time:.2f}s, success={success}")

        # 保存追踪到文件
        trace_file = os.path.join(os.path.dirname(os.path.abspath(__file__)), "rag_traces.json")
        try:
            with open(trace_file, "a", encoding='utf-8') as f:
                json.dump(trace, f, ensure_ascii=False)
                f.write("\n")
        except Exception as e:
            logger.error(f"[RAG_TRACE] Failed to save trace: {e}")

def get_rag_traces(limit: int = 10) -> list:
    """获取最近的RAG执行追踪"""
    return _rag_traces[-limit:]

def clear_rag_traces():
    """清除RAG执行追踪历史"""
    _rag_traces.clear()
    logger.info("[RAG_TRACE] Cleared all traces")

def format_rag_trace(trace: dict) -> str:
    """格式化RAG执行追踪为可读文本"""
    formatted = f"=== RAG执行追踪 ===\n"
    formatted += f"追踪ID: {trace['trace_id']}\n"
    formatted += f"时间: {trace['timestamp']}\n"
    formatted += f"查询: {trace['query']}\n"
    formatted += f"知识库: {trace.get('kb_id', '全局')}\n"
    formatted += f"对话ID: {trace.get('conversation_id', 'default')}\n"

    # 性能信息
    if trace.get('performance'):
        formatted += "\n[性能指标]\n"
        for stage, perf in trace['performance'].items():
            if isinstance(perf, dict):
                formatted += f"  {stage}: {perf}\n"
            else:
                formatted += f"  {stage}: {perf}\n"

    # 阶段信息
    if trace.get('stages'):
        formatted += "\n[执行阶段]\n"
        for stage, data in trace['stages'].items():
            formatted += f"  {stage}: "
            if isinstance(data, (list, dict)):
                formatted += f"包含 {len(data) if isinstance(data, list) else len(data.keys())} 项数据\n"
            else:
                formatted += f"{str(data)[:100]}...\n" if len(str(data)) > 100 else f"{str(data)}\n"

    # 最终结果
    if trace.get('answer'):
        formatted += f"\n[最终回答]\n{trace['answer']}\n"

    if trace.get('total_time'):
        formatted += f"\n[总耗时]: {trace['total_time']:.2f}秒\n"

    if 'success' in trace:
        status = "成功" if trace['success'] else "失败"
        formatted += f"[执行状态]: {status}\n"

    return formatted

_state = {
    "loaded": False,
    "index": None,               # IndexIDMap2(IndexFlatL2)，每个向量有全局自增 id
    "chunks": [],                # list[str]，下标即向量 id（删除项保留空串占位，保证 id 与下标不偏移）
    "meta": [],                  # list[dict|None]，与 chunks 位置对应：完整的Chunk Metadata；删除项为 None
    "tokenized_corpus": [],      # list[list[str]]，与 chunks 位置对应（含占位空项）
    "embedder": None,
    "bm25": None,
    "reranker": None,
    "client": None,
    "load_time": 0.0,
    "error": None,
}

# 写操作锁：增量 add/delete/rebuild 需串行，避免并发写坏索引
_write_lock = threading.RLock()

# 问答缓存：{question: (answer, contexts)}
cache = {}

# 对话历史管理：用于Query Rewrite
_conversation_history = {}
_conversation_counter = {}


# ========== Redis 缓存后端 ==========
# 将问答缓存从进程内 dict 升级为可选的 Redis 缓存（跨进程/容器共享，支持 TTL）。
# 通过 CACHE_BACKEND 切换：redis（默认，连不上自动回退 memory）/ memory。
_redis_client = None
_redis_ok = False


def _get_redis() -> "redis.Redis | None":
    """惰性创建 Redis 客户端；连接失败时置 _redis_ok=False 并回退内存缓存。"""
    global _redis_client, _redis_ok
    if _redis_ok and _redis_client is not None:
        return _redis_client
    if CACHE_BACKEND != "redis":
        return None
    try:
        import redis
    except Exception:
        logger.warning("未安装 redis 包，回退进程内存缓存（pip install redis）")
        _redis_ok = False
        return None
    try:
        if REDIS_PASSWORD:
            _redis_client = redis.Redis(
                host=REDIS_HOST, port=REDIS_PORT, db=REDIS_DB,
                password=REDIS_PASSWORD, decode_responses=False,
            )
        else:
            _redis_client = redis.Redis(
                host=REDIS_HOST, port=REDIS_PORT, db=REDIS_DB, decode_responses=False,
            )
        _redis_client.ping()
        _redis_ok = True
        logger.info(f"✅ Redis 缓存已连接（{REDIS_HOST}:{REDIS_PORT}/{REDIS_DB}）")
    except Exception as e:
        _redis_ok = False
        logger.warning(f"⚠️ Redis 连接失败，回退进程内存缓存: {e}")
        _redis_client = None
    return _redis_client


def _cache_key(question: str, kb_id=None) -> str:
    """问答缓存 key。含 kb_id，不同知识库各自缓存。"""
    kb = kb_id if kb_id is not None else "global"
    return f"{REDIS_KEY_PREFIX}{kb}:{hash(question)}"


def cache_get(question: str, kb_id=None):
    """按 question+kb 取缓存。返回 (answer, contexts) 或 None。"""
    r = _get_redis()
    if r is not None:
        try:
            raw = r.get(_cache_key(question, kb_id))
            if raw is None:
                return None
            return pickle.loads(raw)
        except Exception:
            return None
    return cache.get(_cache_key(question, kb_id))


def cache_set(question: str, kb_id=None, value=None, ttl: int = None):
    """写入缓存。Redis 模式带 TTL；memory 模式受 CACHE_MAX_SIZE 限制。"""
    key = _cache_key(question, kb_id)
    ttl = ttl if ttl is not None else CACHE_TTL
    r = _get_redis()
    if r is not None:
        try:
            r.set(key, pickle.dumps(value, protocol=pickle.HIGHEST_PROTOCOL), ex=ttl)
            return
        except Exception:
            return
    # 内存回退：先插入再裁剪，避免超限
    cache[key] = value
    if len(cache) > CACHE_MAX_SIZE:
        oldest_keys = list(cache.keys())[: len(cache) - CACHE_MAX_SIZE]
        for k in oldest_keys:
            cache.pop(k, None)


# ========== 文本提取 ==========
_ocr_engine = None


def get_ocr():
    """OCR 引擎延迟加载，仅扫描版页面需要时才用"""
    global _ocr_engine
    if _ocr_engine is None:
        from rapidocr_onnxruntime import RapidOCR
        _ocr_engine = RapidOCR()
    return _ocr_engine


# OCR 渲染分辨率：扫描版整本打印 dpi 越高越慢且吃内存；
# CPU 容器下 200 会导致 485 页级大书 OCR 数十分钟/超时，故可配置下调。
OCR_DPI = int(os.environ.get("OCR_DPI", "144"))


def ocr_page(page) -> str:
    """对 PDF 页面做 OCR 识别，返回文本"""
    pix = page.get_pixmap(dpi=OCR_DPI)
    img_bytes = pix.tobytes("png")
    result, _ = get_ocr()(img_bytes)
    if not result:
        return ""
    return "\n".join(item[1] for item in result)


def extract_pdf(data: bytes) -> str:
    """提取 PDF 全文文本；无文本的页面尝试 OCR。
    - 按页插入 [[PAGE:N]] 标记，便于 split_text 解析页码写入 chunk metadata
    - 疑似章节标题（行末'章'/'节' 或以'第'开头含'章'/'节'）用 === line === 包裹
      交给 split_text 章节识别，避免本函数做章节切割丢正文
    """
    doc = fitz.open(stream=data, filetype="pdf")
    parts = []
    for page_no, page in enumerate(doc, start=1):
        text = page.get_text()
        if len(text.strip()) <= 10:
            text = ocr_page(page)
        if len(text.strip()) <= 10:
            continue
        # 页码标记，split_text 解析后写入 chunk.metadata['page']
        parts.append(f"\n[[PAGE:{page_no}]]\n")
        for line in text.split('\n'):
            line = line.strip()
            if not line:
                continue
            # 行级章节识别：疑似标题的行用 === 包裹，避免丢正文
            if line.endswith('章') or line.endswith('节') or \
               (line.startswith('第') and ('章' in line or '节' in line)):
                parts.append(f"=== {line} ===")
            else:
                parts.append(line)
    return "\n".join(parts)


def extract_docx(data: bytes) -> str:
    """提取 docx 全文文本（纯 zip 解析，不引额外依赖）"""
    try:
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            xml = z.read("word/document.xml").decode("utf-8", errors="ignore")
        # 段落分隔：</w:p> 换行；去所有标签
        xml = xml.replace("</w:p>", "\n").replace("</w:tbl>", "\n")
        import re
        text = re.sub(r"<[^>]+>", "", xml)
        return text.strip()
    except Exception:
        return ""


def extract_text(data: bytes, filename: str) -> str:
    """按文件类型提取全文文本"""
    name = (filename or "").lower()
    if name.endswith(".pdf"):
        return extract_pdf(data)
    if name.endswith(".docx"):
        return extract_docx(data)
    if name.endswith(".txt") or name.endswith(".md") or name.endswith(".markdown"):
        return data.decode("utf-8", errors="ignore")
    # 其他类型（doc 等）暂不支持解析
    return ""


def split_text(text: str) -> list[dict]:
    """结构化切分，识别章节并生成带Metadata的Chunk（支持Parent-Child机制）"""
    # 小Chunk用于检索（512字符）
    small_splitter = RecursiveCharacterTextSplitter(
        separators=["\n\n", "\n", "。", "；", "，", " ", ""],
        chunk_size=SMALL_CHUNK_SIZE,
        chunk_overlap=CHUNK_OVERLAP,
        length_function=len,
    )

    # 大Chunk提供完整上下文（2048字符）
    large_splitter = RecursiveCharacterTextSplitter(
        separators=["\n\n", "\n", "。", "；", "，", " ", ""],
        chunk_size=LARGE_CHUNK_SIZE,
        chunk_overlap=CHUNK_OVERLAP,
        length_function=len,
    )

    # 首先按章节分割（同时跟踪页码 [[PAGE:N]] 标记）
    chapter_splits = []
    current_chapter = {"title": "无标题", "content": [], "page": None}
    current_page = None

    for line in text.split('\n'):
        line = line.strip()
        if not line:
            continue

        # 解析页码标记
        if line.startswith("[[PAGE:") and line.endswith("]]"):
            try:
                current_page = int(line[7:-2])
            except ValueError:
                pass
            continue

        # 简单的章节识别（可根据实际需求调整）
        if line.startswith('=== ') and line.endswith(' ==='):
            # 新章节开始，保存前一个章节
            if current_chapter["content"]:
                chapter_splits.append(current_chapter)
            current_chapter = {"title": line[4:-4], "content": [], "page": current_page}  # 去掉 === 标记
        else:
            current_chapter["content"].append(line)

    # 添加最后一个章节
    if current_chapter["content"]:
        chapter_splits.append(current_chapter)

    # 在每个章节内进行切分
    structured_chunks = []
    chunk_id = 0

    for chapter_idx, chapter in enumerate(chapter_splits):
        chapter_title = chapter["title"]
        chapter_page = chapter.get("page")  # 章节级页码（章节起始页）
        chapter_content = "\n".join(chapter["content"])

        # 对章节内容进行大Chunk切分（提供完整上下文）
        large_chunks = large_splitter.split_text(chapter_content)

        for large_idx, large_chunk in enumerate(large_chunks):
            large_chunk_id = chunk_id  # 当前大Chunk的全局id，供其下属小Chunk作parent_id
            # 生成大Chunk的Metadata
            large_metadata = {
                "document_id": None,  # 将在add_document_file中设置
                "document_name": None,  # 将在add_document_file中设置
                "subject": None,  # 知识库名称，将在add_document_file中设置
                "chapter": chapter_title,
                "section": f"大Chunk_{large_idx+1}",
                "page": chapter_page,  # 章节级页码（章节起始页）
                "chunk_id": chunk_id,
                "parent_id": None,  # 大Chunk没有父Chunk
                "text": large_chunk,
                "chunk_type": "large"
            }

            structured_chunks.append(large_metadata)
            chunk_id += 1

            # 对大Chunk内容进行小Chunk切分（用于检索）
            small_chunks = small_splitter.split_text(large_chunk)

            for small_idx, small_chunk in enumerate(small_chunks):
                # 生成小Chunk的Metadata
                small_metadata = {
                    "document_id": None,  # 将在add_document_file中设置
                    "document_name": None,  # 将在add_document_file中设置
                    "subject": None,  # 知识库名称，将在在add_document_file中设置
                    "chapter": chapter_title,
                    "section": f"小Chunk_{large_idx+1}_{small_idx+1}",
                    "page": chapter_page,  # 章节级页码（章节起始页）
                    "chunk_id": chunk_id,
                    "parent_id": large_chunk_id,  # 指向对应的大Chunk
                    "text": small_chunk,
                    "chunk_type": "small"
                }

                structured_chunks.append(small_metadata)
                chunk_id += 1

    return structured_chunks


# ========== 索引持久化 ==========
def _rebuild_bm25():
    """根据 tokenized_corpus 重建 BM25 索引（含占位空项，get_scores 结果长度仍与全局 id 对齐）"""
    corpus = _state["tokenized_corpus"]
    # 空语料时构建 BM25 会 ZeroDivisionError，仅在非空时构建（入库后 add_document_file 会重建）
    if not corpus:
        _state["bm25"] = None
        return
    _state["bm25"] = BM25Okapi(corpus)


def _persist():
    """把内存索引落盘（支持结构化Metadata）"""
    faiss.write_index(_state["index"], INDEX_PATH)
    with open(CHUNKS_PATH, "wb") as f:
        pickle.dump(_state["chunks"], f)
    with open(META_PATH, "wb") as f:
        pickle.dump(_state["meta"], f)
    with open(TOKENIZED_CORPUS_PATH, "wb") as f:
        pickle.dump(_state["tokenized_corpus"], f)


def _new_idmap(dimension: int):
    """新建可删除/可带 ID 的 FAISS 索引"""
    return faiss.IndexIDMap2(faiss.IndexFlatL2(dimension))


# ========== 全量重建 ==========
def rebuild_from_folder(kb_id):
    """从 knowledge/ 目录全量重建索引（用于初始迁移 / 兜底）。
    目录下所有 PDF 视为属于同一个知识库 kb_id，doc_id 用文件名。
    返回 chunk 总数。"""
    with _write_lock:
        if not os.path.exists(KNOWLEDGE_DIR):
            os.makedirs(KNOWLEDGE_DIR)
        files = sorted(f for f in os.listdir(KNOWLEDGE_DIR) if f.endswith(".pdf"))
        if not files:
            print("⚠️ knowledge 目录下没有 PDF，请先放入教材后重建")
            return 0

        dimension = _state["embedder"].get_sentence_embedding_dimension()
        index = _new_idmap(dimension)
        chunks, metas, tokenized = [], [], []
        cid = 0
        for file in files:
            print(f"  解析 {file} ...")
            with open(os.path.join(KNOWLEDGE_DIR, file), "rb") as f:
                text = extract_pdf(f.read())
            if not text.strip():
                print(f"  ⚠️ {file} 未提取到文本，跳过")
                continue
            file_chunks = split_text(text)
            if not file_chunks:
                continue
            # split_text 返回 list[dict]，必须取 text 字段传给 encoder 和 jieba
            chunk_texts = [c["text"] for c in file_chunks]
            vecs = _state["embedder"].encode(chunk_texts)
            ids = np.arange(cid, cid + len(file_chunks), dtype=np.int64)
            index.add_with_ids(vecs, ids)
            doc_id_str = str(file)  # 全量重建无 doc_id，用文件名作 doc_id，统一字符串
            subject = os.path.splitext(file)[0]  # 用文件名作为 subject
            for c in file_chunks:
                c["document_id"] = doc_id_str
                c["document_name"] = file
                c["subject"] = subject
                c["kb_id"] = int(kb_id)  # 供 hybrid_search 按知识库过滤使用
                chunks.append(c["text"])
                # 与 add_document_file 一致：meta 存完整 chunk dict（含 chapter/section/page/parent_id 等）
                metas.append(c)
                tokenized.append(list(jieba.cut(c["text"])))
            cid += len(file_chunks)
            print(f"  {file} -> {len(file_chunks)} 个片段")

        _state["index"] = index
        _state["chunks"] = chunks
        _state["meta"] = metas
        _state["tokenized_corpus"] = tokenized
        _rebuild_bm25()
        _persist()
        print(f"✅ 重建完成，共 {cid} 个片段，已绑定知识库 kb_id={kb_id}")
        return cid


# ========== 增量增删 ==========
def add_document_file(kb_id: int, doc_id, data: bytes, filename: str, title: str = ""):
    """增量加入一个文档（支持结构化Chunk）。
    1. 若 doc_id 已存在则先删除（幂等）
    2. 提取文本 → 结构化切分 → 向量化 → add_with_ids → 更新 BM25 → 落盘
    返回 (chunk_count, extracted_text)。extracted_text 供 Java 存全文预览。"""
    with _write_lock:
        delete_document(doc_id, _persist_after=False)

        text = extract_text(data, filename)
        if not text.strip():
            print(f"⚠️ 文档 {filename} 未能提取到文本，未入库")
            return 0, ""

        # 结构化切分，返回带Metadata的Chunk列表
        structured_chunks = split_text(text)
        if not structured_chunks:
            return 0, text

        # 提取纯文本用于向量化
        chunk_texts = [chunk["text"] for chunk in structured_chunks]
        vecs = _state["embedder"].encode(chunk_texts)

        # 全局自增 id：由于删除只置空占位不缩 list，next_id 恒等于当前 meta 长度
        base = len(_state["meta"])
        ids = np.arange(base, base + len(structured_chunks), dtype=np.int64)
        _state["index"].add_with_ids(vecs, ids)

        doc_id_str = str(doc_id)
        # subject 优先用 title（Java 文档表带标题），无 title 时退化为 filename 去扩展名
        subject = title.strip() if (title and title.strip()) else os.path.splitext(filename or "")[0]
        for chunk in structured_chunks:
            # 更新Metadata中的文档信息
            # document_id 统一存字符串，与 delete_document 比较保持一致
            chunk["document_id"] = doc_id_str
            chunk["document_name"] = filename
            chunk["subject"] = subject  # 真实 subject（文档标题或文件名）
            chunk["kb_id"] = int(kb_id)  # 供 hybrid_search 按知识库过滤使用

            _state["chunks"].append(chunk["text"])
            _state["meta"].append(chunk)  # 存储完整的Metadata
            _state["tokenized_corpus"].append(list(jieba.cut(chunk["text"])))

        _rebuild_bm25()
        _persist()
        print(f"✅ 文档已入库: {filename} (kb_id={kb_id}, doc_id={doc_id}, {len(structured_chunks)} 个片段)")
        return len(structured_chunks), text


def delete_document(doc_id, _persist_after=True):
    """增量删除一个文档的全部 chunk（支持结构化Metadata）。
    FAISS remove_ids 移除向量；chunks/meta/tokenized 置空占位（保 id 与下标对齐）。"""
    with _write_lock:
        doc_id_str = str(doc_id)
        # 防御性 str()：兼容历史索引中可能存的 int 型 document_id
        ids_to_remove = [i for i, m in enumerate(_state["meta"]) if m and str(m.get("document_id")) == doc_id_str]
        if not ids_to_remove:
            return 0
        arr = np.array(ids_to_remove, dtype=np.int64)
        _state["index"].remove_ids(faiss.IDSelectorBatch(arr))
        for i in ids_to_remove:
            _state["chunks"][i] = ""
            _state["meta"][i] = None
            _state["tokenized_corpus"][i] = []
        _rebuild_bm25()
        if _persist_after:
            _persist()
        print(f"🗑 已删除文档 {doc_id_str} 的 {len(ids_to_remove)} 个片段")
        return len(ids_to_remove)


# ========== 加载 ==========
def load():
    """加载所有索引和模型（首次调用耗时约 90 秒）。重复调用是空操作。"""
    if _state["loaded"]:
        return
    start_time = time.time()
    try:
        print("正在加载知识库与模型...")

        # 加载 FAISS 索引和文本片段；支持空索引启动（无 index 文件时初始化空 IDMap）
        # 后续通过 /index/add 或 rebuild 逐条入库填充
        if os.path.exists(INDEX_PATH):
            _state["index"] = faiss.read_index(INDEX_PATH)
            with open(CHUNKS_PATH, "rb") as f:
                _state["chunks"] = pickle.load(f)
        else:
            print("⚠️ 未检测到 knowledge.index，初始化空索引（待 /index/add 入库）")
            dimension = None
            try:
                # 无法从空索引得知维度，先加载 embedder 以确定向量维度
                _state["embedder"] = SentenceTransformer(EMBEDDING_MODEL)
                dimension = _state["embedder"].get_sentence_embedding_dimension()
            except Exception as e:
                print(f"⚠️ 初始化空索引导出 embedder 失败: {e}")
            if dimension is None:
                dimension = 512  # bge-small-zh 默认 512，后续 add_with_ids 会校验
            _state["index"] = _new_idmap(dimension)
            _state["chunks"] = []

        # 加载/初始化 meta（旧版索引没有 meta.pkl，需 rebuild）
        if os.path.exists(META_PATH):
            with open(META_PATH, "rb") as f:
                _state["meta"] = pickle.load(f)
        else:
            _state["meta"] = [None] * len(_state["chunks"])
            if _state["chunks"]:
                print("⚠️ 未检测到 meta.pkl（旧版索引）。如需按知识库隔离/增量管理，请调用 rebuild_from_folder 重建。")

        # 校验索引是否为 IDMap 家族（支持按 id 增删）
        try:
            is_idmap = bool(faiss.index_is_IDMap(_state["index"]))
        except AttributeError:  # 不同 faiss 版本 API 差异，兜底用 isinstance
            is_idmap = isinstance(_state["index"], (faiss.IndexIDMap, faiss.IndexIDMap2))
        if not is_idmap:
            print("⚠️ 当前 knowledge.index 不是 IDMap 索引，无法增量删除。请调用 rebuild_from_folder 重建。")

        # tokenized_corpus 缺失时（空索引）初始化为空列表
        if os.path.exists(TOKENIZED_CORPUS_PATH):
            with open(TOKENIZED_CORPUS_PATH, "rb") as f:
                _state["tokenized_corpus"] = pickle.load(f)
        else:
            _state["tokenized_corpus"] = []
        # 兼容：tokenized 长度与 chunks 不一致时补齐
        if len(_state["tokenized_corpus"]) < len(_state["chunks"]):
            for c in _state["chunks"][len(_state["tokenized_corpus"]):]:
                _state["tokenized_corpus"].append(list(jieba.cut(c)))

        # 加载向量模型（用于查询向量化）；空索引初始化时可能已加载，避免重复
        if _state.get("embedder") is None:
            _state["embedder"] = SentenceTransformer(EMBEDDING_MODEL)

        # 构建 BM25 关键词索引
        _rebuild_bm25()

        # 加载 Reranker 精排模型（首次约 1.1GB）
        print(f"正在加载 Reranker 精排模型 ({RERANKER_MODEL})，首次约 1.1GB...")
        _state["reranker"] = CrossEncoder(RERANKER_MODEL, max_length=512)

        # 配置大模型客户端（支持任意 OpenAI 兼容 API，通过环境变量切换）
        _state["client"] = OpenAI(
            api_key=VLLM_API_KEY,
            base_url=VLLM_BASE_URL,
        )

        _state["load_time"] = time.time() - start_time
        _state["loaded"] = True
        _state["error"] = None
        print(f"✅ 所有模型加载完成，耗时 {_state['load_time']:.2f} 秒")
    except Exception as e:
        _state["error"] = str(e)
        raise


def is_ready() -> bool:
    """健康检查：模型是否加载完成"""
    return _state["loaded"]


def get_status() -> dict:
    """获取详细状态，供 /health 接口返回"""
    r = _get_redis()
    return {
        "loaded": _state["loaded"],
        "load_time": round(_state["load_time"], 2),
        "chunks_count": len(_state["chunks"]),
        "vllm_base_url": VLLM_BASE_URL,
        "vllm_model": VLLM_MODEL,
        "cache_backend": "redis" if r is not None else "memory",
        "redis_connected": r is not None,
        "error": _state["error"],
    }


# ========== Query Rewrite ==========
def rewrite_query(query: str, conversation_id: str = "default") -> str:
    """
    查询重写：基于对话历史重写当前查询，处理多轮对话中的省略和指代问题

    Args:
        query: 当前查询
        conversation_id: 对话ID，用于区分不同对话

    Returns:
        重写后的查询
    """
    if not ENABLE_QUERY_REWRITE:
        return query

    # 如果没有对话历史，直接返回原查询
    if conversation_id not in _conversation_history or not _conversation_history[conversation_id]:
        return query

    # 获取最近的对话历史（最近2轮）
    recent_history = _conversation_history[conversation_id][-2:]

    if not recent_history:
        return query

    # 构造重写提示词
    history_text = "\n".join([f"用户: {item['user']}\n助手: {item['assistant']}" for item in recent_history])

    prompt = f"""你是查询重写专家。请根据对话历史，将当前查询重写为一个完整的独立查询。

【对话历史】
{history_text}

【当前查询】
{query}

【重写要求】
1. 处理省略：如果查询中有省略的主语、宾语等，根据上下文补充完整
2. 处理指代：将"它"、"这个"、"那个"等代词替换为具体的实体名称
3. 保持原意：不要改变查询的核心语义和意图
4. 独立完整：重写后的查询应该可以独立理解，不依赖历史对话

【重写后的查询】
"""

    try:
        # 调用大模型进行查询重写
        response = _state["client"].chat.completions.create(
            model=VLLM_MODEL,
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,  # 较低温度保证重写稳定性
            max_tokens=256,
            stream=False,
        )

        rewritten_query = response.choices[0].message.content.strip()

        # 如果重写结果太短或太长，可能重写失败，返回原查询
        if len(rewritten_query) < len(query) * 0.5 or len(rewritten_query) > len(query) * 3:
            return query

        return rewritten_query

    except Exception as e:
        print(f"查询重写失败: {e}，使用原查询")
        return query


def update_conversation_history(query: str, answer: str, conversation_id: str = "default"):
    """
    更新对话历史

    Args:
        query: 用户查询
        answer: 助手回答
        conversation_id: 对话ID
    """
    if conversation_id not in _conversation_history:
        _conversation_history[conversation_id] = []

    # 添加当前对话到历史
    _conversation_history[conversation_id].append({
        "user": query,
        "assistant": answer
    })

    # 限制历史长度，最多保留10轮对话
    if len(_conversation_history[conversation_id]) > 10:
        _conversation_history[conversation_id] = _conversation_history[conversation_id][-10:]

    # 更新对话计数器
    _conversation_counter[conversation_id] = _conversation_counter.get(conversation_id, 0) + 1


def clear_conversation_history(conversation_id: str = "default"):
    """清除指定对话的历史"""
    if conversation_id in _conversation_history:
        del _conversation_history[conversation_id]
    if conversation_id in _conversation_counter:
        del _conversation_counter[conversation_id]


def get_conversation_stats(conversation_id: str = "default") -> dict:
    """获取对话统计信息"""
    return {
        "conversation_id": conversation_id,
        "turn_count": _conversation_counter.get(conversation_id, 0),
        "history_length": len(_conversation_history.get(conversation_id, [])),
        "has_history": conversation_id in _conversation_history and len(_conversation_history[conversation_id]) > 0
    }


def generate_citations(answer: str, contexts: list[dict]) -> list[dict]:
    """
    生成引用信息，追踪回答的具体来源

    Args:
        answer: 生成的回答
        contexts: 检索到的上下文列表

    Returns:
        引用信息列表: [{"text": str, "metadata": dict, "score": float, "citation": str}, ...]
    """
    citations = []

    for i, context in enumerate(contexts):
        metadata = context.get("metadata", {})
        citation_info = {
            "index": i + 1,  # 引用编号
            "text": context.get("text", "")[:200] + "...",  # 来源文本预览
            "document_name": metadata.get("document_name", "未知文档"),
            "chapter": metadata.get("chapter", "无章节"),
            "section": metadata.get("section", "无片段"),
            "chunk_id": metadata.get("chunk_id", i),
            "score": context.get("score", 0.0),
            "source_type": metadata.get("chunk_type", "unknown"),
            "full_metadata": metadata
        }
        citations.append(citation_info)

    return citations


def format_citations(citations: list[dict]) -> str:
    """
    格式化引用信息为可读文本

    Args:
        citations: 引用信息列表

    Returns:
        格式化的引用文本
    """
    if not citations:
        return "无引用来源"

    citation_text = "\n【引用来源】\n"

    for citation in citations:
        citation_text += f"{citation['index']}. {citation['document_name']} - {citation['chapter']} ({citation['section']})\n"
        citation_text += f"   来源文本: {citation['text']}\n"
        citation_text += f"   相关度: {citation['score']:.4f}\n"

    return citation_text


def extract_citation_markers(answer: str, citations: list[dict]) -> list[dict]:
    """
    从回答中提取引用标记（如[1]、[2]等），并映射到具体引用

    Args:
        answer: 生成的回答
        citations: 引用信息列表

    Returns:
        提取的引用映射: [{"marker": "[1]", "citation_index": 1, "text": "..."}, ...]
    """
    import re

    # 查找所有引用标记
    citation_markers = re.findall(r'\[(\d+)\]', answer)

    extracted_citations = []

    for marker in citation_markers:
        citation_index = int(marker)
        if 1 <= citation_index <= len(citations):
            citation = citations[citation_index - 1]
            extracted_citations.append({
                "marker": f"[{citation_index}]",
                "citation_index": citation_index,
                "document_name": citation["document_name"],
                "chapter": citation["chapter"],
                "section": citation["section"]
            })

    return extracted_citations

# ========== 检索 ==========
def hybrid_search(query: str, kb_id=None, top_k: int = 3) -> list[dict]:
    """
    混合检索 + Reranker 精排（返回结构化Chunk，支持Parent-Child机制）
    1. FAISS 捞 DENSE_RETRIEVAL_TOP_K 个候选（可按 kb_id 过滤，优先使用小Chunk）
    2. BM25 捞 SPARSE_RETRIEVAL_TOP_K 个候选（可按 kb_id 过滤）
    3. RRF 融合取前 HYBRID_FUSION_TOP_K 个
    4. Reranker 精排取最终 Top-K
    5. 将小Chunk替换为对应的大Chunk提供完整上下文
    kb_id 为 None 时全局检索（兼容旧调用）。
    返回：[{"text": str, "metadata": dict}, ...]
    """
    # 阶段耗时埋点：定位响应慢的瓶颈环节
    _t = {"dense": 0.0, "bm25": 0.0, "rerank": 0.0}
    _t0 = time.time()
    # 配置参数（从 rag_config 导入，支持环境变量覆盖）
    dense_retrieval_top_k = DENSE_TOP_K
    sparse_retrieval_top_k = BM25_TOP_K
    hybrid_fusion_top_k = HYBRID_FUSION_TOP_K
    reranker_top_k = RERANKER_TOP_K
    dense_weight = DENSE_WEIGHT
    bm25_weight = BM25_WEIGHT
    # 指定知识库时，收集其 chunk id 集合（跳过占位项）
    kb_id_set = None
    if kb_id is not None:
        # 防御性 .get()：兼容旧索引 meta 中无 kb_id 字段的情况
        kb_id_set = {i for i, m in enumerate(_state["meta"])
                     if m and m.get("kb_id") == int(kb_id)}
        if not kb_id_set:
            return []

    # 第一步：向量检索
    query_vec = _state["embedder"].encode([query])
    if kb_id_set is not None:
        # 精确按知识库过滤。不同 faiss 版本参数 API 有差异：
        # 优先用 SearchParameters(sel=...)，不支持时退回全局检索 + Python 侧过滤
        try:
            selector = faiss.IDSelectorBatch(np.array(sorted(kb_id_set), dtype=np.int64))
            params = faiss.SearchParameters(sel=selector)
            _, faiss_indices = _state["index"].search(np.array(query_vec), dense_retrieval_top_k, params)
        except (TypeError, RuntimeError):
            _, all_indices = _state["index"].search(np.array(query_vec), 200)
            faiss_indices = [[int(i) for i in row if int(i) >= 0 and int(i) in kb_id_set][:dense_retrieval_top_k]
                             for row in all_indices]
    else:
        _, faiss_indices = _state["index"].search(np.array(query_vec), dense_retrieval_top_k)
    _t["dense"] = time.time() - _t0

    # 第二步：关键词检索（优先使用小Chunk提高检索精度）
    tokenized_query = list(jieba.cut(query))
    bm25_scores = _state["bm25"].get_scores(tokenized_query)
    _t_after_bm25 = time.time()
    if kb_id_set is not None:
        order = np.argsort(bm25_scores)[::-1]
        # 优先选择小Chunk，如果没有足够小Chunk再用大Chunk
        small_chunks_first = []
        other_chunks = []
        for i in order:
            if i not in kb_id_set:
                continue
            metadata = _state["meta"][i]
            if metadata and metadata.get("chunk_type") == "small":
                small_chunks_first.append(i)
            else:
                other_chunks.append(i)
        # 先取小Chunk，再补充其他Chunk
        bm25_indices = small_chunks_first[:sparse_retrieval_top_k]
        if len(bm25_indices) < sparse_retrieval_top_k:
            bm25_indices.extend(other_chunks[:sparse_retrieval_top_k - len(bm25_indices)])
    else:
        order = np.argsort(bm25_scores)[::-1]
        # 优先选择小Chunk
        small_chunks_first = []
        other_chunks = []
        for i in order:
            metadata = _state["meta"][i]
            if metadata and metadata.get("chunk_type") == "small":
                small_chunks_first.append(i)
            else:
                other_chunks.append(i)
        bm25_indices = small_chunks_first[:sparse_retrieval_top_k]
        if len(bm25_indices) < sparse_retrieval_top_k:
            bm25_indices.extend(other_chunks[:sparse_retrieval_top_k - len(bm25_indices)])

    # 第三步：RRF 粗排融合（给予小Chunk更高权重，dense/bm25 各自独立权重）
    scores = {}
    for rank, idx in enumerate(faiss_indices[0]):
        idx = int(idx)
        if idx < 0:  # 过滤条件不足时 FAISS 返回 -1
            continue
        # 检查是否为小Chunk，给予更高权重
        metadata = _state["meta"][idx]
        is_small = metadata and metadata.get("chunk_type") == "small"
        weight = PARENT_CHILD_SMALL_CHUNK_WEIGHT if is_small else 1.0
        scores[idx] = scores.get(idx, 0) + dense_weight * weight / (rank + 60)

    for rank, idx in enumerate(bm25_indices):
        # 检查是否为小Chunk，给予更高权重
        metadata = _state["meta"][idx]
        is_small = metadata and metadata.get("chunk_type") == "small"
        weight = PARENT_CHILD_SMALL_CHUNK_WEIGHT if is_small else 1.0
        scores[idx] = scores.get(idx, 0) + bm25_weight * weight / (rank + 60)

    # 取前 N 个候选送给精排（优先保留小Chunk）
    sorted_candidates = sorted(scores.keys(), key=lambda x: scores[x], reverse=True)

    # 按chunk_type分组
    small_chunk_candidates = []
    large_chunk_candidates = []
    for idx in sorted_candidates:
        metadata = _state["meta"][idx]
        if metadata and metadata.get("chunk_type") == "small":
            small_chunk_candidates.append(idx)
        else:
            large_chunk_candidates.append(idx)

    # 优先选择小Chunk，再补充大Chunk
    candidates = small_chunk_candidates[:int(hybrid_fusion_top_k * PARENT_CHILD_SMALL_CHUNK_RATIO)]
    if len(candidates) < hybrid_fusion_top_k:
        candidates.extend(large_chunk_candidates[:hybrid_fusion_top_k - len(candidates)])

    if not candidates:
        return []

    # 获取候选的完整信息（文本+Metadata）
    candidate_items = []
    for idx in candidates:
        if idx >= 0 and idx < len(_state["chunks"]):
            chunk_text = _state["chunks"][idx]
            metadata = _state["meta"][idx] if _state["meta"][idx] is not None else {}
            candidate_items.append({
                "text": chunk_text,
                "metadata": metadata,
                "id": idx
            })

    # 第四步：Reranker 精排（Cross-Encoder 打分）
    candidate_texts = [item["text"] for item in candidate_items]
    pairs = [[query, text] for text in candidate_texts]
    _rerank_start = time.time()
    rerank_scores = _state["reranker"].predict(pairs)
    _t["rerank"] = time.time() - _rerank_start
    # BM25 阶段耗时 =（get_scores 之后 - dense 结束）
    _t["bm25"] = _t_after_bm25 - _t0 - _t["dense"]

    # 组合结果并排序
    reranked_items = []
    for item, score in zip(candidate_items, rerank_scores):
        reranked_items.append({
            "text": item["text"],
            "metadata": item["metadata"],
            "score": score,
            "id": item["id"]
        })

    sorted_items = sorted(reranked_items, key=lambda x: x["score"], reverse=True)
    final_items = sorted_items[:reranker_top_k]

    # Parent-Child机制：将小Chunk替换为对应的大Chunk提供完整上下文，并去重
    # 同一 parent 只保留一次（保留 reranker 分数最高的那个）
    enhanced_items = []
    seen_parent_ids = set()

    def _to_parent(item):
        """若 small chunk 且 parent 有效，替换为 parent chunk；否则原样返回。"""
        m = item["metadata"]
        if m and m.get("chunk_type") == "small" and m.get("parent_id") is not None:
            pid = m["parent_id"]
            if pid >= 0 and pid < len(_state["meta"]):
                pm = _state["meta"][pid]
                if pm and pm.get("chunk_type") == "large":
                    return {
                        "text": _state["chunks"][pid],
                        "metadata": pm,
                        "score": item["score"],
                        "id": pid
                    }
        return item

    for item in final_items:
        final = _to_parent(item)
        # 记录 parent 占用，用于后续补充去重
        seen_parent_ids.add(final["id"])
        enhanced_items.append(final)

    # 去重后数量不足 reranker_top_k 时，从其余候选补充（跳过已占用的 parent）
    if len(enhanced_items) < reranker_top_k:
        for item in sorted_items[reranker_top_k:]:
            if len(enhanced_items) >= reranker_top_k:
                break
            final = _to_parent(item)
            if final["id"] in seen_parent_ids:
                continue
            seen_parent_ids.add(final["id"])
            enhanced_items.append(final)

    _total = time.time() - _t0
    logger.info(
        f"[检索耗时] 总={_total:.3f}s | 向量检索(dense)={_t['dense']:.3f}s | "
        f"BM25={_t['bm25']:.3f}s | Reranker精排={_t['rerank']:.3f}s | 候选={len(candidates)}"
    )
    return enhanced_items[:reranker_top_k]


def ask_question(question: str, top_k: int = 3, kb_id=None, return_contexts: bool = False,
                   conversation_id: str = "default", enable_rewrite: bool = False,
                   history: list[dict] = None):
    """问答函数（带缓存，支持结构化Chunk，支持多轮对话查询重写）
    - return_contexts=False: 返回 answer
    - return_contexts=True:  返回 (answer, contexts, cached)
    - conversation_id: 对话ID，用于区分不同对话
    - enable_rewrite: 是否启用查询重写
    - history: list[dict] = [{"role": "user"/"assistant", "content": str}, ...]，多轮上下文；
      传入非空 history 时不走缓存，确保每次结合前文独立生成
    contexts: list[dict] = [{"text": str, "metadata": dict, "score": float}, ...]
    """
    _qa_start = time.time()
    # 配置参数
    reranker_top_k = RERANKER_TOP_K
    has_history = bool(history)
    # 查询缓存（Redis / 内存，含 kb_id，不同知识库各自缓存）
    # 带多轮历史时不命中缓存，避免返回忽略语境的旧答案
    cached = False
    hit = None if has_history else cache_get(question, kb_id)
    if hit is not None:
        answer, contexts = hit
        cached = True
        if return_contexts:
            return answer, contexts, True
        return answer

    # 查询重写（如果启用）
    original_question = question
    if enable_rewrite and ENABLE_QUERY_REWRITE:
        question = rewrite_query(question, conversation_id)
        if question != original_question:
            print(f"[Query Rewrite] 原查询: {original_question}")
            print(f"[Query Rewrite] 重写后: {question}")

    # 检索
    docs = hybrid_search(question, kb_id=kb_id, top_k=reranker_top_k)
    if not docs:
        return "根据现有教材资料，无法确定该问题的答案。", [], cached
    # 构造带来源信息的上下文
    context_parts = []
    for doc in docs:
        metadata = doc["metadata"]
        source_info = f"来源：{metadata.get('document_name', '未知文档')}, "
        source_info += f"章节：{metadata.get('chapter', '无章节')}, "
        source_info += f"片段：{metadata.get('section', '无片段')}"
        context_parts.append(f"{source_info}\n\n{doc['text']}")

    context = "\n\n---\n\n".join(context_parts)

    # 多轮对话历史（作为上下文，帮助模型理解前文指代）
    history_text = ""
    if has_history:
        lines = []
        for h in history:
            role_label = "用户" if h.get("role") == "user" else "助手"
            lines.append(f"{role_label}: {h.get('content', '')}")
        history_text = "\n".join(lines)

    # 构造严谨的提示词（防止幻觉）
    history_block = f"""
【对话历史】
{history_text}
""" if has_history else ""
    prompt = f"""你是严谨的大学课程助教。请严格根据以下【教材原文】回答问题。
{history_block}
【教材原文】
{context}

【学生提问】
{question}

【回答要求】
1. 如果原文有答案，请用自己的话转述，并保持准确。
2. 如果原文没有明确答案，请直接回复："根据现有教材资料，无法确定该问题的答案。"
3. 严禁添加教材外的个人知识或猜测。
4. 回答时请引用具体的教材来源。
5. 请结合以上【对话历史】理解问题的语境，但最终回答必须基于【教材原文】。

【你的回答】
"""

    # 调用大模型
    _llm_start = time.time()
    response = _state["client"].chat.completions.create(
        model=VLLM_MODEL,
        messages=[{"role": "user", "content": prompt}],
        temperature=0.7,
        max_tokens=2048,
        stream=False,
    )
    _llm_time = time.time() - _llm_start
    logger.info(f"[LLM耗时] 大模型生成回答耗时={_llm_time:.3f}s")

    answer = response.choices[0].message.content
    logger.info(f"[问答总耗时] ask_question 全程={time.time() - _qa_start:.3f}s")

    # 更新对话历史（如果启用重写功能）
    if enable_rewrite:
        update_conversation_history(original_question, answer, conversation_id)

    if not has_history:
        cache_set(question, kb_id, (answer, docs))  # 缓存答案和上下文（Redis / 内存）

    if return_contexts:
        return answer, docs, False
    return answer
