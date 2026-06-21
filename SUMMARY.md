# AI 换发型 / 换发色 系统 — 项目总结

> 美容美发智能镜配套系统，提供 AI 换发型、换发色、3D 视频生成功能，支持发型/发色库的云端管理与终端增量同步。

---

## 一、项目概览

```
├─ server/        API 服务端        FastAPI + SQLite    20+ 端点
├─ processor/     AI 处理引擎        Python 模块化       9 个模块
├─ faxin/         WebView 前端 SPA   HTML + JS          8 页面 Hash 路由
├─ hairstyle_     Android App        Kotlin             32 个 .kt 文件
│  android_pad-main/
└─ docker-        Docker 部署        容器化              一键启动
   compose.yml
```

**数据量：** 女发 103 款 · 男发 24 款 · 发色 134 种（含详细染发流程）

---

## 二、系统架构

```
┌─────────────────────────────────────────────────────┐
│  Android App (Kotlin)                                │
│  ┌───────────────────────────────────────────────┐  │
│  │  WebView → faxin/ SPA                          │  │
│  │  AndroidBridge (JsInterface)                   │  │
│  │  10 个 Activity · Camera · QR · Gallery        │  │
│  └──────────┬────────────────────────────────────┘  │
└─────────────┼────────────────────────────────────────┘
              │ JSON / HTTPS
┌─────────────▼────────────────────────────────────────┐
│  API 服务端 (FastAPI)                                 │
│  ┌────────────────────┐  ┌─────────────────────────┐ │
│  │  发型/发色库 API     │  │  管理后台 (SPA)           │ │
│  │  GET/POST/PUT/DEL  │  │  概览 · CRUD · 图片上传   │ │
│  │  增量同步 + 搜索    │  │  统计 · 终端列表           │ │
│  │  Bearer Token 认证  │  │  登录保护                 │ │
│  └────────┬───────────┘  └─────────────────────────┘ │
│           ▼                                          │
│  ┌────────────────────────────────────────────────┐  │
│  │  SQLite · SQLAlchemy · 版本化增量同步           │  │
│  └────────────────────────────────────────────────┘  │
└─────────────┬────────────────────────────────────────┘
              │ API
┌─────────────▼────────────────────────────────────────┐
│  AI 处理引擎 (processor/)                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────────┐ │
│  │RunningHub│ │ 火山引擎  │ │  PixVerse / Pai AI   │ │
│  │换发型/发色│ │ 3D 视频   │ │  图生视频             │ │
│  └──────────┘ └──────────┘ └──────────────────────┘ │
│  ┌────────────────────────────────────────────────┐  │
│  │ Gemini 预处理 · 缓存管理 · Word 报告生成        │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

---

## 三、API 端点清单

| 方法 | 端点 | 权限 | 说明 |
|---|---|---|---|
| `GET` | `/api/health` | 🔓 公开 | 健康检查 |
| `GET` | `/api/hairstyles` | 🔓 公开 | 发型列表（增量同步 + 搜索） |
| `GET` | `/api/hairstyles/{id}` | 🔓 公开 | 发型详情 |
| `GET` | `/api/hairstyles/{id}/image` | 🔓 公开 | 发型图片 |
| `POST` | `/api/hairstyles` | 🔒 管理 | 新增发型 |
| `PUT` | `/api/hairstyles/{id}` | 🔒 管理 | 更新发型 |
| `DELETE` | `/api/hairstyles/{id}` | 🔒 管理 | 软删除 |
| `POST` | `/api/hairstyles/{id}/image` | 🔒 管理 | 上传发型图 |
| `GET` | `/api/hair-colors` | 🔓 公开 | 发色列表（增量同步 + 搜索） |
| `GET` | `/api/hair-colors/{id}` | 🔓 公开 | 发色详情（含染发流程） |
| `GET` | `/api/hair-colors/{id}/image` | 🔓 公开 | 发色图片 |
| `POST` | `/api/hair-colors` | 🔒 管理 | 新增发色 |
| `PUT` | `/api/hair-colors/{id}` | 🔒 管理 | 更新发色 |
| `DELETE` | `/api/hair-colors/{id}` | 🔒 管理 | 软删除 |
| `POST` | `/api/hair-colors/{id}/image` | 🔒 管理 | 上传发色图 |
| `POST` | `/api/library/sync` | 🔓 公开 | 终端同步上报 |
| `GET` | `/api/admin/stats` | 🔒 管理 | 运营统计 |
| `GET` | `/api/admin/devices` | 🔒 管理 | 终端列表 |
| `POST` | `/api/admin/login` | 🔓 公开 | 管理后台登录 |

---

## 四、技术栈

| 层级 | 技术 | 版本 |
|---|---|---|
| **服务端框架** | FastAPI + Uvicorn | 0.138+ |
| **数据库** | SQLite + SQLAlchemy 2.0 | — |
| **认证** | Bearer Token（环境变量配置） | — |
| **AI 处理** | RunningHub / 火山引擎(豆包) / PixVerse | — |
| **图像预处理** | Gemini 2.5 Flash (via OpenRouter) | — |
| **Android** | Kotlin + ViewBinding + Retrofit | targetSdk 34 |
| **前端** | Vanilla JS SPA (Hash 路由) | — |
| **部署** | Docker / Railway | — |

---

## 五、已实现功能

### 🖥 服务端
- [x] 发型/发色库完整 CRUD（增删改查 + 软删除）
- [x] 增量同步（version 机制，终端只获取变更）
- [x] 文本搜索（?q= 参数，覆盖名称/描述/分类/标签）
- [x] 图片上传与本地存储
- [x] 终端同步状态追踪
- [x] 管理统计看板
- [x] Bearer Token 认证（保护管理端操作）
- [x] 全局统一异常处理
- [x] 完整的 OpenAPI 文档 (/docs)

### 🤖 AI 引擎
- [x] RunningHub 换发型/换发色/3D 视频
- [x] 火山引擎 3D 图生视频
- [x] PixVerse 图生视频
- [x] Gemini 图像预处理（自动构图/光线校正）
- [x] Gemini 处理缓存（基于文件哈希）
- [x] 批量处理流水线（用户图 × 发型图 全组合）
- [x] Word 文档结果报告
- [x] 2473 行 → 9 模块重构

### 📱 Android App
- [x] WebView 核心交互（AndroidBridge 桥接）
- [x] 拍照 + 相册选择
- [x] AI 换发型 / 换发色 / 3D 视频
- [x] 发型库 / 发色库浏览
- [x] 二维码扫描与上传
- [x] 历史记录
- [x] 3 种图片加载模式 (ASSETS/REMOTE/LOCAL)

### 🌐 前端
- [x] SPA Hash 路由（8 页面）
- [x] 色盘组件 + 轮播组件
- [x] 桌面/平板自适应缩放
- [x] 管理后台（概览 + CRUD + 搜索 + 图片上传 + 终端列表）

### 🛠 运维
- [x] Git 版本管理（12 次提交）
- [x] 一键启动脚本 (start.sh)
- [x] 环境变量模板 (.env.example)
- [x] 24 个 API 回归测试 (test_api.sh)
- [x] Docker 部署 (docker-compose.yml)
- [x] GitHub 远程仓库
- [x] Claude 项目记忆

---

## 六、快速启动

```bash
# 方式一：Docker
docker compose up -d
open http://localhost:8000/admin

