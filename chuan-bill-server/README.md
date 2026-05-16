# 小川记账 - 后端

<p>
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

项目使用 Spring Profile 进行环境配置：

- `application.yaml` - 公共配置
- `application-dev.yaml` - 开发环境配置
- `application-prod.yaml` - 生产环境配置

开发环境默认使用 `dev` profile，无需额外配置。如需切换环境，设置 `spring.profiles.active` 参数：

```bash
# 使用开发环境配置（默认）
mvn spring-boot:run

# 使用生产环境配置
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

敏感信息通过环境变量注入，参考 `application-dev.yaml` 中的 `${VARIABLE_NAME:default}` 格式。

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
├── dao/              # 数据访问层
├── entity/           # 实体类
├── dto/              # 数据传输对象
├── vo/               # 视图对象
├── config/           # 配置类
├── handler/          # 处理器
├── exception/        # 异常处理
├── result/           # 统一响应封装
├── constant/         # 常量定义
├── utils/            # 工具类
└── job/              # 定时任务
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
