# 小川记账项目文档实现计划

> **致代理工作者：** 必须使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 来逐任务实施此计划。步骤使用复选框 (`- [ ]`) 语法进行跟踪。

**目标：** 为小川记账项目编写完整的项目文档，包括 README、Dockerfile 和配置文件。

**架构：** 采用标准文档结构，分别为项目根目录、前端、后端编写 README，使用 Docker 多阶段构建部署，配置文件按环境分离。

**技术栈：** Markdown、Docker、Nginx、Spring Boot YAML

---

## 文件结构

在定义任务之前，先列出将要创建或修改的文件及其职责：

| 文件路径 | 职责 |
|---------|------|
| `chuan-bill-server/src/main/resources/application.yaml` | 公共配置 |
| `chuan-bill-server/src/main/resources/application-dev.yaml` | 开发环境配置 |
| `chuan-bill-server/src/main/resources/application-prod.yaml` | 生产环境配置 |
| `chuan-bill-server/README.md` | 后端项目文档 |
| `chuan-bill-app/README.md` | 前端项目文档 |
| `README.md` | 项目根目录文档 |
| `chuan-bill-server/Dockerfile` | 后端 Docker 镜像 |
| `chuan-bill-app/Dockerfile` | 前端 Docker 镜像 |
| `chuan-bill-app/nginx.conf` | Nginx 配置 |
| `docker-compose.yml` | Docker 编排 |

---

## Task 1: 编写后端配置文件

**文件：**
- Modify: `chuan-bill-server/src/main/resources/application.yaml`
- Create: `chuan-bill-server/src/main/resources/application-dev.yaml`
- Create: `chuan-bill-server/src/main/resources/application-prod.yaml`

- [ ] **Step 1: 重写 application.yaml 为公共配置**

```yaml
spring:
  application:
    name: chuan-bill-server
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
  quartz:
    job-store-type: memory

sa-token:
  token-name: token
  timeout: 2592000
  active-timeout: -1
  is-concurrent: true
  is-share: false
  token-style: random-64
  is-log: true

mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

preference:
  allowed-keys:
    - notification.master.enabled
    - notification.billReminder.enabled
    - notification.billReminder.time
    - notification.family.enabled
    - notification.system.enabled
```

- [ ] **Step 2: 创建 application-dev.yaml**

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/chuan_bill?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:123456}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      database: 0
      password: ${REDIS_PASSWORD:}
      timeout: 10s
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true

dashscope:
  api-key: ${DASHSCOPE_API_KEY}
  bill-recognition:
    app-id: ${DASHSCOPE_BILL_RECOGNITION_APP_ID}
  bill-analysis:
    app-id: ${DASHSCOPE_BILL_ANALYSIS_APP_ID}

wx:
  miniapp:
    appid: ${WX_MINIAPP_APPID}
    secret: ${WX_MINIAPP_SECRET}

qiniu:
  access_key: ${QINIU_ACCESS_KEY}
  secret_key: ${QINIU_SECRET_KEY}
  bucket: ${QINIU_BUCKET}
  cdn-domain: ${QINIU_CDN_DOMAIN}
  endpoint: ${QINIU_ENDPOINT}
```

- [ ] **Step 3: 创建 application-prod.yaml**

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:chuan_bill}?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
      database: 0
      password: ${REDIS_PASSWORD}
      timeout: 10s
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
          max-wait: 3000ms

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl

springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

dashscope:
  api-key: ${DASHSCOPE_API_KEY}
  bill-recognition:
    app-id: ${DASHSCOPE_BILL_RECOGNITION_APP_ID}
  bill-analysis:
    app-id: ${DASHSCOPE_BILL_ANALYSIS_APP_ID}

wx:
  miniapp:
    appid: ${WX_MINIAPP_APPID}
    secret: ${WX_MINIAPP_SECRET}

qiniu:
  access_key: ${QINIU_ACCESS_KEY}
  secret_key: ${QINIU_SECRET_KEY}
  bucket: ${QINIU_BUCKET}
  cdn-domain: ${QINIU_CDN_DOMAIN}
  endpoint: ${QINIU_ENDPOINT}
```

