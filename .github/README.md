# GitHub Actions 工作流说明

本项目包含多个 GitHub Actions 工作流，用于自动化构建、测试、安全扫描和部署。

## 工作流文件

### 1. `docker-build-push.yml` - 基础 Docker 构建推送
**触发条件：**
- 推送到 `main` 或 `develop` 分支
- 创建标签 `v*`
- Pull Request 到 `main` 分支

**功能：**
- 构建多架构 Docker 镜像 (amd64, arm64)
- 推送到 GitHub Container Registry
- 支持缓存优化构建速度

### 2. `docker-multi-registry.yml` - 多注册表推送
**触发条件：**
- 推送到 `main` 分支
- 创建标签 `v*`
- 发布 Release

**功能：**
- 同时推送到 GitHub Container Registry 和 Docker Hub
- 支持多架构构建
- 自动标签管理

**所需 Secrets：**
```
DOCKERHUB_USERNAME - Docker Hub 用户名
DOCKERHUB_TOKEN - Docker Hub 访问令牌
```

### 3. `ci-cd.yml` - 完整 CI/CD 流水线
**触发条件：**
- 推送到 `main` 或 `develop` 分支
- Pull Request 到 `main` 分支
- 发布 Release

**功能：**
- 运行 Maven 测试
- 安全漏洞扫描 (Trivy)
- 构建和推送 Docker 镜像
- 自动部署到 staging/production 环境

### 4. `release.yml` - 发布工作流
**触发条件：**
- 创建标签 `v*`

**功能：**
- 构建 JAR 文件
- 构建和推送 Docker 镜像
- 创建 GitHub Release
- 上传构建产物

## 使用指南

### 设置 Secrets

在 GitHub 仓库设置中添加以下 Secrets：

```bash
# Docker Hub (可选，用于多注册表推送)
DOCKERHUB_USERNAME=your-dockerhub-username
DOCKERHUB_TOKEN=your-dockerhub-token
```

### 发布新版本

1. **创建标签并推送：**
```bash
git tag v1.0.1
git push origin v1.0.1
```

2. **自动触发：**
- `release.yml` 工作流自动运行
- 构建 Docker 镜像并推送
- 创建 GitHub Release

### 使用发布的镜像

```bash
# 从 GitHub Container Registry
docker pull ghcr.io/your-username/beian-exporter:latest
docker pull ghcr.io/your-username/beian-exporter:v1.0.1

# 从 Docker Hub (如果配置了多注册表推送)
docker pull your-username/beian-exporter:latest
```

## 环境配置

### Staging 环境
- 分支：`develop`
- 自动部署：推送到 `develop` 分支时触发

### Production 环境
- 分支：`main`
- 自动部署：推送到 `main` 分支或创建 Release 时触发

## 安全扫描

所有工作流都包含 Trivy 安全扫描：
- **代码扫描**：扫描源代码中的漏洞
- **镜像扫描**：扫描构建的 Docker 镜像
- **结果上传**：扫描结果自动上传到 GitHub Security 标签页

## 缓存优化

- **Maven 依赖缓存**：加速 Java 构建
- **Docker 层缓存**：使用 GitHub Actions 缓存
- **多阶段构建**：优化镜像大小

## 监控和通知

- **构建状态**：在 Actions 标签页查看
- **安全报告**：在 Security 标签页查看
- **Release 通知**：自动创建 Release 页面

## 故障排除

### 常见问题

1. **权限错误**
   - 确保 `GITHUB_TOKEN` 有足够权限
   - 检查仓库的 Actions 权限设置

2. **Docker Hub 推送失败**
   - 验证 `DOCKERHUB_USERNAME` 和 `DOCKERHUB_TOKEN`
   - 确保 Docker Hub 仓库存在

3. **构建超时**
   - 检查 Maven 依赖是否正确
   - 验证 Dockerfile 语法

### 调试技巧

```yaml
# 在工作流中添加调试步骤
- name: Debug
  run: |
    echo "Event: ${{ github.event_name }}"
    echo "Ref: ${{ github.ref }}"
    echo "SHA: ${{ github.sha }}"
```