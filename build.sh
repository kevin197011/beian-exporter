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
docker build -t beian-exporter .

echo "✅ Docker 镜像构建完成！"

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