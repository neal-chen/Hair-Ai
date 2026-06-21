#!/usr/bin/env bash
# ============================================================
# AI 换发型 / 换发色 智能镜系统 — 一键启动脚本
#
# 首次运行:
#   cp .env.example .env      # 配置环境变量
#   ./start.sh                # 启动服务端
#
# 选项:
#   ./start.sh --seed         # 强制重新导入种子数据
#   ./start.sh --port 8080    # 自定义端口 (默认 8000)
#   ./start.sh --help         # 帮助
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVER_DIR="$SCRIPT_DIR/server"
PORT=8000
SEED=false

# 解析参数
while [[ $# -gt 0 ]]; do
  case "$1" in
    --seed) SEED=true; shift ;;
    --port) PORT="$2"; shift 2 ;;
    --help) sed -n '2,11p' "$0"; exit 0 ;;
    *) echo "未知参数: $1"; exit 1 ;;
  esac
done

echo "╔══════════════════════════════════════════════╗"
echo "║  发型/发色库 API 服务端                      ║"
echo "╚══════════════════════════════════════════════╝"

# ── 加载 .env (如果存在) ──
if [ -f "$SCRIPT_DIR/.env" ]; then
  echo "📄 加载 .env 环境变量..."
  set -a; source "$SCRIPT_DIR/.env"; set +a
fi

# ── 检查 Python ──
PYTHON=$(command -v python3 || command -v python)
if [ -z "$PYTHON" ]; then
  echo "❌ 未找到 Python，请先安装 Python 3.10+"
  exit 1
fi
echo "🐍 Python: $($PYTHON --version)"

# ── 虚拟环境 ──
VENV="$SERVER_DIR/venv"
if [ ! -f "$VENV/bin/python" ]; then
  echo "📦 创建虚拟环境..."
  $PYTHON -m venv "$VENV"
fi

echo "📦 安装依赖..."
"$VENV/bin/pip" install -q --upgrade pip
"$VENV/bin/pip" install -q -r "$SERVER_DIR/requirements.txt"

# ── 初始化数据库 ──
DB_PATH="$SERVER_DIR/hair_library.db"
if [ "$SEED" = true ] || [ ! -f "$DB_PATH" ]; then
  echo "🗄️  初始化数据库..."
  "$VENV/bin/python" "$SERVER_DIR/seed.py"
else
  echo "🗄️  数据库已存在，跳过初始化"
fi

# ── 启动 ──
echo ""
echo "🚀 启动服务端 → http://localhost:$PORT"
echo "🌐 管理后台   → http://localhost:$PORT/admin"
echo "📖 API 文档   → http://localhost:$PORT/docs"
echo ""

"$VENV/bin/python" -m uvicorn main:app \
  --app-dir "$SERVER_DIR" \
  --host 0.0.0.0 \
  --port "$PORT" \
  --reload
