"""发型 & 发色库 API 服务端

FastAPI 应用，提供发型库和发色库的查询、增量同步、图片服务接口。
"""

import json
import os
from datetime import datetime, timezone
from typing import Optional

from fastapi import FastAPI, Depends, Query, HTTPException, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse, RedirectResponse
from fastapi.staticfiles import StaticFiles
from sqlalchemy.orm import Session
from sqlalchemy import func, or_

from database import get_db, init_db
from models import Hairstyle, HairColor, DeviceSyncLog
from auth import require_admin, ADMIN_API_KEY
from schemas import (
    HairstyleCreate, HairstyleUpdate, HairstyleOut,
    HairColorCreate, HairColorUpdate, HairColorOut,
    SyncRequest,
)

# ── 应用初始化 ──

app = FastAPI(
    title="发型/发色库 API",
    description="美容美发智能镜 - 发型库 & 发色库服务端",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 图片存储目录
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
HAIRSTYLE_IMG_DIR = os.path.join(BASE_DIR, "images", "hairstyles")
COLOR_IMG_DIR = os.path.join(BASE_DIR, "images", "hair_colors")

# ── 管理后台静态文件 ──
ADMIN_DIR = os.path.join(BASE_DIR, "admin")
os.makedirs(ADMIN_DIR, exist_ok=True)
app.mount("/static/admin", StaticFiles(directory=ADMIN_DIR), name="admin")


@app.get("/admin", include_in_schema=False)
@app.get("/admin/", include_in_schema=False)
def admin_redirect():
    return RedirectResponse(url="/static/admin/index.html")


@app.get("/", include_in_schema=False)
def root_redirect():
    return RedirectResponse(url="/static/admin/index.html")


@app.get("/favicon.ico", include_in_schema=False)
def favicon():
    return RedirectResponse(url="/static/admin/favicon.ico") if os.path.exists(
        os.path.join(ADMIN_DIR, "favicon.ico")
    ) else JSONResponse(status_code=204)


@app.on_event("startup")
def on_startup():
    """启动时初始化数据库"""
    os.makedirs(HAIRSTYLE_IMG_DIR, exist_ok=True)
    os.makedirs(COLOR_IMG_DIR, exist_ok=True)
    init_db()

    # 尝试自动导入种子数据
    try:
        from seed import seed_hairstyles, seed_hair_colors
        seed_hairstyles()
        seed_hair_colors()
    except ImportError:
        pass  # seed.py 不存在时跳过


# ═══════════════════════════════════════════════════════════
#  发型库 API
# ═══════════════════════════════════════════════════════════

@app.get("/api/hairstyles")
def list_hairstyles(
    gender: Optional[str] = Query(None, description="筛选性别：男/女"),
    category: Optional[str] = Query(None, description="筛选分类"),
    version: int = Query(0, description="客户端当前版本号，用于增量同步"),
    device_id: str = Query(..., description="终端设备标识"),
    db: Session = Depends(get_db),
):
    """获取发型库列表（支持增量同步）"""
    query = db.query(Hairstyle)

    # 版本过滤：version=0 返回全部，>0 只返回更新的
    if version > 0:
        query = query.filter(Hairstyle.version > version)
    else:
        query = query.filter(Hairstyle.is_active == True)

    if gender:
        query = query.filter(Hairstyle.gender == gender)
    if category:
        query = query.filter(Hairstyle.category == category)

    items = query.order_by(Hairstyle.sort_order).all()

    # 获取当前最大版本号
    max_version = db.query(Hairstyle.version).order_by(Hairstyle.version.desc()).first()
    current_version = max_version[0] if max_version else 0

    # 如果是增量同步，查出已删除的 ID
    deleted_ids = []
    if version > 0:
        deleted = db.query(Hairstyle.id).filter(
            Hairstyle.version > version,
            Hairstyle.is_active == False
        ).all()
        deleted_ids = [d[0] for d in deleted]

    return {
        "success": True,
        "data": {
            "hairstyles": [item.to_dict() for item in items if item.is_active],
            "deleted_ids": deleted_ids,
            "current_version": current_version,
            "server_time": datetime.now(timezone.utc).isoformat(),
        },
    }


@app.get("/api/hairstyles/{item_id}")
def get_hairstyle(item_id: str, db: Session = Depends(get_db)):
    """获取单个发型详情"""
    item = db.query(Hairstyle).filter(Hairstyle.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="发型不存在")
    return {"success": True, "data": item.to_dict()}


@app.post("/api/hairstyles")
def create_hairstyle(data: HairstyleCreate, db: Session = Depends(get_db), _: bool = Depends(require_admin)):
    """新增发型（管理后台用）"""
    import uuid
    now = datetime.now(timezone.utc)

    # 获取当前最大版本号
    max_ver = db.query(func.max(Hairstyle.version)).scalar() or 0

    item = Hairstyle(
        id=str(uuid.uuid4()),
        category=data.category,
        name=data.name,
        description=data.description,
        gender=data.gender,
        tags=data.tags,
        image_url=data.image_url,
        sort_order=data.sort_order,
        version=max_ver + 1,
        created_at=now,
        updated_at=now,
    )
    db.add(item)
    db.commit()
    db.refresh(item)
    return {"success": True, "data": item.to_dict()}


@app.put("/api/hairstyles/{item_id}")
def update_hairstyle(item_id: str, data: HairstyleUpdate, db: Session = Depends(get_db), _: bool = Depends(require_admin)):
    """更新发型（管理后台用）"""
    item = db.query(Hairstyle).filter(Hairstyle.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="发型不存在")

    update_data = data.model_dump(exclude_unset=True)
    if update_data:
        for field, value in update_data.items():
            setattr(item, field, value)
        item.version += 1
        item.updated_at = datetime.now(timezone.utc)
        db.commit()
        db.refresh(item)

    return {"success": True, "data": item.to_dict()}


@app.delete("/api/hairstyles/{item_id}")
def delete_hairstyle(item_id: str, db: Session = Depends(get_db), _: bool = Depends(require_admin)):
    """软删除发型（管理后台用）"""
    item = db.query(Hairstyle).filter(Hairstyle.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="发型不存在")
    item.is_active = False
    item.version += 1
    item.updated_at = datetime.now(timezone.utc)
    db.commit()
    return {"success": True, "message": "已删除"}


# ═══════════════════════════════════════════════════════════
#  发色库 API
# ═══════════════════════════════════════════════════════════

@app.get("/api/hair-colors")
def list_hair_colors(
    category: Optional[str] = Query(None, description="筛选色系"),
    version: int = Query(0, description="客户端当前版本号"),
    device_id: str = Query(..., description="终端设备标识"),
    db: Session = Depends(get_db),
):
    """获取发色库列表（支持增量同步）"""
    query = db.query(HairColor)

    if version > 0:
        query = query.filter(HairColor.version > version)
    else:
        query = query.filter(HairColor.is_active == True)

    if category:
        query = query.filter(HairColor.category == category)

    items = query.order_by(HairColor.sort_order).all()

    max_version = db.query(HairColor.version).order_by(HairColor.version.desc()).first()
    current_version = max_version[0] if max_version else 0

    deleted_ids = []
    if version > 0:
        deleted = db.query(HairColor.id).filter(
            HairColor.version > version,
            HairColor.is_active == False
        ).all()
        deleted_ids = [d[0] for d in deleted]

    return {
        "success": True,
        "data": {
            "colors": [item.to_dict() for item in items if item.is_active],
            "deleted_ids": deleted_ids,
            "current_version": current_version,
            "server_time": datetime.now(timezone.utc).isoformat(),
        },
    }


@app.get("/api/hair-colors/{item_id}")
def get_hair_color(item_id: str, db: Session = Depends(get_db)):
    """获取单个发色详情"""
    item = db.query(HairColor).filter(HairColor.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="发色不存在")
    return {"success": True, "data": item.to_dict()}


@app.post("/api/hair-colors")
def create_hair_color(data: HairColorCreate, db: Session = Depends(get_db), _: bool = Depends(require_admin)):
    """新增发色（管理后台用）"""
    import uuid
    now = datetime.now(timezone.utc)

    max_ver = db.query(func.max(HairColor.version)).scalar() or 0

    item = HairColor(
        id=str(uuid.uuid4()),
        category=data.category,
        name=data.name,
        procedure=data.procedure,
        color_hex=data.color_hex,
        image_url=data.image_url,
        sort_order=data.sort_order,
        version=max_ver + 1,
        created_at=now,
        updated_at=now,
    )
    db.add(item)
    db.commit()
    db.refresh(item)
    return {"success": True, "data": item.to_dict()}


@app.put("/api/hair-colors/{item_id}")
def update_hair_color(item_id: str, data: HairColorUpdate, db: Session = Depends(get_db), _: bool = Depends(require_admin)):
    """更新发色（管理后台用）"""
    item = db.query(HairColor).filter(HairColor.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="发色不存在")

    update_data = data.model_dump(exclude_unset=True)
    if update_data:
        for field, value in update_data.items():
            setattr(item, field, value)
        item.version += 1
        item.updated_at = datetime.now(timezone.utc)
        db.commit()
        db.refresh(item)

    return {"success": True, "data": item.to_dict()}


@app.delete("/api/hair-colors/{item_id}")
def delete_hair_color(item_id: str, db: Session = Depends(get_db), _: bool = Depends(require_admin)):
    """软删除发色（管理后台用）"""
    item = db.query(HairColor).filter(HairColor.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="发色不存在")
    item.is_active = False
    item.version += 1
    item.updated_at = datetime.now(timezone.utc)
    db.commit()
    return {"success": True, "message": "已删除"}


# ═══════════════════════════════════════════════════════════
#  图片服务 API
# ═══════════════════════════════════════════════════════════

@app.get("/api/hairstyles/{item_id}/image")
def get_hairstyle_image(item_id: str, db: Session = Depends(get_db)):
    """获取发型图片"""
    item = db.query(Hairstyle).filter(Hairstyle.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="发型不存在")

    # 优先返回本地图片
    local_path = os.path.join(HAIRSTYLE_IMG_DIR, f"{item_id}.png")
    if os.path.exists(local_path):
        return FileResponse(local_path, media_type="image/png")

    # 如果有远程 URL 则重定向
    if item.image_url:
        from fastapi.responses import RedirectResponse
        return RedirectResponse(url=item.image_url)

    raise HTTPException(status_code=404, detail="图片不存在")


@app.get("/api/hair-colors/{item_id}/image")
def get_hair_color_image(item_id: str, db: Session = Depends(get_db)):
    """获取发色图片"""
    item = db.query(HairColor).filter(HairColor.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="发色不存在")

    local_path = os.path.join(COLOR_IMG_DIR, f"{item_id}.png")
    if os.path.exists(local_path):
        return FileResponse(local_path, media_type="image/png")

    if item.image_url:
        from fastapi.responses import RedirectResponse
        return RedirectResponse(url=item.image_url)

    raise HTTPException(status_code=404, detail="图片不存在")


@app.post("/api/hairstyles/{item_id}/image")
async def upload_hairstyle_image(item_id: str, file: UploadFile = File(...), db: Session = Depends(get_db), _: bool = Depends(require_admin)):
    """上传发型图片（管理后台用）"""
    item = db.query(Hairstyle).filter(Hairstyle.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="发型不存在")

    ext = os.path.splitext(file.filename or ".png")[1] or ".png"
    save_path = os.path.join(HAIRSTYLE_IMG_DIR, f"{item_id}{ext}")
    content = await file.read()
    with open(save_path, "wb") as f:
        f.write(content)

    # 更新 image_url 为本地路径
    item.image_url = f"/api/hairstyles/{item_id}/image"
    item.version += 1
    item.updated_at = datetime.now(timezone.utc)
    db.commit()

    return {"success": True, "data": {"image_url": item.image_url}}


@app.post("/api/hair-colors/{item_id}/image")
async def upload_hair_color_image(item_id: str, file: UploadFile = File(...), db: Session = Depends(get_db), _: bool = Depends(require_admin)):
    """上传发色图片（管理后台用）"""
    item = db.query(HairColor).filter(HairColor.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="发色不存在")

    ext = os.path.splitext(file.filename or ".png")[1] or ".png"
    save_path = os.path.join(COLOR_IMG_DIR, f"{item_id}{ext}")
    content = await file.read()
    with open(save_path, "wb") as f:
        f.write(content)

    item.image_url = f"/api/hair-colors/{item_id}/image"
    item.version += 1
    item.updated_at = datetime.now(timezone.utc)
    db.commit()

    return {"success": True, "data": {"image_url": item.image_url}}


# ═══════════════════════════════════════════════════════════
#  终端同步 API
# ═══════════════════════════════════════════════════════════

@app.post("/api/library/sync")
def report_sync(data: SyncRequest, db: Session = Depends(get_db)):
    """终端上报同步状态"""
    now = datetime.now(timezone.utc)

    record = db.query(DeviceSyncLog).filter(
        DeviceSyncLog.device_id == data.device_id
    ).first()

    if record:
        record.last_hairstyle_version = data.hairstyle_version
        record.last_color_version = data.color_version
        record.last_sync_at = now
        if data.device_name:
            record.device_name = data.device_name
    else:
        record = DeviceSyncLog(
            device_id=data.device_id,
            device_name=data.device_name or "",
            last_hairstyle_version=data.hairstyle_version,
            last_color_version=data.color_version,
            last_sync_at=now,
        )
        db.add(record)

    db.commit()

    return {"success": True, "message": "sync recorded"}


# ═══════════════════════════════════════════════════════════
#  管理统计 API
# ═══════════════════════════════════════════════════════════

@app.get("/api/admin/stats")
def get_stats(db: Session = Depends(get_db), _: bool = Depends(require_admin)):
    """运营统计"""
    hairstyle_count = db.query(Hairstyle).filter(Hairstyle.is_active == True).count()
    color_count = db.query(HairColor).filter(HairColor.is_active == True).count()
    device_count = db.query(DeviceSyncLog).count()

    # 各性别发型数量
    male_count = db.query(Hairstyle).filter(Hairstyle.gender == "男", Hairstyle.is_active == True).count()
    female_count = db.query(Hairstyle).filter(Hairstyle.gender == "女", Hairstyle.is_active == True).count()

    # 发型分类统计
    categories_raw = db.query(
        Hairstyle.category, Hairstyle.gender, func.count(Hairstyle.id)
    ).filter(
        Hairstyle.is_active == True
    ).group_by(Hairstyle.category, Hairstyle.gender).all()
    categories = {}
    for row in categories_raw:
        key = f"{row.gender}_{row.category}"
        categories[key] = row[2]

    # 发色色系统计
    color_categories_raw = db.query(
        HairColor.category, func.count(HairColor.id)
    ).filter(
        HairColor.is_active == True
    ).group_by(HairColor.category).all()
    color_categories = {}
    for row in color_categories_raw:
        color_categories[row.category] = row[1]

    return {
        "success": True,
        "data": {
            "hairstyle_count": hairstyle_count,
            "hair_color_count": color_count,
            "device_count": device_count,
            "male_hairstyles": male_count,
            "female_hairstyles": female_count,
            "hairstyle_categories": categories,
            "hair_color_categories": color_categories,
        },
    }


@app.get("/api/admin/devices")
def list_devices(db: Session = Depends(get_db), _: bool = Depends(require_admin)):
    """查看所有已注册终端"""
    devices = db.query(DeviceSyncLog).order_by(DeviceSyncLog.last_sync_at.desc()).all()
    return {
        "success": True,
        "data": {
            "devices": [d.to_dict() for d in devices]
        }
    }


# ── 管理后台认证 ──

@app.post("/api/admin/login")
def admin_login(body: dict):
    """管理后台登录验证"""
    key = body.get("api_key", "")
    if key == ADMIN_API_KEY:
        return {"success": True, "data": {"token": key, "message": "验证成功"}}
    raise HTTPException(status_code=403, detail="API Key 无效")


# ── 健康检查 ──

@app.get("/api/health")
def health_check():
    return {"status": "ok", "service": "hair-library-api", "version": "1.0.0"}


# ── 直接启动 ──

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
