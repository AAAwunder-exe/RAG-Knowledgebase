# ==================== 前端多阶段构建 ====================
# Stage 1: Node 构建前端
FROM node:20-alpine AS builder

WORKDIR /app

# 复制依赖配置文件（利用 Docker 缓存）
COPY package*.json ./

# 安装依赖
RUN npm ci --legacy-peer-deps

# 复制源代码
COPY . .

# 构建生产版本（增加 Node 内存防止 vue-tsc OOM）
ENV NODE_OPTIONS=--max-old-space-size=1536
RUN npm run build

# Stage 2: Nginx 运行时
FROM nginx:alpine

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 从构建阶段复制静态文件
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
