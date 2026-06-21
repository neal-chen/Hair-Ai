"""Pydantic 请求/响应模型"""

from datetime import datetime
from typing import Optional, List
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


class HairstyleOut(BaseModel):
    id: str
    category: str
    name: str
    description: str
    gender: str
    tags: str
    image_url: str
    sort_order: int
    is_active: bool
    version: int

    model_config = {"from_attributes": True}


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


class HairColorOut(BaseModel):
    id: str
    category: str
    name: str
    procedure: str
    color_hex: Optional[str]
    image_url: str
    sort_order: int
    is_active: bool
    version: int

    model_config = {"from_attributes": True}


# ── 同步 ──

class SyncRequest(BaseModel):
    device_id: str
    device_name: Optional[str] = None
    hairstyle_version: int = 0
    color_version: int = 0


class HairstyleSyncData(BaseModel):
    id: str
    category: str
    name: str
    description: str
    gender: str
    tags: str
    image_url: str
    sort_order: int
    is_active: bool
    version: int


class HairColorSyncData(BaseModel):
    id: str
    category: str
    name: str
    procedure: str
    color_hex: Optional[str]
    image_url: str
    sort_order: int
    is_active: bool
    version: int


class SyncResponse(BaseModel):
    success: bool
    data: dict
    message: str = ""


# ── 通用 ──

class ApiResponse(BaseModel):
    success: bool
    data: Optional[dict] = None
    message: str = ""
