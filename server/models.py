"""SQLAlchemy 数据模型"""

import json
import uuid
from datetime import datetime, timezone
from typing import List

from sqlalchemy import Column, String, Text, Integer, Boolean, BigInteger, DateTime
from database import Base


def _uuid() -> str:
    return str(uuid.uuid4())


def _now() -> datetime:
    return datetime.now(timezone.utc)


def serialize_tags(tags) -> str:
    """将 tags 序列化为 JSON 字符串"""
    if isinstance(tags, str):
        return tags
    if isinstance(tags, (list, tuple)):
        return json.dumps(tags, ensure_ascii=False)
    return "[]"


def deserialize_tags(tags_str: str) -> List[str]:
    """将 JSON 字符串反序列化为列表"""
    if not tags_str:
        return []
    try:
        return json.loads(tags_str)
    except (json.JSONDecodeError, TypeError):
        return []
    return datetime.now(timezone.utc)


class Hairstyle(Base):
    """发型库表"""
    __tablename__ = "hairstyles"

    id = Column(String(36), primary_key=True, default=_uuid)
    category = Column(String(50), nullable=False, index=True)
    name = Column(String(100), nullable=False)
    description = Column(Text, nullable=False, default="")
    gender = Column(String(4), nullable=False, index=True)       # "男" / "女"
    tags = Column(Text, nullable=False, default="[]")             # JSON 数组
    image_url = Column(String(500), nullable=False, default="")
    sort_order = Column(Integer, nullable=False, default=0)
    is_active = Column(Boolean, nullable=False, default=True, index=True)
    version = Column(BigInteger, nullable=False, default=1, index=True)
    created_at = Column(DateTime, nullable=False, default=_now)
    updated_at = Column(DateTime, nullable=False, default=_now, onupdate=_now)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "category": self.category,
            "name": self.name,
            "description": self.description,
            "gender": self.gender,
            "tags": self.tags,
            "image_url": self.image_url,
            "sort_order": self.sort_order,
            "is_active": self.is_active,
            "version": self.version,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }


class HairColor(Base):
    """发色库表"""
    __tablename__ = "hair_colors"

    id = Column(String(36), primary_key=True, default=_uuid)
    category = Column(String(50), nullable=False, index=True)
    name = Column(String(100), nullable=False)
    procedure = Column(Text, nullable=False, default="")
    color_hex = Column(String(7), nullable=True)
    image_url = Column(String(500), nullable=False, default="")
    sort_order = Column(Integer, nullable=False, default=0)
    is_active = Column(Boolean, nullable=False, default=True, index=True)
    version = Column(BigInteger, nullable=False, default=1, index=True)
    created_at = Column(DateTime, nullable=False, default=_now)
    updated_at = Column(DateTime, nullable=False, default=_now, onupdate=_now)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "category": self.category,
            "name": self.name,
            "procedure": self.procedure,
            "color_hex": self.color_hex,
            "image_url": self.image_url,
            "sort_order": self.sort_order,
            "is_active": self.is_active,
            "version": self.version,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }


class DeviceSyncLog(Base):
    """终端同步记录表"""
    __tablename__ = "device_sync_log"

    id = Column(Integer, primary_key=True, autoincrement=True)
    device_id = Column(String(64), nullable=False, unique=True, index=True)
    device_name = Column(String(200), nullable=True)
    last_hairstyle_version = Column(BigInteger, nullable=False, default=0)
    last_color_version = Column(BigInteger, nullable=False, default=0)
    last_sync_at = Column(DateTime, nullable=False, default=_now)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "device_id": self.device_id,
            "device_name": self.device_name,
            "last_hairstyle_version": self.last_hairstyle_version,
            "last_color_version": self.last_color_version,
            "last_sync_at": self.last_sync_at.isoformat() if self.last_sync_at else None,
        }
