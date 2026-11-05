# Stage 1: Build Web application (Fast native build on builder platform)
ARG NODE_BASE=swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/node:20-slim
FROM --platform=$BUILDPLATFORM ${NODE_BASE} AS build-stage

WORKDIR /app
COPY package*.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY . .
RUN npm run build

# Stage 2: Target-platform Nginx server
FROM docker.m.daocloud.io/library/nginx:alpine

COPY --from=build-stage /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# OCI Image Metadata Labels
LABEL org.opencontainers.image.title="SproutMemo Web" \
      org.opencontainers.image.description="知拾 Web 前端 (多架构支持: linux/amd64, linux/arm64, linux/arm/v7)" \
      org.opencontainers.image.vendor="SproutMemo"

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

