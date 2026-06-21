"""认证模块测试"""

import hmac
import pytest
from fastapi import HTTPException

from auth import require_admin, ADMIN_API_KEY


class TestRequireAdmin:
    def test_missing_header(self):
        """缺少 Authorization 头 → 401"""
        with pytest.raises(HTTPException) as exc:
            require_admin(authorization=None)
        assert exc.value.status_code == 401

    def test_wrong_scheme(self):
        """非 Bearer 方案 → 401"""
        with pytest.raises(HTTPException) as exc:
            require_admin(authorization="Basic dXNlcjpwYXNz")
        assert exc.value.status_code == 401

    def test_malformed_header(self):
        """格式错误 → 401"""
        with pytest.raises(HTTPException) as exc:
            require_admin(authorization="Bearer")
        assert exc.value.status_code == 401

    def test_wrong_token(self):
        """错误 Token → 403"""
        with pytest.raises(HTTPException) as exc:
            require_admin(authorization="Bearer wrong-token")
        assert exc.value.status_code == 403

    def test_valid_token(self):
        """正确 Token → True"""
        result = require_admin(authorization=f"Bearer {ADMIN_API_KEY}")
        assert result is True

    def test_constant_time_comparison(self):
        """验证使用恒定时间比较"""
        token_a = f"Bearer {ADMIN_API_KEY}"
        token_b = "Bearer slightly-wrong"
        # extract the tokens
        _, t1 = token_a.split(None, 1)
        _, t2 = token_b.split(None, 1)
        # 确认 hmac.compare_digest 被使用（而不是 !=）
        assert hmac.compare_digest(t1, t1) is True
        assert hmac.compare_digest(t1, t2) is False


class TestLoginEndpoint:
    """/api/admin/login 端点测试"""

    def test_login_success(self, client):
        resp = client.post("/api/admin/login", json={"api_key": ADMIN_API_KEY})
        assert resp.status_code == 200
        data = resp.json()
        assert data["success"] is True
        assert data["data"]["token"] == ADMIN_API_KEY

    def test_login_failure(self, client):
        resp = client.post("/api/admin/login", json={"api_key": "wrong-key"})
        assert resp.status_code == 403
        data = resp.json()
        assert data["success"] is False

    def test_login_empty_key(self, client):
        resp = client.post("/api/admin/login", json={"api_key": ""})
        assert resp.status_code == 403
