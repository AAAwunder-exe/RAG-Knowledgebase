"""
RAG 引擎配置文件
包含所有可配置的参数，通过环境变量或直接修改此文件来调整
"""

import os

# ========== 文本切分参数 ==========
CHUNK_SIZE = int(os.environ.get("CHUNK_SIZE", "800"))
CHUNK_OVERLAP = int(os.environ.get("CHUNK_OVERLAP", "200"))

# ========== 检索参数（优化后，支持Parent-Child机制） ==========
# 所有参数支持环境变量覆盖，优先使用新命名（DENSE_TOP_K / BM25_TOP_K），
# 向后兼容旧命名（DENSE_RETRIEVAL_TOP_K / SPARSE_RETRIEVAL_TOP_K）
# FAISS检索参数：检索更多候选以提高召回率
DENSE_TOP_K = int(os.environ.get("DENSE_TOP_K", os.environ.get("DENSE_RETRIEVAL_TOP_K", "15")))
# BM25检索参数：检索更多候选以提高关键词匹配覆盖率
BM25_TOP_K = int(os.environ.get("BM25_TOP_K", os.environ.get("SPARSE_RETRIEVAL_TOP_K", "15")))
# 混合融合参数：确保有足够的候选供精排
HYBRID_FUSION_TOP_K = int(os.environ.get("HYBRID_FUSION_TOP_K", "12"))
# Reranker精排参数：精排后保留最优结果
RERANKER_TOP_K = int(os.environ.get("RERANKER_TOP_K", "6"))
# 混合检索权重：RRF 融合时对 dense / BM25 得分的缩放系数
DENSE_WEIGHT = float(os.environ.get("DENSE_WEIGHT", "1.0"))
BM25_WEIGHT = float(os.environ.get("BM25_WEIGHT", "1.0"))
# 保留旧名作为别名，兼容已有 .env 配置
DENSE_RETRIEVAL_TOP_K = DENSE_TOP_K
SPARSE_RETRIEVAL_TOP_K = BM25_TOP_K

# ========== 模型参数 ==========
# EMBEDDING/RERANKER：默认用 HuggingFace 模型 ID，SentenceTransformer 会自动从
# 本地 HF 缓存（~/.cache/huggingface/hub/models--BAAI--...）加载；
# Docker 已挂载宿主机 HF cache 到 /root/.cache/huggingface，所以本地与容器内通用。
# 如需指定本地绝对路径，设环境变量 EMBEDDING_MODEL=/path/to/model 即可。
MODEL_DIR = os.environ.get("MODEL_DIR", "")  # 可选；为空时走 HF ID
EMBEDDING_MODEL = os.environ.get(
    "EMBEDDING_MODEL",
    f"{MODEL_DIR}\\BAAI\\bge-small-zh-v1.5" if MODEL_DIR else "BAAI/bge-small-zh-v1.5",
)
RERANKER_MODEL = os.environ.get(
    "RERANKER_MODEL",
    "d:\\models\\bge-reranker-base",
)
# LLM：优先 VLLM_*（与 docker-compose 命名一致），向后兼容 LLM_*
VLLM_BASE_URL = os.environ.get("VLLM_BASE_URL", os.environ.get("LLM_BASE_URL", "https://api.moonshot.cn/v1"))
VLLM_MODEL = os.environ.get("VLLM_MODEL", os.environ.get("LLM_MODEL", "kimi-k2.6"))
VLLM_API_KEY = os.environ.get("VLLM_API_KEY", os.environ.get("LLM_API_KEY", ""))
# 保留旧名作为别名，避免 rag_core.py 现有 LLM_* 引用大量改动
LLM_BASE_URL = VLLM_BASE_URL
LLM_MODEL = VLLM_MODEL
LLM_API_KEY = VLLM_API_KEY

# ========== 缓存参数 ==========
CACHE_MAX_SIZE = int(os.environ.get("CACHE_MAX_SIZE", "1000"))
CACHE_TTL = int(os.environ.get("CACHE_TTL", "3600"))  # 秒

# ========== Redis 缓存参数 ==========
# 缓存后端：redis / memory（二选一）。容器内注入 REDIS_HOST=redis 自动走 Redis；
# 本地开发未配置 Redis 时可设为 memory 回退到进程内 dict 缓存
CACHE_BACKEND = os.environ.get("CACHE_BACKEND", "redis").lower()
REDIS_HOST = os.environ.get("REDIS_HOST", "127.0.0.1")
REDIS_PORT = int(os.environ.get("REDIS_PORT", "6379"))
REDIS_DB = int(os.environ.get("REDIS_DB", "0"))
REDIS_PASSWORD = os.environ.get("REDIS_PASSWORD", "")
REDIS_URL = os.environ.get("REDIS_URL", "")
# 缓存 key 前缀，区分不同应用/环境，避免混淆
REDIS_KEY_PREFIX = os.environ.get("REDIS_KEY_PREFIX", "rag:cache:")

