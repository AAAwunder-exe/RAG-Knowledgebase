from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, UploadFile, File, Form
from pydantic import BaseModel, Field

import rag_core


# ========== 生命周期：启动时加载所有模型 ==========
@asynccontextmanager
async def lifespan(app: FastAPI):
    # 同步阻塞加载（约 90 秒），加载完成前 /health 返回 503
    rag_core.load()
    yield


app = FastAPI(
    title="RAG Service",
    description="企业 AI 知识平台 RAG 引擎（FAISS + BM25 + Reranker + Kimi kimi-k2.6）",
    version="1.1.0",
    lifespan=lifespan,
)


# ========== 请求/响应模型 ==========
class HistoryItem(BaseModel):
    role: str = Field(..., description="消息角色：user 或 assistant")
    content: str = Field(..., description="消息内容")


class AskRequest(BaseModel):
    question: str = Field(..., description="用户问题")
    top_k: int = Field(3, ge=1, le=10, description="检索返回的片段数")
    kb_id: int | None = Field(None, description="知识库 ID，指定后只在该知识库内检索；不传则全局检索")
    history: list[HistoryItem] = Field(default_factory=list, description="多轮对话历史，作为上下文")


class SourceItem(BaseModel):
    """引用来源，对应真实检索 chunk，禁止 LLM 编造"""
    document_name: str
    chapter: str
    section: str
    page: int | None
    chunk_id: int
    parent_id: int | None
    chunk_type: str
    relevance_score: float


class AskResponse(BaseModel):
    answer: str = Field(..., description="大模型生成的回答")
    contexts: list[str] = Field(..., description="检索到的教材片段")
    sources: list[SourceItem] = Field(default_factory=list, description="回答的引用来源（对应真实检索 chunk）")
    cached: bool = Field(..., description="是否命中缓存")


class DeleteRequest(BaseModel):
    doc_id: int = Field(..., description="要删除的文档 ID（Java document 表主键）")


# ========== 健康检查接口（Java 侧服务发现用） ==========
@app.get("/health")
async def health():
    """服务就绪返回 200，加载中/出错返回 503"""
    if not rag_core.is_ready():
        raise HTTPException(status_code=503, detail="RAG service is loading")
    return {
        "status": "ok",
        **rag_core.get_status(),
    }


# ========== 问答接口 ==========
@app.post("/ask", response_model=AskResponse)
async def ask(req: AskRequest):
    if not rag_core.is_ready():
        raise HTTPException(status_code=503, detail="RAG service is loading")
    try:
        answer, contexts, cached = rag_core.ask_question(
            req.question, top_k=req.top_k, kb_id=req.kb_id,
            return_contexts=True, history=[
                {"role": h.role, "content": h.content} for h in req.history
            ],
        )
        # contexts: list[dict] = [{"text", "metadata", "score", "id"}, ...]
        # 仅取 text 字段保持与旧 AskResponse.contexts 兼容；完整结构提取为 sources
        contexts_str = []
        sources = []
        for doc in contexts:
            contexts_str.append(doc["text"] if isinstance(doc, dict) else str(doc))
            if isinstance(doc, dict):
                m = doc.get("metadata") or {}
                sources.append(SourceItem(
                    document_name=str(m.get("document_name", "未知文档")),
                    chapter=str(m.get("chapter", "无章节")),
                    section=str(m.get("section", "无片段")),
                    page=m.get("page"),
                    chunk_id=int(m.get("chunk_id", 0)),
                    parent_id=m.get("parent_id"),
                    chunk_type=str(m.get("chunk_type", "unknown")),
                    relevance_score=float(doc.get("score", 0.0) or 0.0),
                ))
        return AskResponse(answer=answer, contexts=contexts_str, sources=sources, cached=cached)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"RAG query failed: {e}")


# ========== 索引管理接口（Java 文档上传/删除时调用） ==========
@app.post("/index/add")
def index_add(
    file: UploadFile = File(...),
    kb_id: int = Form(...),
    doc_id: int = Form(...),
    title: str = Form(""),
):
    """增量加入一个文档：解析文本 → 切分 → 向量化 → 入库。
    返回 chunk 数和提取出的全文文本（供 Java 存全文预览）。"""
    if not rag_core.is_ready():
        raise HTTPException(status_code=503, detail="RAG service is loading")
    try:
        data = file.file.read()
        chunk_count, text = rag_core.add_document_file(kb_id, doc_id, data, file.filename or "", title)
        return {
            "chunk_count": chunk_count,
            "text": text,
            "file": file.filename or "",
        }
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"index add failed: {e}")


@app.post("/index/delete")
def index_delete(req: DeleteRequest):
    """增量删除一个文档的全部 chunk。"""
    if not rag_core.is_ready():
        raise HTTPException(status_code=503, detail="RAG service is loading")
    try:
        removed = rag_core.delete_document(req.doc_id)
        return {"doc_id": req.doc_id, "removed_chunks": removed}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"index delete failed: {e}")


@app.post("/index/rebuild")
def index_rebuild(kb_id: int | None = Form(None)):
    """从 knowledge/ 目录全量重建索引（初始迁移 / 兜底）。"""
    if not rag_core.is_ready():
        raise HTTPException(status_code=503, detail="RAG service is loading")
    try:
        kb = kb_id if kb_id is not None else 1
        total = rag_core.rebuild_from_folder(kb)
        return {"chunk_count": total}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"index rebuild failed: {e}")


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8001)
