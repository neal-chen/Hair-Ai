#!/usr/bin/env bash
# ============================================================
# API 回归测试
# ============================================================

set -u
BASE="${1:-http://localhost:8000}"
KEY="${ADMIN_API_KEY:-hair-admin-dev-2026}"
P=0; F=0

GRN='\033[0;32m'; RED='\033[0;31m'; RST='\033[0m'

pass() { P=$((P+1)); echo -e "  ${GRN}✓${RST} $1"; }
fail() { F=$((F+1)); echo -e "  ${RED}✗${RST} $1"; }

ck_len() {
    local out
    out=$(curl -s "$1" 2>/dev/null | python3 -c "import sys,json; print(len(json.load(sys.stdin)$2))" 2>/dev/null)
    if [ "$out" = "$3" ]; then pass "$4"; else fail "$4"; echo "    期望: $3, 实际: $out"; fi
}

ck_code() {
    local c; c=$(curl -s -o /dev/null -w "%{http_code}" "$2" 2>/dev/null)
    if [ "$c" = "$1" ]; then pass "$3"; else fail "$3"; echo "    HTTP $c ≠ $1"; fi
}

ck_contain() {
    if curl -s "$1" 2>/dev/null | grep -q "$2"; then pass "$3"; else fail "$3"; fi
}

echo "════════════════════════════════════════"
echo "  发型/发色库 API 回归测试 — $BASE"
echo "════════════════════════════════════════"

# 1. 健康检查
HEALTH=$(curl -s "$BASE/api/health" | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null)
[ "$HEALTH" = "ok" ] && pass "1.1 GET /api/health → ok" || fail "1.1 health → $HEALTH"
ck_code 200 "$BASE/api/health" "1.2 状态码 200"

# 2. 发型列表
ck_len "$BASE/api/hairstyles?gender=%E5%A5%B3&version=0&device_id=t" "['data']['hairstyles']" "104" "2.1 女发 ≥ 102"
ck_len "$BASE/api/hairstyles?gender=%E7%94%B7&version=0&device_id=t" "['data']['hairstyles']" "24" "2.2 男发 24 款"
VERSION_MAX=$(curl -s "$BASE/api/hairstyles?gender=%E5%A5%B3&version=0&device_id=t" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['current_version'])" 2>/dev/null)
ck_len "$BASE/api/hairstyles?version=$VERSION_MAX&device_id=t" "['data']['hairstyles']" "0" "2.3 增量 version=$VERSION_MAX → 0"

# 3. 搜索
ck_len "$BASE/api/hairstyles?q=%E6%B3%A2%E6%B5%AA&version=0&device_id=t" "['data']['hairstyles']" "5" "3.1 搜索「波浪」=5"
ck_len "$BASE/api/hairstyles?q=%E9%9F%A9%E5%BC%8F&version=0&device_id=t" "['data']['hairstyles']" "19" "3.2 搜索「韩式」≥ 18"
ck_contain "$BASE/api/hair-colors?q=%E7%81%B0&version=0&device_id=t" "灰" "3.3 搜索发色含「灰」"

# 4. 详情
ck_contain "$BASE/api/hairstyles/f-0001" "复古" "4.1 发型详情含复古"
ck_contain "$BASE/api/hair-colors/c-0001" "蜂蜜" "4.2 发色详情含蜂蜜"
NOT_FOUND=$(curl -s "$BASE/api/hairstyles/not-exist" | python3 -c "import sys,json; print(json.load(sys.stdin)['message'])" 2>/dev/null)
[ "$NOT_FOUND" = "发型不存在" ] && pass "4.3 不存在 → 404" || fail "4.3 404 → $NOT_FOUND"

# 5. 发色库
ck_len "$BASE/api/hair-colors?version=0&device_id=t" "['data']['colors']" "134" "5.1 发色 134 种"

# 6. 认证
LOGIN_CHECK=$(curl -s -X POST "$BASE/api/admin/login" -H "Content-Type: application/json" \
    -d "{\"api_key\":\"$KEY\"}" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['success'])" 2>/dev/null)
