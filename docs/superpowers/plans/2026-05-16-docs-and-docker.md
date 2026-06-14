# Documentation & Docker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add project READMEs, Docker support, and proper Spring Boot profile configuration to the Chuan Bill monorepo.

**Architecture:** Three README files (root, frontend, backend) document the project for new contributors. Dockerfiles containerize both apps. Spring Boot config splits into `application.yaml` (shared), `application-dev.yaml` (local development), and `application-prod.yaml` (production) following Spring profile conventions.

**Tech Stack:** Markdown, Docker, Spring Boot Profiles, docker-compose

---

## File Structure

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `README.md` | Root monorepo README — project overview, quickstart, architecture diagram |
| Create | `chuan-bill-app/README.md` | Frontend README — tech stack, dev commands, project structure |
| Create | `chuan-bill-server/README.md` | Backend README — tech stack, dev commands, API docs, DB setup |
| Create | `chuan-bill-server/Dockerfile` | Multi-stage build: Maven build + JRE runtime |
| Create | `chuan-bill-app/Dockerfile` | Multi-stage build: Node build + Nginx static serving |
| Create | `docker-compose.yml` | Orchestrate MySQL, Redis, backend, frontend |
| Create | `docker-compose.dev.yml` | Dev override: bind mounts, hot reload |
| Create | `.env.example` | Template env vars (no secrets) |
| Modify | `chuan-bill-server/src/main/resources/application.yaml` | Keep shared config, activate profile via env |
| Create | `chuan-bill-server/src/main/resources/application-dev.yaml` | Dev profile: localhost, SQL logging, relaxed settings |
| Create | `chuan-bill-server/src/main/resources/application-prod.yaml` | Prod profile: no SQL logging, connection pooling, actuator |

---

### Task 1: Create `.env.example`

**Files:**
- Create: `.env.example`

- [ ] **Step 1: Write `.env.example`**

Create a template with all required env vars but placeholder values. This documents what's needed without exposing secrets.

```bash
# MySQL配置
MYSQL_USERNAME=root
MYSQL_PASSWORD=your_password_here

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# 百炼配置
DASHSCOPE_API_KEY=your_dashscope_api_key
DASHSCOPE_BILL_RECOGNITION_APP_ID=your_recognition_app_id
DASHSCOPE_BILL_ANALYSIS_APP_ID=your_analysis_app_id

# 微信小程序配置
WX_MINIAPP_APPID=your_wechat_appid
WX_MINIAPP_SECRET=your_wechat_secret

# 七牛云配置
QINIU_ACCESS_KEY=your_qiniu_access_key
QINIU_SECRET_KEY=your_qiniu_secret_key
QINIU_BUCKET=chuan-bill
QINIU_CDN_DOMAIN=https://your-cdn-domain.com
QINIU_ENDPOINT=https://up-z1.qiniup.com
```

- [ ] **Step 2: Commit**

```bash
git add .env.example
git commit -m "docs: add .env.example with required environment variables"
```

---

### Task 2: Create Root `README.md`

**Files:**
- Create: `README.md`

- [ ] **Step 1: Write root README.md**

```markdown
# 小川记账 (Chuan Bill)

个人与家庭记账应用，支持手动记账、语音记账、OCR 识别、AI 分析等功能。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + uni-app + UnoCSS + wot-design-uni |
| 后端 | Spring Boot 3 + Java 17 + MyBatis-Plus + Sa-Token |
| 数据库 | MySQL 8 + Redis |
| AI | 阿里云百炼（DashScope） |
| 构建 | Vite (前端) + Maven (后端) + pnpm (Monorepo) |

## 快速开始

### 环境要求

- Node.js >= 22 (推荐使用 nvm)
- pnpm >= 10
- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+

### 1. 克隆项目

```bash
git clone <repo-url> chuan-bill
cd chuan-bill
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 填入实际配置
```

### 3. 初始化数据库

```bash
mysql -u root -p < chuan-bill-server/init.sql
```

### 4. 启动项目

```bash
# 安装依赖
pnpm install
cd chuan-bill-app && pnpm install && cd ..

# 同时启动前后端
pnpm start
```

前端开发服务器运行在 `http://localhost:5173`，后端 API 运行在 `http://localhost:8080`。

### Docker 启动（可选）

```bash
docker-compose up -d
```

详见各子项目 README。

## 项目结构

```
chuan-bill/
├── chuan-bill-app/          # 前端 (uni-app / Vue 3)
├── chuan-bill-server/       # 后端 (Spring Boot)
├── .env.example             # 环境变量模板
├── docker-compose.yml       # Docker 编排
├── package.json             # Monorepo 脚本
└── PRD.md                   # 产品需求文档
```

