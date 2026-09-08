# ==================== Python RAG 服务镜像 ====================
FROM python:3.11

WORKDIR /app

# 换阿里云镜像源加速 apt
RUN if [ -f /etc/apt/sources.list.d/debian.sources ]; then \
        sed -i 's|deb.debian.org|mirrors.aliyun.com|g' /etc/apt/sources.list.d/debian.sources; \
    fi && \
    if [ -f /etc/apt/sources.list ]; then \
        sed -i 's|deb.debian.org|mirrors.aliyun.com|g' /etc/apt/sources.list; \
    fi

# 系统依赖（faiss 需要 libgomp；rapidocr/opencv 需要 libgl1 + libglib2.0-0）
# Debian trixie 中 libgl1 已提供 libGL.so.1，无需 libgl1-mesa-glx
RUN apt-get update && apt-get install -y --no-install-recommends \
    libglib2.0-0 \
    libgomp1 \
    libgl1 \
    && rm -rf /var/lib/apt/lists/*

# 网络超时兜底：国内镜像偶发响应慢，默认 15s read timeout 太短会误杀
ENV PIP_DEFAULT_TIMEOUT=120 \
    PIP_RETRIES=10 \
    PIP_NO_CACHE_DIR=1

# 先装依赖。
# 说明：本项目 RAG 为 CPU 推理（embedding/rerank 本机跑，LLM 走远端 vLLM），
# 因而不从 PyPI 装 torch——默认包会拉 nvidia 的 CUDA 依赖链（cudnn 等，单个 500MB+）。
# 且 PyTorch 官方 CDN (download-r2.pytorch.org) 在国内几乎不可用（实测 <10kB/s 并中断）。
# 因此从阿里云 pytorch 镜像拉 CPU 版 torch（--find-links + 显式 +cpu 强制 CPU 构建），依赖走阿里云 pypi。
COPY requirements.txt .
RUN pip install --index-url https://mirrors.aliyun.com/pypi/simple/ \
        --find-links https://mirrors.aliyun.com/pytorch-wheels/cpu/ \
        --timeout 120 --retries 10 "torch==2.4.1+cpu" \
    && pip install --timeout 120 --retries 10 -i https://mirrors.aliyun.com/pypi/simple/ -r requirements.txt

# 复制代码（不再内置索引文件；索引由 RAG_DATA_DIR 挂载卷持久化）
COPY rag_config.py rag_core.py main.py ./

EXPOSE 8001

# 健康检查（模型加载约 90 秒，start_period 给足时间）
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=120s \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8001/health')" || exit 1

CMD ["python", "main.py"]