# ========== 日志参数 ==========
# .upper() 兼容大小写写法（INFO / info / Info 都能识别）
LOG_LEVEL = os.environ.get("LOG_LEVEL", "INFO").upper()
LOG_FORMAT = os.environ.get("LOG_FORMAT", "%(asctime)s - %(name)s - %(levelname)s - %(message)s")

# ========== 路径配置 ==========
MODEL_DIR = os.environ.get("MODEL_DIR", "d:\\models")
KNOWLEDGE_DIR = os.environ.get("KNOWLEDGE_DIR", "knowledge")
INDEX_PATH = os.environ.get("INDEX_PATH", "knowledge.index")
CHUNKS_PATH = os.environ.get("CHUNKS_PATH", "chunks.pkl")
META_PATH = os.environ.get("META_PATH", "meta.pkl")
TOKENIZED_CORPUS_PATH = os.environ.get("TOKENIZED_CORPUS_PATH", "tokenized_corpus.pkl")

# ========== 性能参数 ==========
MAX_CONCURRENT_REQUESTS = int(os.environ.get("MAX_CONCURRENT_REQUESTS", "10"))
REQUEST_TIMEOUT = int(os.environ.get("REQUEST_TIMEOUT", "120"))  # 秒

# ========== Parent-Child机制参数 ==========
PARENT_CHILD_SMALL_CHUNK_RATIO = float(os.environ.get("PARENT_CHILD_SMALL_CHUNK_RATIO", "0.7"))  # 融合时小Chunk比例
PARENT_CHILD_SMALL_CHUNK_WEIGHT = float(os.environ.get("PARENT_CHILD_SMALL_CHUNK_WEIGHT", "1.2"))  # 小Chunk权重系数

# ========== 功能开关 ==========
ENABLE_BM25 = os.environ.get("ENABLE_BM25", "true").lower() == "true"
ENABLE_QUERY_REWRITE = os.environ.get("ENABLE_QUERY_REWRITE", "true").lower() == "true"
ENABLE_PARENT_CHILD = os.environ.get("ENABLE_PARENT_CHILD", "true").lower() == "true"
ENABLE_RERANKER = os.environ.get("ENABLE_RERANKER", "true").lower() == "true"
ENABLE_CACHE = os.environ.get("ENABLE_CACHE", "true").lower() == "true"

# ========== 输出配置信息 ==========
def print_config():
    """打印当前配置信息"""
    print("=== RAG 引擎配置 ===")
    print(f"文本切分: CHUNK_SIZE={CHUNK_SIZE}, CHUNK_OVERLAP={CHUNK_OVERLAP}")
    print(f"检索参数: DENSE_TOP_K={DENSE_TOP_K}, BM25_TOP_K={BM25_TOP_K}, "
          f"HYBRID_FUSION_TOP_K={HYBRID_FUSION_TOP_K}, "
          f"RERANKER_TOP_K={RERANKER_TOP_K}")
    print(f"混合检索权重: DENSE_WEIGHT={DENSE_WEIGHT}, BM25_WEIGHT={BM25_WEIGHT}")
    print(f"模型配置: EMBEDDING_MODEL={EMBEDDING_MODEL}, "
          f"RERANKER_MODEL={RERANKER_MODEL}, "
          f"LLM_BASE_URL={LLM_BASE_URL}, "
          f"LLM_MODEL={LLM_MODEL}")
    print(f"缓存配置: CACHE_MAX_SIZE={CACHE_MAX_SIZE}, CACHE_TTL={CACHE_TTL}")
    print(f"功能开关: ENABLE_BM25={ENABLE_BM25}, ENABLE_QUERY_REWRITE={ENABLE_QUERY_REWRITE}, "
          f"ENABLE_PARENT_CHILD={ENABLE_PARENT_CHILD}, ENABLE_RERANKER={ENABLE_RERANKER}, "
          f"ENABLE_CACHE={ENABLE_CACHE}")
    print(f"Parent-Child参数: PARENT_CHILD_SMALL_CHUNK_RATIO={PARENT_CHILD_SMALL_CHUNK_RATIO}, "
          f"PARENT_CHILD_SMALL_CHUNK_WEIGHT={PARENT_CHILD_SMALL_CHUNK_WEIGHT}")
    print(f"路径配置: MODEL_DIR={MODEL_DIR}, KNOWLEDGE_DIR={KNOWLEDGE_DIR}")
    print("===================")

def validate_config():
    """验证配置参数"""
    errors = []

    # 检查模型路径
    if MODEL_DIR and not os.path.exists(MODEL_DIR):
        errors.append(f"模型目录不存在: {MODEL_DIR}")

    # 检查知识库目录
    if not os.path.exists(KNOWLEDGE_DIR):
        errors.append(f"知识库目录不存在: {KNOWLEDGE_DIR}")

    # 检查API密钥（必须配置，不能为空）
    if not LLM_API_KEY:
        errors.append("请设置 LLM API 密钥: VLLM_API_KEY 或 LLM_API_KEY（写入 .env）")

    if errors:
        print("⚠️ 配置验证发现以下问题:")
        for error in errors:
            print(f"  - {error}")
        return False
    return True