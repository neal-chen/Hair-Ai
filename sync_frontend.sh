#!/usr/bin/env bash
# ============================================================
# 前端同步脚本
# 将 faxin/（开发目录）同步到 Android assets/web/（部署目录）
#
# 用法:
#   ./sync_frontend.sh           # 同步 common 资源
#   ./sync_frontend.sh --diff    # 只显示差异不复制
#   ./sync_frontend.sh --help    # 帮助
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FAXIN="$SCRIPT_DIR/faxin"
ASSETS="$SCRIPT_DIR/hairstyle_android_pad-main/app/src/main/assets/web"

MODE="${1:-sync}"

if [ "$MODE" = "--help" ]; then
    sed -n '2,11p' "$0"
    exit 0
fi

# 确保目标目录存在
mkdir -p "$ASSETS/js" "$ASSETS/css" "$ASSETS/images"

echo "═══════════════════════════════════════════"
echo "  前端同步: faxin/ → assets/web/"
echo "═══════════════════════════════════════════"

# ── 1. CSS ──
echo ""
echo "📦 CSS"
for f in style.css; do
    if [ -f "$FAXIN/css/$f" ]; then
        if [ "$MODE" = "--diff" ]; then
            diff -q "$FAXIN/css/$f" "$ASSETS/css/$f" 2>/dev/null || echo "  ⚠  $f 有差异"
        else
            cp "$FAXIN/css/$f" "$ASSETS/css/$f"
            echo "  ✅ $f"
        fi
    fi
done

# ── 2. JS（通用模块）──
echo ""
echo "📦 JS"
for f in android-bridge.js; do
    if [ -f "$FAXIN/js/$f" ]; then
        if [ "$MODE" = "--diff" ]; then
            diff -q "$FAXIN/js/$f" "$ASSETS/js/$f" 2>/dev/null || echo "  ⚠  $f 有差异"
        else
            cp "$FAXIN/js/$f" "$ASSETS/js/$f"
            echo "  ✅ $f"
        fi
    fi
done

# ── 3. 图片（只同步共有文件）──
echo ""
echo "📦 Images"
count=0
for f in "$FAXIN/images/"*; do
    fname=$(basename "$f")
    target="$ASSETS/images/$fname"
    if [ -f "$target" ]; then
        if [ "$MODE" = "--diff" ]; then
            diff -q "$f" "$target" 2>/dev/null || echo "  ⚠  $fname 有差异"
        else
            cp "$f" "$target"
            count=$((count + 1))
        fi
    fi
done
[ "$MODE" != "--diff" ] && echo "  ✅ $count 个图片已同步"

# ── 4. 结构概览 ──
echo ""
echo "═══════════════════════════════════════════"
echo "  结构对比"
echo ""
echo "faxin/ (开发目录 — SPA 版本)"
echo "  ├── index.html          # 单页入口 (Hash 路由)"
echo "  ├── css/style.css       # 样式"
echo "  ├── js/"
echo "  │   ├── android-bridge.js  # Android 桥接"
echo "  │   ├── state.js           # 全局状态"
echo "  │   ├── router.js          # Hash 路由"
echo "  │   ├── pages.js           # 8 个页面渲染"
echo "  │   ├── carousel.js        # 轮播组件"
echo "  │   └── colorwheel.js      # 色盘组件"
echo "  └── images/             # 图片资源"
echo ""
echo "assets/web/ (部署目录 — 多页 HTML 版本)"
echo "  ├── index.html          # 登录页"
echo "  ├── home.html           # 首页"
echo "  ├── page1.html / page2.html"
echo "  ├── faselib.html / faxcl.html / faxlist.html / faxse.html"
echo "  ├── 3d.html             # 3D 展示"
echo "  ├── css/ + js/ + images/"
echo ""
echo "📝 注意:"
echo "  两个版本架构不同（SPA vs 多页HTML），当前只同步共有资源。"
echo "  faxin/ 是新一代 SPA，assets/web/ 是生产版本。"
echo "═══════════════════════════════════════════"
