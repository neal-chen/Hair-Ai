"""数据库初始化与种子数据导入"""

import json
import os
from datetime import datetime, timezone

from database import SessionLocal, init_db
from models import Hairstyle, HairColor


def seed_hairstyles():
    """导入发型种子数据"""
    db = SessionLocal()
    existing = db.query(Hairstyle).count()
    if existing > 0:
        print(f"发型库已有 {existing} 条数据，跳过导入")
        db.close()
        return

    seed_dir = os.path.join(os.path.dirname(__file__), "seed_data")
    now = datetime.now(timezone.utc)

    for gender, filename in [("女", "hairstyles_female.json"), ("男", "hairstyles_male.json")]:
        filepath = os.path.join(seed_dir, filename)
        if not os.path.exists(filepath):
            print(f"跳过：{filepath} 不存在")
            continue

        with open(filepath, "r", encoding="utf-8") as f:
            items = json.load(f)

        for item in items:
            hairstyle = Hairstyle(
                id=item["id"],
                category=item["category"],
                name=item["name"],
                description=item["description"],
                gender=item["gender"],
                tags=json.dumps(item.get("tags", []), ensure_ascii=False) if isinstance(item.get("tags"), list) else item.get("tags", "[]"),
                image_url=item.get("image_url", ""),
                sort_order=item["sort_order"],
                is_active=item.get("is_active", True),
                version=item.get("version", 1),
                created_at=now,
                updated_at=now,
            )
            db.add(hairstyle)

        print(f"发型 [{gender}]: 导入 {len(items)} 条")

    db.commit()
    db.close()


def seed_hair_colors():
    """导入发色种子数据"""
    db = SessionLocal()
    existing = db.query(HairColor).count()
    if existing > 0:
        print(f"发色库已有 {existing} 条数据，跳过导入")
        db.close()
        return

    seed_dir = os.path.join(os.path.dirname(__file__), "seed_data")
    filepath = os.path.join(seed_dir, "hair_colors.json")
    now = datetime.now(timezone.utc)

    if not os.path.exists(filepath):
        print(f"跳过：{filepath} 不存在")
        db.close()
        return

    with open(filepath, "r", encoding="utf-8") as f:
        items = json.load(f)

    for item in items:
        color = HairColor(
            id=item["id"],
            category=item["category"],
            name=item["name"],
            procedure=item.get("procedure", ""),
            color_hex=item.get("color_hex"),
            image_url=item.get("image_url", ""),
            sort_order=item["sort_order"],
            is_active=item.get("is_active", True),
            version=item.get("version", 1),
            created_at=now,
            updated_at=now,
        )
        db.add(color)

    db.commit()
    db.close()
    print(f"发色: 导入 {len(items)} 条")


def run():
    """运行所有种子数据导入"""
    print("=" * 40)
    print("初始化数据库...")
    init_db()
    print("导入发型数据...")
    seed_hairstyles()
    print("导入发色数据...")
    seed_hair_colors()
    print("=" * 40)
    print("完成！")


if __name__ == "__main__":
    run()