## 常用命令

| 命令 | 说明 |
|------|------|
| `pnpm start` | 同时启动前后端 |
| `pnpm lint` | 检查代码规范 |
| `pnpm lint:fix` | 自动修复代码规范 |

## 功能特性

- 📝 手动记账、语音记账、OCR 票据识别
- 📊 多维度统计分析，AI 智能建议
- 👨‍👩‍👧 家庭账本，成员协作
- 🔔 账单提醒通知
- 📤 数据导出 (Excel / PDF)
- 🌙 深色模式
- 📱 微信小程序 + H5 + App 多端支持

## 开源协议

[MIT](LICENSE)
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: add root README with project overview and quickstart"
```

---

### Task 3: Create Frontend `chuan-bill-app/README.md`

**Files:**
- Modify: `chuan-bill-app/README.md` (replace boilerplate wot-starter content)

- [ ] **Step 1: Write frontend README.md**

Read the existing file first, then overwrite it.

```markdown
# 小川记账 - 前端

基于 uni-app 的跨平台记账应用前端，支持 H5、微信小程序和 App。

## 技术栈

- **框架**: Vue 3.4 + TypeScript
- **跨平台**: uni-app (via @dcloudio/uni-*)
- **UI 组件**: wot-design-uni
- **样式**: UnoCSS + SCSS
- **状态管理**: Pinia
- **HTTP 客户端**: Alova.js
- **图表**: ECharts 6
- **构建工具**: Vite 5
- **路由**: vite-plugin-uni-pages (文件路由)

## 开发

### 环境要求

- Node.js >= 22
- pnpm >= 9

### 安装依赖

```bash
pnpm install
```

### 启动开发服务器

```bash
# H5 开发
pnpm dev

# 微信小程序开发
pnpm dev:mp-weixin

# App 开发
pnpm dev:app
```

### 构建

```bash
pnpm build           # H5 构建
pnpm build:mp-weixin # 微信小程序构建
pnpm build:app       # App 构建
```

### 代码检查

```bash
pnpm lint       # ESLint 检查
pnpm lint:fix   # ESLint 自动修复
pnpm type-check # TypeScript 类型检查
```

### API 定义生成

后端启动后，从 OpenAPI 规范重新生成 API 类型定义：

```bash
pnpm alova-gen
```

## 项目结构

```
src/
├── api/              # API 请求层 (Alova.js)
│   ├── core/         # 请求实例、拦截器、中间件
│   └── mock/         # Mock 数据适配器
├── pages/            # 主包页面 (文件路由)
│   ├── bill/         # 记账页面及组件
│   ├── family/       # 家庭管理
│   ├── message/      # 消息中心
│   ├── mine/         # 个人中心
│   └── statistics/   # 统计分析
├── subPages/         # 分包页面
├── store/            # Pinia 状态管理
├── components/       # 全局组件 (Toast/Message/Loading)
├── composables/      # 组合式函数
├── layouts/          # 布局组件
├── router/           # 路由守卫
└── utils/            # 工具函数
```

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `VITE_API_BASE_URL` | 后端 API 地址 | `http://localhost:8080` |
| `VITE_WS_BASE_URL` | WebSocket 地址 | `ws://localhost:8080` |

环境文件: `.env.development` / `.env.staging` / `.env.production`

## 编码规范

- Vue 3 `<script setup>` + TypeScript 严格模式
- UnoCSS 为主要样式方案，复杂场景使用 SCSS
- 使用 `GlobalToast` / `GlobalMessage` / `GlobalLoading` 替代原生 `uni.showToast`
- Pinia Store 命名: `use{Name}Store`
- 2 空格缩进，LF 换行，UTF-8
```

- [ ] **Step 2: Commit**

```bash
git add chuan-bill-app/README.md
git commit -m "docs: replace frontend README with project-specific documentation"
```

---

### Task 4: Create Backend `chuan-bill-server/README.md`

**Files:**
- Create: `chuan-bill-server/README.md`

- [ ] **Step 1: Write backend README.md**

```markdown
# 小川记账 - 后端

Spring Boot 后端服务，提供 RESTful API、WebSocket、定时任务等功能。

## 技术栈

- **框架**: Spring Boot 3.5 + Java 17
- **ORM**: MyBatis-Plus 3.5
- **认证**: Sa-Token (Redis 存储)
- **数据库**: MySQL 8.0 + Redis 7.0
- **AI**: 阿里云百炼 (DashScope SDK)
- **API 文档**: SpringDoc OpenAPI (Swagger UI)
- **微信**: weixin-java-miniapp
- **存储**: 七牛云 SDK
- **导出**: FastExcel + OpenPDF
- **代码格式**: Spotless (Palantir Java Format)

