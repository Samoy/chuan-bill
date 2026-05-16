# 多阶段构建 - 构建阶段
FROM node:20-alpine AS builder
WORKDIR /app

# 安装 pnpm
RUN corepack enable

# 复制依赖文件
COPY package.json pnpm-lock.yaml ./

# 安装依赖
RUN pnpm install --frozen-lockfile

# 复制源代码
COPY . .

# 构建 H5 版本
RUN pnpm build:h5

# 多阶段构建 - 运行阶段
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# 清空默认的 nginx 内容
RUN rm -rf ./*

# 从构建阶段复制构建产物
COPY --from=builder /app/dist/build/h5 .

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