- [ ] **Step 4: 提交配置文件**

```bash
git add chuan-bill-server/src/main/resources/application*.yaml
git commit -m "chore(config): 拆分配置文件为 dev 和 prod 环境"
```

---

## Task 2: 编写后端 README.md

**文件：**
- Create: `chuan-bill-server/README.md`

- [ ] **Step 1: 创建后端 README.md**

```markdown
# 小川记账 - 后端

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-17-orange" alt="Java">
  <img src="https://img.shields.io/badge/MyBatis--Plus-3.5-blue" alt="MyBatis-Plus">
  <img src="https://img.shields.io/badge/Sa--Token-1.44-red" alt="Sa-Token">
</p>

## 简介

基于 Spring Boot 3 的记账应用后端服务

## 技术栈

- 框架：Spring Boot 3.5
- 语言：Java 17
- ORM：MyBatis-Plus 3.5
- 认证：Sa-Token 1.44
- 数据库：MySQL 8.0
- 缓存：Redis 6.0
- 文档：SpringDoc OpenAPI
- 工具：Hutool、Lombok

## 特性

- 分层架构：Controller → Service → Mapper → Entity
- 统一响应：Result<T> 包装器
- 权限认证：Sa-Token + Redis
- 软删除：deleted 字段 (0/1)
- 代码格式：Palantir Java Format (Spotless)
- API 文档：Swagger UI

## 快速开始

### 环境要求

- Java 17
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+

### 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE chuan_bill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入初始化脚本
mysql -u root -p chuan_bill < init.sql
```

### 配置

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑配置
vim .env
```

### 运行

```bash
# 开发环境运行
mvn spring-boot:run

# 或使用 Maven Wrapper
./mvnw spring-boot:run
```

### 构建

```bash
# 打包
mvn clean package

# 跳过测试打包
mvn clean package -DskipTests
```

## 项目结构

```
src/main/java/com/samoy/chuanbillserver/
├── controller/       # 控制器层
├── service/          # 服务层
│   ├── impl/         # 服务实现
├── mapper/           # 数据访问层
├── entity/           # 实体类
├── dto/              # 数据传输对象
├── vo/               # 视图对象
├── config/           # 配置类
├── interceptor/      # 拦截器
├── exception/        # 异常处理
├── util/             # 工具类
└── enums/            # 枚举类
```

## API 文档

启动后访问：http://localhost:8080/swagger-ui.html

## 开发规范

- 命名：驼峰命名法
- 注释：中文注释
- 格式：Palantir Java Format
- 提交：Conventional Commits

## 常用命令

```bash
# 代码格式检查
mvn spotless:check

# 代码格式化
mvn spotless:apply

# 运行测试
mvn test
```

## 相关文档

- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [MyBatis-Plus 文档](https://baomidou.com)
- [Sa-Token 文档](https://sa-token.cc)
```

- [ ] **Step 2: 提交后端 README**

```bash
git add chuan-bill-server/README.md
git commit -m "docs: 添加后端项目 README.md"
```

---

## Task 3: 编写前端 README.md

**文件：**
- Modify: `chuan-bill-app/README.md`

- [ ] **Step 1: 重写前端 README.md**