## 开发

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+

### 配置环境变量

在项目根目录或 `chuan-bill-server/` 下创建 `.env` 文件，参考根目录 `.env.example`。

### 初始化数据库

```bash
mysql -u root -p < init.sql
```

### 启动

```bash
mvn spring-boot:run
```

服务默认运行在 `http://localhost:8080`。

### 代码格式化

```bash
mvn spotless:check   # 检查格式
mvn spotless:apply   # 自动格式化
```

### API 文档

启动后访问 Swagger UI: `http://localhost:8080/swagger-ui.html`

## 项目结构

```
src/main/java/com/samoy/chuanbillserver/
├── config/       # Spring 配置 (MVC, Redis, WebSocket, Sa-Token 等)
├── controller/   # REST 控制器 (9 个)
├── service/      # 业务逻辑接口 + 实现
├── dao/          # MyBatis-Plus Mapper 接口 (12 个)
├── entity/       # 数据库实体 (12 个)
├── dto/          # 请求数据传输对象
├── vo/           # 响应视图对象
├── exception/    # 异常处理
├── handler/      # WebSocket 处理器 (语音识别)
├── job/          # Quartz 定时任务
├── result/       # 统一响应封装
├── constant/     # 系统常量
└── utils/        # 工具类
```

## 数据库表

| 表名 | 说明 |
|------|------|
| t_user | 用户表 |
| t_category | 类目表 |
| t_bill | 账单表 |
| t_payment_method | 支付方式表 |
| t_family | 家庭表 |
| t_family_member | 家庭成员表 |
| t_family_join_apply | 家庭加入申请表 |
| t_budget | 预算表 |
| t_message | 消息表 |
| t_user_preference | 用户偏好设置表 |
| t_ai_suggestion | AI 建议表 |
| t_ai_usage | AI 使用记录表 |

## API 模块

| 控制器 | 路径前缀 | 说明 |
|--------|----------|------|
| AuthController | /auth | 登录注册、验证码 |
| UserController | /user | 用户信息管理 |
| BillController | /bill | 账单 CRUD、导出 |
| StatisticsController | /statistics | 统计分析 |
| FamilyController | /family | 家庭管理 |
| MessageController | /message | 消息通知 |
| AIController | /ai | AI 分析建议 |
| FileController | /file | 文件上传 |
| UserPreferenceController | /preference | 用户偏好设置 |

## 配置文件

