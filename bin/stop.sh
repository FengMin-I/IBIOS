#!/bin/bash

# IBIOS 停止脚本

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$SCRIPT_DIR/ibios.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "⚠️  PID 文件不存在，IBIOS 可能未运行"
    exit 0
fi

PID=$(cat "$PID_FILE")

if ps -p "$PID" > /dev/null 2>&1; then
    echo "🛑 停止 IBIOS (PID: $PID)..."
    kill "$PID"
    
    # 等待进程结束
    for i in {1..10}; do
        if ! ps -p "$PID" > /dev/null 2>&1; then
            break
        fi
        sleep 1
    done
    
    # 如果还在运行，强制终止
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "⚠️  强制终止进程..."
        kill -9 "$PID"
    fi
    
    echo "✅ IBIOS 已停止"
else
    echo "⚠️  进程 $PID 不存在"
fi

rm -f "$PID_FILE"