```markdown
# 小川记账 - 前端

<p align="center">
  <img alt="logo" src="https://starter.wot-ui.cn/logo.svg" width="200">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/uni--app-3.0-brightgreen" alt="uni-app">
  <img src="https://img.shields.io/badge/Vue-3-brightgreen" alt="Vue">
  <img src="https://img.shields.io/badge/TypeScript-5.5-blue" alt="TypeScript">
  <img src="https://img.shields.io/badge/wot--design--uni-1.14-orange" alt="wot-design-uni">
</p>

## 简介

基于 uni-app + Vue 3 + TypeScript 的跨平台记账应用前端

## 技术栈

- 框架：uni-app 3.0 + Vue 3.4
- 语言：TypeScript 5.5
- UI 组件：wot-design-uni 1.14
- 样式：UnoCSS
- 状态管理：Pinia
- 请求库：Alova.js
- 图表：ECharts

## 特性

- 跨平台：H5、微信小程序、APP
- 文件路由：基于 vite-plugin-uni-pages
- 组件自动导入：unplugin-auto-import
- 布局系统：vite-plugin-uni-layouts
- 主题定制：CSS 变量 + 暗黑模式

## 快速开始

### 环境要求

- Node.js >= 20.19.0
- pnpm >= 9.9.0

### 安装

```bash
# 安装依赖
pnpm install
```

### 开发

```bash
# H5 开发
pnpm dev

# 微信小程序开发
pnpm dev:mp-weixin

# APP 开发
pnpm dev:app
```

### 构建

```bash
# H5 构建
pnpm build:h5

# 微信小程序构建
pnpm build:mp-weixin

# APP 构建
pnpm build:app
```

## 项目结构

```
src/
├── pages/           # 主包页面
├── subPages/        # 分包页面
├── components/      # 公共组件
├── store/           # Pinia 状态
├── api/             # API 接口
├── layouts/         # 布局组件
├── utils/           # 工具函数
└── static/          # 静态资源
```

## 开发规范

- 组件：Vue 3 `<script setup>` 语法
- 样式：UnoCSS 优先，SCSS 仅用于复杂场景
- 状态：Pinia Store，use{Name}Store 命名
- 请求：Alova.js，自动导入
- 反馈：GlobalToast/GlobalMessage/GlobalLoading

## 常用命令

```bash
# 代码检查
pnpm lint

# 代码修复
pnpm lint:fix

# 类型检查
pnpm type-check

# 重新生成 API
pnpm alova-gen
```

## 相关文档

- [wot-design-uni 文档](https://wot-ui.cn)
- [uni-app 文档](https://uniapp.dcloud.net.cn)
- [UnoCSS 文档](https://unocss.dev)
```

- [ ] **Step 2: 提交前端 README**

```bash
git add chuan-bill-app/README.md
git commit -m "docs: 更新前端项目 README.md"
```

---

## Task 4: 编写项目根目录 README.md

**文件：**
- Create: `README.md`

- [ ] **Step 1: 创建项目 README.md**

```markdown
# 小川记账 (Chuan Bill)

<p align="center">
  <img alt="logo" src="logo.png" width="200">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-1.0.0-blue" alt="version">
  <img src="https://img.shields.io/badge/license-MIT-green" alt="license">
  <img src="https://img.shields.io/badge/Java-17-orange" alt="Java">
  <img src="https://img.shields.io/badge/Vue-3-brightgreen" alt="Vue">
</p>

## 项目简介

"小川记账"是一款集个人记账、家庭共享、预算管理和智能分析于一体的应用。旨在为用户提供便捷的记账体验和科学的财务管理方案。

## 核心功能

- **记账功能**：手动记账、OCR识别、语音输入
- **账单管理**：增删改查、多维度搜索筛选
- **家庭共享**：创建家庭、成员管理、账单共享
- **预算管理**：个人/家庭预算、预算提醒
- **统计分析**：数据可视化、AI智能建议
- **消息中心**：系统消息、社交消息、账单提醒

## 技术栈

| 类别 | 技术 |
|------|------|
| 前端 | uni-app 3.0 / Vue 3.4 / TypeScript 5.5 |
| UI 组件 | wot-design-uni 1.14 / UnoCSS |
| 后端 | Spring Boot 3.5 / Java 17 |
| ORM | MyBatis-Plus 3.5 |
| 认证 | Sa-Token 1.44 |
| 数据库 | MySQL 8.0 + Redis 6.0 |
| 部署 | Docker + Nginx |

## 快速开始

### 环境要求

- Node.js >= 20.19.0
- Java 17
- MySQL 8.0+
- Redis 6.0+
- pnpm >= 9.9.0

### 安装与运行

```bash
# 克隆项目
git clone https://github.com/your-username/chuan-bill.git
cd chuan-bill