- `application.yaml` — 共享配置 (Spring Profile 通过 `SPRING_PROFILES_ACTIVE` 环境变量激活)
- `application-dev.yaml` — 开发环境 (SQL 日志、宽松设置)
- `application-prod.yaml` — 生产环境 (关闭 SQL 日志、连接池调优)
```

- [ ] **Step 2: Commit**

```bash
git add chuan-bill-server/README.md
git commit -m "docs: add backend README with API and database documentation"
```

---

### Task 5: Split Spring Boot Configuration into Profiles

**Files:**
- Modify: `chuan-bill-server/src/main/resources/application.yaml`
- Create: `chuan-bill-server/src/main/resources/application-dev.yaml`
- Create: `chuan-bill-server/src/main/resources/application-prod.yaml`

- [ ] **Step 1: Rewrite `application.yaml` (shared config)**

Strip dev-specific settings (SQL logging) and move them to profile files. Keep only shared infrastructure config. Add profile activation via env var.

```yaml
spring:
  application:
    name: chuan-bill-server
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/chuan_bill?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
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
  mapper-locations: classpath:mapper/*.xml

springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
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

preference:
  allowed-keys:
    - notification.master.enabled
    - notification.billReminder.enabled
    - notification.billReminder.time
    - notification.family.enabled
    - notification.system.enabled
```

- [ ] **Step 2: Create `application-dev.yaml`**

```yaml
# 开发环境配置
# 激活方式: SPRING_PROFILES_ACTIVE=dev

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
```

- [ ] **Step 3: Create `application-prod.yaml`**

```yaml
# 生产环境配置
# 激活方式: SPRING_PROFILES_ACTIVE=prod

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl

springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
          max-wait: 3000ms
```

- [ ] **Step 4: Commit**

```bash
git add chuan-bill-server/src/main/resources/application.yaml chuan-bill-server/src/main/resources/application-dev.yaml chuan-bill-server/src/main/resources/application-prod.yaml
git commit -m "refactor: split application config into dev/prod Spring profiles"
```

---

### Task 6: Create Backend Dockerfile

**Files:**
- Create: `chuan-bill-server/Dockerfile`

- [ ] **Step 1: Write multi-stage Dockerfile**

```dockerfile
# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: Add `.dockerignore` for backend**

Create `chuan-bill-server/.dockerignore`:

```
target/
.env
*.md
.idea/
.vscode/
```

- [ ] **Step 3: Commit**

```bash
git add chuan-bill-server/Dockerfile chuan-bill-server/.dockerignore
git commit -m "feat: add backend Dockerfile with multi-stage build"
```

---

### Task 7: Create Frontend Dockerfile

**Files:**
- Create: `chuan-bill-app/Dockerfile`
- Create: `chuan-bill-app/nginx.conf`

- [ ] **Step 1: Write frontend Dockerfile**

```dockerfile
# ---- Build Stage ----
FROM node:22-alpine AS build
WORKDIR /app

RUN corepack enable && corepack prepare pnpm@9.9.0 --activate

COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

# ---- Runtime Stage ----
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

COPY --from=build /app/dist/build/h5 .
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

- [ ] **Step 2: Write nginx.conf**

```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # SPA fallback
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API proxy (adjust upstream in docker-compose)
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket proxy
    location /ws/ {
        proxy_pass http://backend:8080/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

- [ ] **Step 3: Add `.dockerignore` for frontend**

Create `chuan-bill-app/.dockerignore`:

```
node_modules/
dist/
.env*
*.md
.idea/
.vscode/
```

- [ ] **Step 4: Commit**

```bash
git add chuan-bill-app/Dockerfile chuan-bill-app/nginx.conf chuan-bill-app/.dockerignore
git commit -m "feat: add frontend Dockerfile with Nginx serving"
```

---

### Task 8: Create `docker-compose.yml`

**Files:**
- Create: `docker-compose.yml`
- Create: `docker-compose.dev.yml`

- [ ] **Step 1: Write `docker-compose.yml`**

```yaml
services:
  mysql:
    image: mysql:8.0
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_PASSWORD:-123456}
      MYSQL_DATABASE: chuan_bill
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./chuan-bill-server/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build: ./chuan-bill-server
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_USERNAME: root
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-123456}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      DASHSCOPE_API_KEY: ${DASHSCOPE_API_KEY}
      DASHSCOPE_BILL_RECOGNITION_APP_ID: ${DASHSCOPE_BILL_RECOGNITION_APP_ID}
      DASHSCOPE_BILL_ANALYSIS_APP_ID: ${DASHSCOPE_BILL_ANALYSIS_APP_ID}
      WX_MINIAPP_APPID: ${WX_MINIAPP_APPID}
      WX_MINIAPP_SECRET: ${WX_MINIAPP_SECRET}
      QINIU_ACCESS_KEY: ${QINIU_ACCESS_KEY}
      QINIU_SECRET_KEY: ${QINIU_SECRET_KEY}
      QINIU_BUCKET: ${QINIU_BUCKET}
      QINIU_CDN_DOMAIN: ${QINIU_CDN_DOMAIN}
      QINIU_ENDPOINT: ${QINIU_ENDPOINT}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy

  frontend:
    build: ./chuan-bill-app
    restart: unless-stopped
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  mysql-data:
  redis-data:
```

- [ ] **Step 2: Write `docker-compose.dev.yml`**

```yaml
# 开发环境覆盖: docker-compose -f docker-compose.yml -f docker-compose.dev.yml up
services:
  backend:
    build:
      context: ./chuan-bill-server
      target: build
    environment:
      SPRING_PROFILES_ACTIVE: dev
    volumes:
      - ./chuan-bill-server/src:/app/src
      - ~/.m2/repository:/root/.m2/repository

  frontend:
    # 开发时不使用 Docker 构建前端，直接在宿主机 pnpm dev
    profiles:
      - donotstart
```

- [ ] **Step 3: Commit**

```bash
git add docker-compose.yml docker-compose.dev.yml
git commit -m "feat: add docker-compose for production and development"
```

---

### Task 9: Update `.gitignore` for Docker artifacts

**Files:**
- Modify: `.gitignore`

- [ ] **Step 1: Append Docker-related entries**

Read the current `.gitignore`, then append:

```
# Docker
docker-compose.override.yml
```

- [ ] **Step 2: Commit**

```bash
git add .gitignore
git commit -m "chore: update .gitignore for Docker artifacts"
```
