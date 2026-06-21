"""Pydantic 请求/响应模型（仅保留实际使用的模型）"""

from typing import Optional
from pydantic import BaseModel


# ── 发型 ──

class HairstyleCreate(BaseModel):
    category: str
    name: str
    description: str = ""
    gender: str
    tags: str = "[]"
    image_url: str = ""
    sort_order: int = 0


class HairstyleUpdate(BaseModel):
    category: Optional[str] = None
    name: Optional[str] = None
    description: Optional[str] = None
    gender: Optional[str] = None
    tags: Optional[str] = None
    image_url: Optional[str] = None
    sort_order: Optional[int] = None
    is_active: Optional[bool] = None


# ── 发色 ──

class HairColorCreate(BaseModel):
    category: str
    name: str
    procedure: str = ""
    color_hex: Optional[str] = None
    image_url: str = ""
    sort_order: int = 0


class HairColorUpdate(BaseModel):
    category: Optional[str] = None
    name: Optional[str] = None
    procedure: Optional[str] = None
    color_hex: Optional[str] = None
    image_url: Optional[str] = None
    sort_order: Optional[int] = None
    is_active: Optional[bool] = None


# ── 同步 ──

class SyncRequest(BaseModel):
    device_id: str
    device_name: Optional[str] = None
    hairstyle_version: int = 0
    color_version: int = 0