# 安装前端依赖
cd chuan-bill-app
pnpm install

# 启动前端开发服务器
pnpm dev

# 启动后端（新终端）
cd chuan-bill-server
mvn spring-boot:run
```

### Docker 部署

```bash
# 使用 docker-compose 一键部署
docker-compose up -d
```

## 项目结构

```
chuan-bill/
├── chuan-bill-app/          # 前端项目 (uni-app)
├── chuan-bill-server/       # 后端项目 (Spring Boot)
├── docs/                    # 项目文档
├── docker-compose.yml       # Docker 编排配置
└── package.json             # 根目录 package.json
```

## 文档链接

- [前端文档](./chuan-bill-app/README.md)
- [后端文档](./chuan-bill-server/README.md)
- [API 文档](http://localhost:8080/swagger-ui.html) (启动后端后访问)

## 开发规范

### Git 提交规范

使用 Conventional Commits 规范：

- `feat:` 新功能
- `fix:` 修复 bug
- `docs:` 文档更新
- `style:` 代码格式调整
- `refactor:` 重构
- `test:` 测试相关
- `chore:` 构建/工具链更新

### 代码规范

- 前端：ESLint + UnoCSS
- 后端：Spotless (Palantir Java Format)

## 常用命令

```bash
# 同时启动前后端
pnpm start

# 代码检查
pnpm lint

# 代码修复
pnpm lint:fix
```

## 开源协议

本项目基于 [MIT](https://zh.wikipedia.org/wiki/MIT%E8%A8%B1%E5%8F%AF%E8%AD%89) 协议。
```

- [ ] **Step 2: 提交项目 README**

```bash
git add README.md
git commit -m "docs: 添加项目根目录 README.md"
```

---

## Task 5: 创建后端 Dockerfile

**文件：**
- Create: `chuan-bill-server/Dockerfile`

- [ ] **Step 1: 创建后端 Dockerfile**

```dockerfile
# 多阶段构建 - 构建阶段
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# 复制 pom.xml 并下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# 多阶段构建 - 运行阶段
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 从构建阶段复制 JAR 文件
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: 提交后端 Dockerfile**

```bash
git add chuan-bill-server/Dockerfile
git commit -m "feat(docker): 添加后端 Dockerfile"
```

---

## Task 6: 创建前端 Dockerfile 和 Nginx 配置

**文件：**
- Create: `chuan-bill-app/Dockerfile`
- Create: `chuan-bill-app/nginx.conf`

- [ ] **Step 1: 创建 Nginx 配置**

```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # 前端路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api/ {
        proxy_pass http://backend:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

- [ ] **Step 2: 创建前端 Dockerfile**

```dockerfile
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
```

- [ ] **Step 3: 提交前端 Docker 配置**

```bash
git add chuan-bill-app/Dockerfile chuan-bill-app/nginx.conf
git commit -m "feat(docker): 添加前端 Dockerfile 和 Nginx 配置"
```

---

## Task 7: 创建 docker-compose.yml

**文件：**
- Create: `docker-compose.yml`
- Create: `.env.example`

- [ ] **Step 1: 创建环境变量模板**

```bash
# 数据库配置
MYSQL_PASSWORD=your_password_here

# Redis 配置
REDIS_PASSWORD=

# 阿里云百炼配置
DASHSCOPE_API_KEY=
DASHSCOPE_BILL_RECOGNITION_APP_ID=
DASHSCOPE_BILL_ANALYSIS_APP_ID=

# 微信小程序配置
WX_MINIAPP_APPID=
WX_MINIAPP_SECRET=

# 七牛云配置
QINIU_ACCESS_KEY=
QINIU_SECRET_KEY=
QINIU_BUCKET=
QINIU_CDN_DOMAIN=
QINIU_ENDPOINT=
```

