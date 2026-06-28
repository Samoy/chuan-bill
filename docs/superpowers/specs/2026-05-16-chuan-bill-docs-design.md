# 小川记账项目文档设计

## 概述

为小川记账项目编写完整的项目文档，包括项目 README、前端 README、后端 README、Dockerfile 和配置文件。

## 设计决策

### 1. 文档风格
- 采用现代 README 风格
- 使用 badges 展示技术栈版本
- 中文文档
- 结构清晰，易于维护

### 2. 部署方案
- 支持后端 + 前端 H5 版本的 Docker 部署
- 使用 docker-compose 编排服务
- 包含 MySQL 和 Redis 服务

### 3. 配置管理
- 采用配置文件分离方式
- 公共配置在 application.yaml
- 环境特定配置在 application-{env}.yaml
- 敏感信息通过环境变量注入

## 详细设计

### 1. 项目 README.md

**位置**：项目根目录

**内容结构**：
- 项目简介
- 核心功能
- 技术栈
- 快速开始
- 项目结构
- 文档链接
- 贡献指南
- 开源协议

**特点**：
- 带有 logo 和 badges
- 清晰的功能列表
- 完整的快速开始指南
- 链接到子项目文档

### 2. 前端 README.md

**位置**：chuan-bill-app/

**内容结构**：
- 简介
- 技术栈
- 特性
- 快速开始
- 项目结构
- 开发规范
- 常用命令
- 相关文档

**特点**：
- 基于现有 wot-starter README 改造
- 突出 uni-app 跨平台特性
- 详细的开发和构建命令
- 明确的开发规范

### 3. 后端 README.md

**位置**：chuan-bill-server/

**内容结构**：
- 简介
- 技术栈
- 特性
- 快速开始
- 项目结构
- API 文档
- 开发规范
- 常用命令
- 相关文档

**特点**：
- 突出 Spring Boot 3 特性
- 包含数据库初始化指南
- 明确的分层架构说明
- API 文档访问方式

### 4. Dockerfile

**后端 Dockerfile**：
- 多阶段构建
- 使用 Maven 构建
- 使用 JRE Alpine 镜像
- 暴露 8080 端口

**前端 Dockerfile**：
- 多阶段构建
- 使用 Node.js 构建
- 使用 Nginx 服务静态文件
- 暴露 80 端口

**docker-compose.yml**：
- 编排后端、前端、MySQL、Redis 服务
- 环境变量配置
- 数据卷持久化
- 服务依赖关系

### 5. 配置文件

**application.yaml**（公共配置）：
- 应用名称
- 文件上传限制
- Sa-Token 配置
- MyBatis-Plus 软删除配置
- SpringDoc 配置
- 偏好设置白名单

**application-dev.yaml**（开发环境）：
- 本地数据库连接
- 本地 Redis 连接
- 开启 SQL 日志
- 开启 Swagger UI
- 第三方服务配置

**application-prod.yaml**（生产环境）：
- 环境变量注入数据库连接
- 环境变量注入 Redis 连接
- 关闭 SQL 日志
- 关闭 Swagger UI
- 更大的连接池配置
- 启用数据库 SSL

## 文件清单

1. `README.md` - 项目根目录
2. `chuan-bill-app/README.md` - 前端项目
3. `chuan-bill-server/README.md` - 后端项目
4. `chuan-bill-server/Dockerfile` - 后端 Docker 镜像
5. `chuan-bill-app/Dockerfile` - 前端 Docker 镜像
6. `chuan-bill-app/nginx.conf` - Nginx 配置
7. `docker-compose.yml` - Docker 编排
8. `chuan-bill-server/src/main/resources/application.yaml` - 公共配置
9. `chuan-bill-server/src/main/resources/application-dev.yaml` - 开发环境配置
10. `chuan-bill-server/src/main/resources/application-prod.yaml` - 生产环境配置

## 实施顺序

1. 编写配置文件（application.yaml, application-dev.yaml, application-prod.yaml）
2. 编写后端 README.md
3. 编写前端 README.md
4. 编写项目 README.md
5. 创建 Dockerfile 和 docker-compose.yml
6. 创建 nginx.conf
