# 多阶段构建 - 构建阶段
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# 从 build args 注入 Sentry auth token
ARG SENTRY_AUTH_TOKEN

# 复制 pom.xml 并下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B -Dspotless.check.skip=true

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests -Dspotless.check.skip=true -B

# 多阶段构建 - 运行阶段
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 创建非 root 用户
RUN addgroup -S app && adduser -S app -G app

# 创建临时文件目录并设置权限
RUN mkdir -p /app/temp/upload && chown -R app:app /app/temp

# 从构建阶段复制 JAR 文件
COPY --from=builder /app/target/*.jar app.jar

# 切换到非 root 用户
USER app

# 暴露端口
EXPOSE 8080

# 启动应用，设置 JVM 容器感知参数
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
