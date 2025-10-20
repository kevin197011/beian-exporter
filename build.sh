#!/bin/bash

# Beian Exporter 构建脚本（多阶段构建）

set -e

echo "🚀 开始构建 Beian Exporter（多阶段构建）..."

# 检查 Docker
echo "📋 检查环境..."
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装，请先安装 Docker"
    exit 1
fi

# 显示Docker版本
echo "🐳 Docker 版本:"
docker --version

# 构建 Docker 镜像（多阶段构建，包含编译步骤）
echo "🐳 构建 Docker 镜像（多阶段构建）..."
echo "   - 第一阶段：Maven 编译打包"
echo "   - 第二阶段：运行时镜像构建"

# 先尝试多阶段构建（Docker Hub 可能临时不可用导致失败）
set +e
docker build -t beian-exporter .
BUILD_STATUS=$?
set -e

if [ $BUILD_STATUS -ne 0 ]; then
    echo "⚠️ 多阶段构建失败，尝试使用运行时回退构建（MCR OpenJDK 基础镜像）..."

    # 检查 Maven
    if ! command -v mvn &> /dev/null; then
        echo "❌ 未找到 Maven，本地打包 JAR 失败，无法使用回退构建。"
        echo "👉 解决方案："
        echo "   1) 安装 Maven 后重试 ./build.sh"
        echo "   2) 或者先执行 'docker login' 后再重试（可缓解匿名拉取失败）"
        echo "   3) 或在 Docker 设置中配置 registry-mirrors 后重试"
        exit 1
    fi

    echo "📦 本地打包 JAR..."
    mvn -q -f "$(dirname "$0")/pom.xml" clean package -DskipTests

    echo "🐳 使用 Dockerfile.runtime 构建运行时镜像..."
    docker build -t beian-exporter -f Dockerfile.runtime .
    echo "✅ 回退构建完成！"
else
    echo "✅ Docker 镜像构建完成！"
fi

# 显示使用说明
echo ""
echo "🎉 构建完成！"
echo ""
echo "📋 使用方法:"
echo "1. 使用 Docker 直接运行:"
echo "   docker run -p 8080:8080 beian-exporter"
echo ""
echo "2. 使用 Docker Compose（推荐）:"
echo "   docker-compose up -d"
echo ""
echo "3. 查看容器日志:"
echo "   docker-compose logs -f beian-exporter"
echo ""
echo "🌐 访问地址:"
echo "   - 主页: http://localhost:8080"
echo "   - Prometheus指标: http://localhost:8080/prometheus"
echo "   - 健康检查: http://localhost:8080/health"
echo "   - Prometheus: http://localhost:9090"
echo "   - Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "📁 配置文件:"
echo "   - 容器内配置: /app/config/application.yml"
echo "   - 外部配置挂载: ./config/application.yml"
echo ""
echo "💡 提示:"
echo "   - 多阶段构建无需本地安装Java和Maven"
echo "   - 配置文件已自动复制到容器内"
echo "   - 可通过挂载./config目录覆盖默认配置"