[ "$LOGIN_CHECK" = "True" ] && pass "6.1 登录成功" || fail "6.1 登录失败"

LOGIN_FAIL=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/admin/login" \
    -H "Content-Type: application/json" -d '{"api_key":"wrong"}')
[ "$LOGIN_FAIL" = "403" ] && pass "6.2 错误 Key → 403" || fail "6.2 错误 Key → $LOGIN_FAIL"

NOAUTH=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/admin/stats")
[ "$NOAUTH" = "401" ] && pass "6.3 无 token → 401" || fail "6.3 无 token → $NOAUTH"

# 7. CRUD
CREATE_ID=$(curl -s -X POST "$BASE/api/hairstyles" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $KEY" \
    -d '{"name":"测试发型","category":"韩式","gender":"女","sort_order":999}' | \
    python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('id',''))" 2>/dev/null)
[ -n "$CREATE_ID" ] && pass "7.1 创建发型成功" || fail "7.1 创建发型失败"

UPDATED=$(curl -s -X PUT "$BASE/api/hairstyles/$CREATE_ID" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $KEY" \
    -d '{"description":"已更新"}' | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('description',''))" 2>/dev/null)
[ "$UPDATED" = "已更新" ] && pass "7.2 更新成功" || fail "7.2 更新失败 → $UPDATED"

DELETED=$(curl -s -X DELETE "$BASE/api/hairstyles/$CREATE_ID" \
    -H "Authorization: Bearer $KEY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('message',''))" 2>/dev/null)
[ "$DELETED" = "已删除" ] && pass "7.3 删除成功" || fail "7.3 删除失败 → $DELETED"

DEL_CHECK=$(curl -s "$BASE/api/hairstyles/$CREATE_ID" | python3 -c "import sys,json; print(json.load(sys.stdin).get('message',''))" 2>/dev/null)
[ "$DEL_CHECK" = "发型不存在" ] && pass "7.4 删除后不可见" || fail "7.4 删除后仍可见 → $DEL_CHECK"

# 8. 终端同步
SYNC_CHECK=$(curl -s -X POST "$BASE/api/library/sync" \
    -H "Content-Type: application/json" \
    -d '{"device_id":"test-script","device_name":"测试脚本","hairstyle_version":5,"color_version":3}' | \
    python3 -c "import sys,json; print(json.load(sys.stdin).get('message',''))" 2>/dev/null)
[ "$SYNC_CHECK" = "sync recorded" ] && pass "8.1 同步上报成功" || fail "8.1 同步上报失败"

# 9. 管理统计
STATS_CHECK=$(curl -s -H "Authorization: Bearer $KEY" "$BASE/api/admin/stats" | \
    python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('hairstyle_count',0))" 2>/dev/null)
[ "$STATS_CHECK" -gt 0 ] 2>/dev/null && pass "9.1 统计正常 (共 $STATS_CHECK 发型)" || fail "9.1 统计失败"

DEV_CHECK=$(curl -s -H "Authorization: Bearer $KEY" "$BASE/api/admin/devices" | \
    python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('data',{}).get('devices',[])))" 2>/dev/null)
[ "$DEV_CHECK" -ge 0 ] 2>/dev/null && pass "9.2 设备列表正常 ($DEV_CHECK 个)" || fail "9.2 设备列表失败"

# 10. 管理后台
ADMIN_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/admin")
[ "$ADMIN_CODE" = "307" ] && pass "10.1 /admin → 307" || fail "10.1 /admin → $ADMIN_CODE"

ADMIN_HTML=$(curl -s "$BASE/static/admin/index.html" | grep -c "sidebar" 2>/dev/null || echo 0)
[ "$ADMIN_HTML" -gt 0 ] && pass "10.2 管理后台含 sidebar" || fail "10.2 管理后台页面异常"

# ── 汇总 ──
echo ""
echo "════════════════════════════════════════"
echo -e "  ${GRN}通过${RST}: $P   ${RED}失败${RST}: $F  总计: $((P+F))"
echo "════════════════════════════════════════"
[ "$F" -gt 0 ] && exit 1
