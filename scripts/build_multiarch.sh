#!/usr/bin/env bash
# ==============================================================================
# SproutMemo 多架构镜像构建与发布脚本 (Buildx Multi-Arch)
# 支持平台: linux/amd64 (x86_64), linux/arm64 (aarch64), linux/arm/v7 (arm32)
# ==============================================================================
set -euo pipefail

# 默认发布标签为 0.3-multiarch，同时同步推送到 latest
VERSION="${1:-0.3-multiarch}"
REGISTRY="${DOCKER_REGISTRY:-crpi-tyyqcg8a2rpatesk.cn-shanghai.personal.cr.aliyuncs.com/zixidaxian}"
PLATFORMS="linux/amd64,linux/arm64,linux/arm/v7"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "${SCRIPT_DIR}")"

cd "${PROJECT_ROOT}"

echo "=================================================================="
echo "🚀 开始构建 SproutMemo 多架构镜像"
echo "📦 发布版本: ${VERSION} & latest"
echo "🌐 目标架构: ${PLATFORMS}"
echo "🏢 目标仓库: ${REGISTRY}"
echo "=================================================================="

# 1. 登录容器镜像服务 (若提供了 DOCKER_USERNAME 与 DOCKER_PASSWORD)
if [[ -n "${DOCKER_USERNAME:-}" && -n "${DOCKER_PASSWORD:-}" ]]; then
  echo "🔑 正在登录容器镜像服务..."
  echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin "${REGISTRY%%/*}"
else
  echo "ℹ️ 未提供 DOCKER_USERNAME/DOCKER_PASSWORD 环境变量，将使用当前 Docker 客户端已保存的登录凭证。"
fi

# 2. 检查并初始化 QEMU 多架构模拟器 (使用国内华为云 SWR 镜像加速)
echo "⚙️ 正在检查/注册 QEMU 模拟器..."
docker run --privileged --rm swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/tonistiigi/binfmt --install all || true

# 3. 检查并配置 Docker Buildx 构建器
BUILDER_NAME="sproutmemo-builder"
if ! docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^moby/buildkit:buildx-stable-1"; then
  echo "📥 正在加速拉取 Buildx 编译引擎..."
  docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/moby/buildkit:buildx-stable-1
  docker tag swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/moby/buildkit:buildx-stable-1 moby/buildkit:buildx-stable-1
  docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/moby/buildkit:buildx-stable-1 2>/dev/null || true
fi

if ! docker buildx ls | grep -q "${BUILDER_NAME}"; then
  echo "🔧 创建新的 Buildx 容器构建器: ${BUILDER_NAME}..."
  docker buildx create --name "${BUILDER_NAME}" \
    --driver docker-container \
    --bootstrap --use
else
  echo "🔧 切换到现有的 Buildx 容器构建器: ${BUILDER_NAME}..."
  docker buildx use "${BUILDER_NAME}"
fi

# 4. 构建并推送后端镜像 (sproutmemo-backend)
echo ""
echo "------------------------------------------------------------------"
echo "🔨 开始构建并推送多架构后端镜像: sproutmemo-backend"
echo "------------------------------------------------------------------"
docker buildx build \
  --platform "${PLATFORMS}" \
  -t "${REGISTRY}/sproutmemo-backend:${VERSION}" \
  -t "${REGISTRY}/sproutmemo-backend:latest" \
  --push \
  ./sproutmemo-api

# 5. 构建并推送前端 Web 镜像 (sproutmemo-web)
echo ""
echo "------------------------------------------------------------------"
echo "🔨 开始构建并推送多架构前端镜像: sproutmemo-web"
echo "------------------------------------------------------------------"
docker buildx build \
  --platform "${PLATFORMS}" \
  -t "${REGISTRY}/sproutmemo-web:${VERSION}" \
  -t "${REGISTRY}/sproutmemo-web:latest" \
  --push \
  ./sproutmemo-web

echo ""
echo "=================================================================="
echo "✅ 所有多架构镜像已成功构建并推送到阿里云容器镜像仓库！"
echo "📋 镜像清单核验 (Manifest Inspection):"
echo "=================================================================="
docker buildx imagetools inspect "${REGISTRY}/sproutmemo-backend:${VERSION}"
echo "------------------------------------------------------------------"
docker buildx imagetools inspect "${REGISTRY}/sproutmemo-web:${VERSION}"
echo "=================================================================="
