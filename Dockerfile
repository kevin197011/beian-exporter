# 多阶段构建 - 构建阶段（使用 GitHub Container Registry 的 Eclipse Temurin 基础镜像）
FROM ghcr.io/eclipse-temurin:17-jdk AS builder

# 设置工作目录
WORKDIR /build

# 安装 Maven（避免从 Docker Hub 拉取 Maven 镜像）
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# 复制 Maven 配置文件
COPY pom.xml .

# 下载依赖（利用Docker缓存层）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 编译和打包应用
RUN mvn clean package -DskipTests -B

# 运行阶段（使用 GitHub Container Registry 的 Eclipse Temurin JRE）
FROM ghcr.io/eclipse-temurin:17-jre

# 设置工作目录
WORKDIR /app

# 安装必要的工具并设置时区
RUN apt-get update && apt-get install -y \
    curl \
    tzdata \
    && ln -sf /usr/share/zoneinfo/Asia/Hong_Kong /etc/localtime \
    && echo "Asia/Hong_Kong" > /etc/timezone \
    && rm -rf /var/lib/apt/lists/*

# 创建配置目录
RUN mkdir -p /app/config

# 从构建阶段复制JAR文件
COPY --from=builder /build/target/beian-exporter-*.jar app.jar

# 复制配置文件到config目录
COPY --from=builder /build/src/main/resources/application.yml /app/config/application.yml

# 创建非 root 用户
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser

# 暴露端口
EXPOSE 8080

# 设置环境变量
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV TZ=Asia/Hong_Kong

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# 启动应用
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]