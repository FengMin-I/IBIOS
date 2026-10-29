# IBIOS 启动脚本

## 快速开始

### 1. 配置凭证（环境变量）

凭证只通过环境变量提供，不写入任何配置文件：

```bash
export DASHSCOPE_API_KEY=your_dashscope_key
export DEEPSEEK_API_KEY=your_deepseek_key
# 可选：接入任意 OpenAI 兼容端点时使用
export OPENAI_COMPAT_API_KEY=your_endpoint_key
```

只需使用某个 Provider 时，设置对应的变量即可；只用 `mock` Provider 时可都不设，应用照常启动。

### 2. 启动服务

```bash
# 使用环境变量中的凭证启动
bin/start.sh

# 如需覆盖非敏感配置（端口、白名单等），传入外部配置文件
cp ibios.yaml.example ibios.yaml
bin/start.sh ibios.yaml
```

不要把 API Key 写进 `ibios.yaml`——示例文件里用的是 `${DASHSCOPE_API_KEY:}` 这类占位符，由环境变量在启动时解析。

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
# ibios.yaml（可选，仅非敏感配置）
ibios:
  providers:
    - name: dashscope
      api-key: ${DASHSCOPE_API_KEY:}
      model: qwen-plus
    # 任意 OpenAI 兼容端点（OpenAI / OpenRouter / vLLM / Ollama / 企业网关）
    - name: openai-compatible
      base-url: https://api.example.com/v1
      api-key: ${OPENAI_COMPAT_API_KEY:}
      model: your-model-id
```

`base-url` 存在时按 OpenAI 兼容协议构建（自动去掉末尾 `/v1`，避免拼出 `/v1/v1/chat/completions`）；`deepseek` 不填 `base-url` 时使用默认地址；其余未配置 `base-url` 的自定义 Provider 会被跳过。

## 注意事项

- 环境变量必须在启动前设置；服务运行中新增或修改不会生效，需要重启
- 首次启动时 Provider 从配置播种进 SQLite；之后以数据库为准，可在管理台或 `/api/v1/providers` 修改
- 首次启动前需要运行 `mvn clean install -DskipTests` 构建项目
- 脚本会自动检测 Java 23（Homebrew 安装），如果未找到则使用默认 java 命令
- PID 文件保存在 `bin/ibios.pid`，日志保存在 `bin/ibios.log`
- 不要重复启动，脚本会检测是否已在运行
