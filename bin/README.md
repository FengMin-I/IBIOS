# IBIOS 启动脚本

## 快速开始

### 1. 首次使用

```bash
# 复制配置文件模板
cp ibios.yaml.example ibios.yaml

# 编辑配置文件，填入你的 API Key
vim ibios.yaml
```

### 2. 启动服务

```bash
# 使用配置文件启动（推荐）
bin/start.sh ibios.yaml

# 或使用默认配置（需要 DASHSCOPE_API_KEY 环境变量）
export DASHSCOPE_API_KEY=your_key_here
bin/start.sh
```

### 3. 访问服务

- **管理平台**: http://localhost:8080/admin
- **API 文档**: http://localhost:8080/swagger-ui.html
- **健康检查**: http://localhost:8080/api/v1/health

### 4. 停止服务

```bash
bin/stop.sh
```

### 5. 查看日志

```bash
tail -f bin/ibios.log
```

## 配置文件格式

```yaml
# ibios.yaml
ibios:
  providers:
    - name: dashscope
      api-key: your_dashscope_api_key_here
      model: qwen-plus
```

## 注意事项

- 首次启动前需要运行 `mvn clean install -DskipTests` 构建项目
- 脚本会自动检测 Java 23（Homebrew 安装），如果未找到则使用默认 java 命令
- PID 文件保存在 `bin/ibios.pid`，日志保存在 `bin/ibios.log`
- 不要重复启动，脚本会检测是否已在运行