- [ ] **Step 2: 创建 docker-compose.yml**

```yaml
version: '3.8'

services:
  backend:
    build:
      context: ./chuan-bill-server
      dockerfile: Dockerfile
    container_name: chuan-bill-backend
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - MYSQL_HOST=mysql
      - MYSQL_PORT=3306
      - MYSQL_DATABASE=chuan_bill
      - MYSQL_USERNAME=root
      - MYSQL_PASSWORD=${MYSQL_PASSWORD}
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY}
      - DASHSCOPE_BILL_RECOGNITION_APP_ID=${DASHSCOPE_BILL_RECOGNITION_APP_ID}
      - DASHSCOPE_BILL_ANALYSIS_APP_ID=${DASHSCOPE_BILL_ANALYSIS_APP_ID}
      - WX_MINIAPP_APPID=${WX_MINIAPP_APPID}
      - WX_MINIAPP_SECRET=${WX_MINIAPP_SECRET}
      - QINIU_ACCESS_KEY=${QINIU_ACCESS_KEY}
      - QINIU_SECRET_KEY=${QINIU_SECRET_KEY}
      - QINIU_BUCKET=${QINIU_BUCKET}
      - QINIU_CDN_DOMAIN=${QINIU_CDN_DOMAIN}
      - QINIU_ENDPOINT=${QINIU_ENDPOINT}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    networks:
      - chuan-bill-network

  frontend:
    build:
      context: ./chuan-bill-app
      dockerfile: Dockerfile
    container_name: chuan-bill-frontend
    restart: unless-stopped
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - chuan-bill-network

  mysql:
    image: mysql:8.0
    container_name: chuan-bill-mysql
    restart: unless-stopped
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_PASSWORD}
      - MYSQL_DATABASE=chuan_bill
    volumes:
      - mysql_data:/var/lib/mysql
      - ./chuan-bill-server/init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "3306:3306"
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - chuan-bill-network

  redis:
    image: redis:6-alpine
    container_name: chuan-bill-redis
    restart: unless-stopped
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    networks:
      - chuan-bill-network

volumes:
  mysql_data:
    driver: local
  redis_data:
    driver: local

networks:
  chuan-bill-network:
    driver: bridge
```

- [ ] **Step 3: 更新 .gitignore**

在 `.gitignore` 文件中添加：

```gitignore
# 环境变量文件
.env
.env.local
.env.*.local
```

- [ ] **Step 4: 提交 Docker 编排配置**

```bash
git add docker-compose.yml .env.example
git commit -m "feat(docker): 添加 docker-compose 编排配置"
```

---

## Task 8: 验证和最终提交

- [ ] **Step 1: 检查所有文件是否创建正确**

```bash
# 检查文件是否存在
ls -la README.md
ls -la chuan-bill-app/README.md
ls -la chuan-bill-app/Dockerfile
ls -la chuan-bill-app/nginx.conf
ls -la chuan-bill-server/README.md
ls -la chuan-bill-server/Dockerfile
ls -la docker-compose.yml
ls -la .env.example
ls -la chuan-bill-server/src/main/resources/application.yaml
ls -la chuan-bill-server/src/main/resources/application-dev.yaml
ls -la chuan-bill-server/src/main/resources/application-prod.yaml
```

- [ ] **Step 2: 检查 Git 状态**

```bash
git status
```

- [ ] **Step 3: 查看提交历史**

```bash
git log --oneline -5
```

- [ ] **Step 4: 如果有未提交的更改，进行最终提交**

```bash
git add .
git commit -m "docs: 完成项目文档和 Docker 配置"
```
