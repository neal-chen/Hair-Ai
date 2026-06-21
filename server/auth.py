"""API 认证模块

提供 Bearer Token 认证依赖，保护管理端 API 端点。
"""

import hmac
import os
from typing import Optional

from fastapi import Header, HTTPException, status

# 管理后台 API 密钥（从环境变量读取，提供开发默认值）
ADMIN_API_KEY = os.environ.get("ADMIN_API_KEY", "hair-admin-dev-2026")


def require_admin(authorization: Optional[str] = Header(None)):
    """验证管理后台 Bearer Token

    在需要保护的路由上添加 `Depends(require_admin)` 即可。
    """
    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="缺少 Authorization 请求头",
            headers={"WWW-Authenticate": "Bearer"},
        )

    parts = authorization.split(None, 1)  # ["Bearer", "token"]
    if len(parts) != 2:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authorization 格式错误，应为: Bearer <token>",
        )

    scheme, token = parts
    if scheme.lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="认证方案不支持，仅支持 Bearer",
        )

    # 使用恒定时间比较，防止时序攻击
    if not hmac.compare_digest(token, ADMIN_API_KEY):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Token 无效",
        )

    return True
