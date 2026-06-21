"""数据模型测试"""

import re
import uuid as _uuid
from datetime import datetime, timezone

from models import serialize_tags, deserialize_tags, Hairstyle, HairColor


class TestSerializers:
    def test_serialize_tags_list(self):
        assert serialize_tags(["卷发", "韩式"]) == '["卷发","韩式"]'

    def test_serialize_tags_str(self):
        assert serialize_tags('["existing"]') == '["existing"]'

    def test_serialize_tags_empty(self):
        assert serialize_tags([]) == "[]"

    def test_deserialize_tags_valid(self):
        assert deserialize_tags('["卷发","韩式"]') == ["卷发", "韩式"]

    def test_deserialize_tags_empty(self):
        assert deserialize_tags("") == []

    def test_deserialize_tags_invalid(self):
        assert deserialize_tags("not-json") == []

    def test_deserialize_tags_none(self):
        assert deserialize_tags(None) == []


class TestHairstyleModel:
    def test_table_name(self):
        assert Hairstyle.__tablename__ == "hairstyles"

    def test_to_dict_shape(self, seeded_db):
        hs = seeded_db.query(Hairstyle).first()
        d = hs.to_dict()
        assert isinstance(d, dict)
        assert d["name"] == "测试发型"
        assert d["gender"] == "女"
        assert d["category"] == "韩式"
        assert d["is_active"] is True
        assert d["version"] == 1
        # 日期格式
        assert d["created_at"] is not None
        assert "T" in d["created_at"]


class TestHairColorModel:
    def test_table_name(self):
        assert HairColor.__tablename__ == "hair_colors"

    def test_to_dict_shape(self, seeded_db):
        hc = seeded_db.query(HairColor).first()
        d = hc.to_dict()
        assert d["name"] == "测试发色"
        assert d["category"] == "温感色系"
        assert d["color_hex"] == "#C4925A"

    def test_procedure_default(self):
        """procedure 默认值为空"""
        hc = HairColor(name="test", category="test")
        # SQLAlchemy Column default 只作用于 INSERT，Python 层面为 None
        assert hc.procedure is None


class TestUUID:
    def test_uuid_format(self):
        from models import _uuid
        val = _uuid()
        # UUID v4: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
        assert re.match(r"^[0-9a-f\-]{36}$", val)
        parts = val.split("-")
        assert len(parts) == 5
        assert parts[2][0] == "4"  # version 4


class TestNow:
    def test_now_returns_utc(self):
        from models import _now
        now = _now()
        assert now.tzinfo is not None
        assert now.tzinfo.utcoffset(now).total_seconds() == 0  # UTC
