# 发型/发色库 API 服务端

美容美发智能镜配套服务端，提供发型库和发色库的统一管理与终端同步。

## 快速启动

```bash
cd server
pip install -r requirements.txt
python3 seed.py          # 首次运行：初始化数据库 + 导入种子数据
python3 -m uvicorn main:app --host 0.0.0.0 --port 8000
```

## API 端点

| 方法 | 端点 | 说明 |
|---|---|---|
| GET | `/api/health` | 健康检查 |
| GET | `/api/hairstyles?gender=&version=&device_id=` | 发型库列表（增量同步） |
| GET | `/api/hairstyles/{id}` | 单个发型详情 |
| GET | `/api/hairstyles/{id}/image` | 发型图片 |
| POST | `/api/hairstyles` | 新增发型（管理后台） |
| PUT | `/api/hairstyles/{id}` | 更新发型（管理后台） |
| DELETE | `/api/hairstyles/{id}` | 删除发型（软删除） |
| GET | `/api/hair-colors?category=&version=&device_id=` | 发色库列表（增量同步） |
| GET | `/api/hair-colors/{id}` | 单个发色详情 |
| GET | `/api/hair-colors/{id}/image` | 发色图片 |
| POST | `/api/hair-colors` | 新增发色（管理后台） |
| PUT | `/api/hair-colors/{id}` | 更新发色（管理后台） |
| DELETE | `/api/hair-colors/{id}` | 删除发色（软删除） |
| POST | `/api/library/sync` | 终端同步上报 |
| GET | `/api/admin/stats` | 运营统计 |
| GET | `/api/admin/devices` | 终端列表 |

## 增量同步机制

客户端首次请求 `version=0` → 全量返回
客户端后续请求 `version=N` → 只返回版本号 > N 的新增/修改项 + `deleted_ids`

## 当前数据量

- 女发: 102 款（5 个分类）
- 男发: 24 款（3 个分类）
- 发色: 134 种（8 个色系）
