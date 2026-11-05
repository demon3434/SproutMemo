# Stage 1: Dependency builder stage (compiles C/Rust wheels)
FROM docker.m.daocloud.io/library/python:3.11-slim AS builder

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# Configure Debian Aliyun mirrors and install build dependencies
RUN sed -i 's/deb.debian.org/mirrors.aliyun.com/g; s/security.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list /etc/apt/sources.list.d/*.sources 2>/dev/null || true && \
    apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    g++ \
    libpq-dev \
    cargo \
    rustc \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

# Pre-compile wheels using Aliyun PyPI mirror
RUN pip wheel --no-cache-dir \
    -i https://mirrors.aliyun.com/pypi/simple/ \
    --trusted-host mirrors.aliyun.com \
    --wheel-dir /app/wheels \
    -r requirements.txt


# Stage 2: Final lightweight runtime stage
FROM docker.m.daocloud.io/library/python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PYTHONPATH=/app

WORKDIR /app

# Configure Debian Aliyun mirrors and install only required runtime libraries
RUN sed -i 's/deb.debian.org/mirrors.aliyun.com/g; s/security.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list /etc/apt/sources.list.d/*.sources 2>/dev/null || true && \
    apt-get update && apt-get install -y --no-install-recommends \
    libpq5 \
    ffmpeg \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Install pre-compiled wheels from builder stage
COPY --from=builder /app/wheels /app/wheels
RUN pip install --no-cache-dir /app/wheels/* && rm -rf /app/wheels

# Copy application source code
COPY . /app/

# OCI Image Metadata Labels
LABEL org.opencontainers.image.title="SproutMemo Backend" \
      org.opencontainers.image.description="知拾后端服务 (多架构支持: linux/amd64, linux/arm64, linux/arm/v7)" \
      org.opencontainers.image.vendor="SproutMemo"

# Expose FastAPI default port
EXPOSE 8000

# Start FastAPI server using uvicorn
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
