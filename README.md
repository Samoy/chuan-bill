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