# 方式二：本地
cp .env.example .env
./start.sh

# 运行测试
./server/test_api.sh
```

管理后台默认 Key：`hair-admin-dev-2026`

---

## 七、路线图展望

### 短期（可继续推进）

| 项目 | 说明 |
|---|---|
| **PostgreSQL 迁移** | 生产环境替换 SQLite，增加连接池 |
| **对象存储** | 图片从本地文件系统迁移到 S3/OSS |
| **Web 管理后台增强** | 批量导入/导出、分类管理、数据统计图表 |
| **Android 发型 Assets** | 补充 `assets/hairstyles_woman/` 和 `assets/hairstyles_man/` |

### 中期

| 项目 | 说明 |
|---|---|
| **API 限流** | 终端同步接口增加速率限制 |
| **CI/CD** | GitHub Actions → 自动测试 + 部署到 Railway |
| **多语言** | Android 和前端增加国际化支持 |
| **前端统一** | 将 `assets/web/` 多页版迁移到 `faxin/` SPA 版 |

### 长期

| 项目 | 说明 |
|---|---|
| **在线试发** | 基于 WebGL/WebAssembly 的实时发型预览 |
| **推荐系统** | 根据用户脸型/肤色推荐发型和发色 |
| **美发师端** | 美发师管理端，可上传案例和管理预约 |

---

## 八、提交历史

```
6c9af13 feat: add Docker support for easy deployment
9c87a0c feat: add API regression test suite and soft-delete fix
96be373 feat: add global exception handler for unified error format
99e7fc2 feat: add text search to hairstyle and color APIs
833c62f refactor: clean up imports and startup handler in server/main.py
a816769 fix: exclude admin redirects from OpenAPI schema, add root redirect
b9bf78b chore: add quick-start setup scripts and env template
aa14b5a feat: add API authentication with Bearer token
9ebf2bf feat: add web admin panel for hairstyle/color library
42a1dd8 chore: sync frontend copies and add sync script
45d9822 refactor: split AI processor into modular package
b4b8f2d feat: initial project setup — AI hairstyle/color smart mirror system
```

---

## 九、相关链接

| 资源 | 地址 |
|---|---|
| GitHub 仓库 | https://github.com/neal-chen/Hair-Ai |
| 管理后台 | http://localhost:8000/admin |
| API 文档 | http://localhost:8000/docs |
| API 回归测试 | `./server/test_api.sh` |
| 环境变量模板 | `.env.example` |
