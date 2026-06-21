FROM python:3.14-slim

WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# 安装 Python 依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制源码
COPY . .

# 数据持久化目录
VOLUME ["/app/images", "/data"]

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD curl -sf http://localhost:8000/api/health || exit 1

# 启动入口
CMD ["sh", "-c", "\
    python seed.py 2>/dev/null || true && \
    python -m uvicorn main:app --host 0.0.0.0 --port 8000 \
"]
