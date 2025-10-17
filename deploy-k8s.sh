#!/bin/bash

# Kubernetes 部署脚本

set -e

echo "🚀 开始部署 Beian Exporter 到 Kubernetes..."

# 检查 kubectl
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl 未安装，请先安装 kubectl"
    exit 1
fi

# 检查集群连接
echo "📋 检查 Kubernetes 集群连接..."
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ 无法连接到 Kubernetes 集群"
    exit 1
fi

echo "✅ Kubernetes 集群连接正常"

# 选择部署模式
echo ""
echo "请选择部署模式:"
echo "1) 简化部署 (k8s-simple.yaml)"
echo "2) 完整部署 (k8s-manifests.yaml)"
read -p "请输入选择 [1-2]: " choice

case $choice in
    1)
        MANIFEST_FILE="k8s-simple.yaml"
        echo "📦 使用简化部署模式..."
        ;;
    2)
        MANIFEST_FILE="k8s-manifests.yaml"
        echo "📦 使用完整部署模式..."
        ;;
    *)
        echo "❌ 无效选择，使用简化部署模式"
        MANIFEST_FILE="k8s-simple.yaml"
        ;;
esac

# 构建 Docker 镜像
echo "🐳 构建 Docker 镜像..."
docker build -t beian-exporter:latest .

# 如果使用 minikube，加载镜像
if command -v minikube &> /dev/null && minikube status &> /dev/null; then
    echo "📥 加载镜像到 minikube..."
    minikube image load beian-exporter:latest
fi

# 部署到 Kubernetes
echo "🚀 部署到 Kubernetes..."
kubectl apply -f $MANIFEST_FILE

# 等待部署完成
echo "⏳ 等待部署完成..."
kubectl wait --for=condition=available --timeout=300s deployment/beian-exporter -n beian-exporter

# 显示部署状态
echo ""
echo "📊 部署状态:"
kubectl get all -n beian-exporter

# 显示访问方式
echo ""
echo "🎉 部署完成！"
echo ""
echo "📋 访问方式:"
echo "1. 端口转发访问:"
echo "   kubectl port-forward -n beian-exporter service/beian-exporter 8080:8080"
echo "   然后访问: http://localhost:8080"
echo ""
echo "2. 查看日志:"
echo "   kubectl logs -n beian-exporter -l app=beian-exporter -f"
echo ""
echo "3. 查看 Prometheus 指标:"
echo "   kubectl port-forward -n beian-exporter service/beian-exporter 8080:8080"
echo "   然后访问: http://localhost:8080/prometheus"
echo ""
echo "4. 删除部署:"
echo "   kubectl delete -f $MANIFEST_FILE"