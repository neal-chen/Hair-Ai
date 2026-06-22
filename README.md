# 💇 AI 换发型 / 换发色智能镜系统

[![CI](https://github.com/neal-chen/Hair-Ai/actions/workflows/ci.yml/badge.svg)](https://github.com/neal-chen/Hair-Ai/actions/workflows/ci.yml)
[![Python](https://img.shields.io/badge/python-3.14-blue)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.138-green)](https://fastapi.tiangolo.com/)
[![License](https://img.shields.io/badge/license-MIT-lightgrey)](LICENSE)

美容美发智能镜配套系统，提供 **AI 换发型、换发色、3D 视频生成**功能，支持发型/发色库的云端管理与终端增量同步。

---

## 快速开始

```bash
# 方式一：Docker（推荐）
docker compose up -d
open http://localhost:8000/admin

# 方式二：本地
cp .env.example .env
./start.sh

# 运行测试
./server/test_api.sh          # 24 个回归测试
cd server && python -m pytest # 50 个单元测试
```

管理后台默认 Key：`hair-admin-dev-2026`

---

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│  Android App (Kotlin)                                   │
│  ┌─────────────────────────────────────────────────┐   │
│  │  WebView → faxin/ SPA · AndroidBridge · Camera  │   │
│  │  10 Activities · QR · Gallery · History         │   │
│  └────────────┬────────────────────────────────────┘   │
└───────────────┼────────────────────────────────────────┘
                │ JSON / HTTPS
┌───────────────▼────────────────────────────────────────┐
│  API 服务端 (FastAPI)                                    │
│  ┌─────────────────────┐  ┌──────────────────────────┐ │
│  │  发型/发色库 API      │  │  管理后台 (SPA)           │ │
│  │  CRUD · 增量同步      │  │  概览 · CRUD · 图片上传   │ │
│  │  搜索 · 认证 · 统计    │  │  统计 · 终端列表          │ │
│  └──────────┬──────────┘  └──────────────────────────┘ │
│             ▼                                           │
│  SQLite · SQLAlchemy · 版本化增量同步 · 日志 · 连接池    │
└───────────────┬────────────────────────────────────────┘
                │ RunningHub / 火山引擎 / PixVerse API
┌───────────────▼────────────────────────────────────────┐
│  AI 处理引擎 (processor/)                               │
│  换发型 · 换发色 · 3D 视频 · Gemini 预处理 · Word 报告   │
└────────────────────────────────────────────────────────┘
```

## 核心能力

### 📋 发型/发色库管理

| 功能 | 说明 |
|---|---|
| **发型库** | 女发 103 款 · 男发 24 款 · 5+3 分类 |
| **发色库** | 134 种 · 8 个色系 · 含详细染发流程 Markdown |
| **增量同步** | 终端携带 version 号获取增量变更 |
| **文本搜索** | 按名称/描述/分类/标签 模糊搜索 |
| **CRUD** | 增删改查 + 图片上传 + 软删除 |

### 🤖 AI 处理

- **RunningHub** — 换发型、换发色、3D 视频生成
- **火山引擎(豆包)** — 360° 发型展示视频
- **PixVerse(拍我AI)** — 图生视频
- **Gemini 预处理** — 自动构图/光线校正

### 📱 Android App

- 拍照 / 相册选取 · AI 换发/换色 · 3D 视频
- 发型/发色库浏览 · 二维码扫描 · 历史记录
- 3 种图片加载模式 (Assets/Remote/Local)

### 🌐 管理后台

- 📊 概览看板 — 分类统计 · 数据总览
- 👩/👨 发型管理 — 搜索 · 筛选 · CRUD · 图片上传
- 🎨 发色管理 — 同上
- 📱 终端列表 — 设备同步状态追踪

---

## API 一览

| 方法 | 端点 | 权限 | 说明 |
|---|---|---|---|
| `GET` | `/api/hairstyles` | 🔓 | 发型列表（增量同步 + 搜索） |
| `GET` | `/api/hairstyles/{id}` | 🔓 | 发型详情 |
| `POST` | `/api/hairstyles` | 🔒 | 新增发型 |
| `PUT` | `/api/hairstyles/{id}` | 🔒 | 更新发型 |
| `DELETE` | `/api/hairstyles/{id}` | 🔒 | 软删除 |
| `GET` | `/api/hair-colors` | 🔓 | 发色列表（增量同步 + 搜索） |
| `GET` | `/api/hair-colors/{id}` | 🔓 | 发色详情（含染发流程） |
| `POST` | `/api/library/sync` | 🔓 | 终端同步上报 |
| `GET` | `/api/admin/stats` | 🔒 | 运营统计 |
| `GET` | `/api/admin/devices` | 🔒 | 终端列表 |

完整文档：http://localhost:8000/docs

---

## 技术栈

| 层级 | 技术 |
|---|---|
| **服务端** | Python 3.14, FastAPI, SQLAlchemy 2.0, SQLite |
| **AI 引擎** | RunningHub, 火山引擎(豆包), PixVerse, Gemini 2.5 |
| **Android** | Kotlin, ViewBinding, Retrofit, OkHttp, Gson, ZXing |
| **前端** | Vanilla JS SPA, Hash Router, CSS3 |
| **部署** | Docker, Railway, 一键启动脚本 |
| **测试** | pytest (50), bash (24), GitHub Actions CI |

---

## 项目结构

```
Hair/
├── server/              API 服务端 (11 文件)
│   ├── main.py          20+ 端点 · 认证 · 搜索 · 异常处理
│   ├── auth.py          Bearer Token 认证
│   ├── models.py        数据模型 + JSON 序列化
│   ├── database.py      SQLAlchemy 连接池配置
│   ├── admin/           管理后台 SPA
│   ├── tests/           50 个 pytest 测试
│   └── Dockerfile       容器镜像
├── processor/           AI 处理引擎 (9 模块)
├── faxin/               WebView 前端 (SPA 开发版)
├── hairstyle_android_   Android App (Kotlin, 29 源文件)
│   pad-main/
├── docker-compose.yml   Docker 编排
├── start.sh             一键启动脚本
└── .env.example         环境变量模板
```

---

## 数据

- 女发: **103** 款 (韩式 · 日式 · 欧美 · 甜酷风 · 氛围感)
- 男发: **24** 款 (短发 · 中等长度 · 长发)
- 发色: **134** 种 (温感 · 清冷 · 流光金 · 热情红 · 薄暮紫 · 静谧蓝 · 中性灰 · 元气橙)
- 终端: 按需注册，增量同步

---

## 开发

```bash
# 安装依赖
cd server && pip install -r requirements-dev.txt

# 运行测试
python -m pytest tests/ -v          # 50 个单元/集成测试
ADMIN_API_KEY=xxx bash test_api.sh  # 24 个端到端测试

# 启动开发服务
python -m uvicorn main:app --reload --port 8000
```

---

## 部署

```bash
# Docker
docker compose up -d

# Railway
git push railway main

# 手动
cd server && python -m uvicorn main:app --host 0.0.0.0 --port 8000
```

---

## 相关资源

| 资源 | 地址 |
|---|---|
| GitHub 仓库 | https://github.com/neal-chen/Hair-Ai |
| 管理后台 | http://localhost:8000/admin |
| API 文档 | http://localhost:8000/docs |
| 项目总结 | [SUMMARY.md](./SUMMARY.md) |
