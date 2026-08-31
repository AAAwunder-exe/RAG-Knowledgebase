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

# 先装依赖
COPY requirements.txt .
RUN pip install --timeout 120 --retries 10 -i https://pypi.tuna.tsinghua.edu.cn/simple -r requirements.txt

# 复制代码（不再内置索引文件；索引由 RAG_DATA_DIR 挂载卷持久化）
COPY rag_config.py rag_core.py main.py ./

EXPOSE 8001

# 健康检查（模型加载约 90 秒，start_period 给足时间）
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=120s \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8001/health')" || exit 1

CMD ["python", "main.py"]
