#!/bin/bash

# IBIOS 启动脚本
# 用法：./start.sh [配置文件]
# 示例：./start.sh ../ibios.yaml

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
# 固定工作目录：config/application.yml 与 ibios.db 都相对项目根解析
cd "$PROJECT_DIR" || exit 1
JAR_FILE="$PROJECT_DIR/ibios-boot/target/ibios-boot-0.0.1-SNAPSHOT.jar"
PID_FILE="$SCRIPT_DIR/ibios.pid"
LOG_FILE="$SCRIPT_DIR/ibios.log"
CONFIG_FILE="${1:-}"

# 检查 JAR 文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR 文件不存在：$JAR_FILE"
    echo "请先运行：mvn clean install -DskipTests"
    exit 1
fi

# 检查配置文件
if [ -n "$CONFIG_FILE" ]; then
    if [ ! -f "$CONFIG_FILE" ]; then
        echo " 配置文件不存在：$CONFIG_FILE"
        exit 1
    fi
    echo "📄 使用配置文件：$CONFIG_FILE"
    SPRING_CONFIG_ARG="--spring.config.additional-location=file:$CONFIG_FILE"
else
    echo "⚠️  未指定配置文件，使用默认配置（需要 DASHSCOPE_API_KEY 环境变量）"
    SPRING_CONFIG_ARG=""
fi

# 检查是否已经在运行
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "⚠️  IBIOS 已经在运行中 (PID: $PID)"
        exit 1
    else
        echo "🧹 清理过期的 PID 文件"
        rm -f "$PID_FILE"
    fi
fi

# 启动服务
echo "🚀 启动 IBIOS Web Service..."
echo "   JAR: $JAR_FILE"
echo "   日志：$LOG_FILE"

# 使用 Java 23（Homebrew 安装）
JAVA_CMD="/opt/homebrew/Cellar/openjdk/23.0.2/libexec/openjdk.jdk/Contents/Home/bin/java"

if [ ! -f "$JAVA_CMD" ]; then
    echo "⚠️  Java 23 not found at $JAVA_CMD, using default java"
    JAVA_CMD="java"
fi

nohup "$JAVA_CMD" -jar "$JAR_FILE" serve $SPRING_CONFIG_ARG > "$LOG_FILE" 2>&1 &
PID=$!

echo $PID > "$PID_FILE"
echo "✅ IBIOS 已启动 (PID: $PID)"
echo "   管理平台：http://localhost:8080/admin"
echo "   API 文档：http://localhost:8080/swagger-ui.html"
echo "   日志文件：$LOG_FILE"
