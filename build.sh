#!/bin/bash

# Beian Exporter Java 版本构建脚本

set -e

echo "🚀 开始构建 Beian Exporter Java 版本..."

# 检查 Java 和 Maven
echo "📋 检查环境..."
if ! command -v java &> /dev/null; then
    echo "❌ Java 未安装，请先安装 Java 17+"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "❌ Maven 未安装，请先安装 Maven"
    exit 1
fi

# 显示版本信息
echo "☕ Java 版本:"
java -version

echo "📦 Maven 版本:"
mvn -version

# 清理并编译
echo "🧹 清理项目..."
mvn clean

echo "🔨 编译项目..."
mvn compile

echo "🧪 运行测试..."
mvn test

echo "📦 打包应用..."
mvn package -DskipTests

# 检查 JAR 文件
JAR_FILE=$(find target -name "beian-exporter-*.jar" | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "❌ 未找到构建的 JAR 文件"
    exit 1
fi

echo "✅ 构建成功！JAR 文件: $JAR_FILE"

# 构建 Docker 镜像
echo "🐳 构建 Docker 镜像..."
docker build -f Dockerfile.java -t beian-exporter:java .

echo "✅ Docker 镜像构建完成！"

# 显示使用说明
echo ""
echo "🎉 构建完成！"
echo ""
echo "📋 使用方法:"
echo "1. 直接运行 JAR:"
echo "   java -jar $JAR_FILE"
echo ""
echo "2. 使用 Docker:"
echo "   docker run -p 8080:8080 beian-exporter:java"
echo ""
echo "3. 使用 Docker Compose:"
echo "   docker-compose -f docker-compose.java.yml up -d"
echo ""
echo "🌐 访问地址:"
echo "   - 主页: http://localhost:8080"
echo "   - 指标: http://localhost:8080/metrics"
echo "   - 健康检查: http://localhost:8080/health"
echo "   - Prometheus: http://localhost:9090"
echo "   - Grafana: http://localhost:3000 (admin/admin)"