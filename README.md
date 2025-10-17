# Beian Exporter

基于 Java Spring Boot 的备案信息 Prometheus 导出器，用于监控域名备案状态。

## 功能特性

- 🔍 **自动备案查询** - 定期检查域名备案状态
- 📊 **Prometheus 指标** - 导出详细的监控指标
- 🔄 **智能重试** - 自动重试失败的查询，支持指数退避
- 🌐 **Web 界面** - 提供友好的状态查看界面
- 🐳 **Docker 支持** - 多阶段构建，完整的容器化部署方案
- 🛡️ **防封禁机制** - 动态请求头、随机延迟、IP轮换
- ⚡ **高性能** - 基于Spring Boot和WebFlux的响应式编程
- 📈 **完整监控栈** - 集成Prometheus和Grafana仪表板

## 快速开始

### 环境要求

- Docker（推荐，使用多阶段构建无需本地Java环境）
- 或者 Java 17+ + Maven 3.6+（本地开发）

### 最简单的启动方式

```bash
# 1. 克隆项目
git clone <repository-url>
cd beian-exporter

# 2. 一键构建并启动
./build.sh && docker-compose up -d

# 3. 访问 http://localhost:8080
```

### 本地开发运行

```bash
# 编译项目
mvn clean compile

# 打包应用
mvn package

# 运行JAR文件
java -jar target/beian-exporter-1.0.0.jar
```

### 使用构建脚本（推荐）

```bash
# 给脚本执行权限
chmod +x build.sh

# 运行构建脚本（多阶段构建，无需本地Java环境）
./build.sh
```

### Docker 运行

```bash
# 构建并运行完整监控栈
docker-compose up -d

# 查看日志
docker-compose logs -f beian-exporter
```

## 配置文件

应用使用 `application.yml` 配置监控参数：

```yaml
# 备案查询配置
beian:
  check-interval: 21600  # 检查间隔（秒）- 6小时
  request-timeout: 30    # 请求超时（秒）
  request-delay: 10      # 请求间隔（秒）- 避免被封
  max-retries: 3         # 最大重试次数
  rate-limit:
    max-requests-per-minute: 10  # 每分钟最大请求数
    burst-size: 3               # 突发请求数量
  domains:
    - baidu.com
    - qq.com
    - taobao.com
    - jd.com
    - sina.com.cn
```

### Docker 配置挂载

可以通过挂载外部配置文件来自定义配置：

```bash
# 创建配置目录
mkdir -p ./config

# 复制并修改配置文件
cp src/main/resources/application.yml ./config/

# 使用docker-compose启动（已配置配置文件挂载）
docker-compose up -d
```

## Prometheus 指标

| 指标名称 | 类型 | 描述 | 标签 |
|---------|------|------|------|
| `beian_status` | Gauge | 备案状态 (1=已备案, 0=未备案, -1=错误) | `domain` |
| `beian_info` | Gauge | 备案详细信息 | `domain`, `company_name`, `beian_number` 等 |
| `beian_check_errors_total` | Counter | 查询错误次数 | `domain`, `error_type` |
| `beian_last_check_timestamp` | Gauge | 最后检查时间戳 | `domain` |

## 访问地址

- **主页**: http://localhost:8080
- **Prometheus指标**: http://localhost:8080/prometheus
- **健康检查**: http://localhost:8080/health
- **配置信息**: http://localhost:8080/api/config
- **手动触发检查**: http://localhost:8080/api/check (POST)
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

## API 接口

### 手动触发检查

```bash
# 触发所有域名检查
curl -X POST http://localhost:8080/api/check

# 检查单个域名
curl http://localhost:8080/api/check/baidu.com

# 获取配置信息
curl http://localhost:8080/api/config
```

## 防封禁机制

为避免被第三方备案查询网站封禁，系统采用了以下策略：

1. **串行查询** - 避免并发请求
2. **随机延迟** - 每次请求前随机延迟1-3秒
3. **固定间隔** - 域名之间固定间隔10秒
4. **智能重试** - 失败后指数退避重试，最多3次
5. **请求头伪装** - 动态生成User-Agent、Cookie等请求头
6. **IP轮换** - 随机生成X-Forwarded-For头模拟不同来源

## 开发

### 项目结构

```
src/main/java/io/devops/beian/
├── BeianExporterApplication.java    # 主应用类
├── config/
│   ├── BeianProperties.java         # 配置属性
│   └── MetricsConfig.java          # 指标配置
├── controller/
│   ├── BeianController.java        # API控制器
│   └── HomeController.java         # 主页控制器
├── model/
│   ├── BeianInfo.java              # 备案信息实体
│   └── BeianResult.java            # 查询结果实体
└── service/
    ├── BeianChecker.java           # 备案查询服务
    ├── BeianMetricsService.java    # 指标服务
    └── BeianScheduler.java         # 调度服务
```

### 构建和测试

```bash
# 方式1：使用多阶段Docker构建（推荐，无需本地环境）
./build.sh

# 方式2：本地构建（需要Java 17+和Maven）
mvn clean compile test package

# 构建Docker镜像
docker build -t beian-exporter .
```

## 部署

### Docker Compose 部署

```bash
# 启动完整监控栈
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### Kubernetes 部署

参考 `k8s-manifests.yaml` 文件进行Kubernetes部署。

## 许可证

MIT License