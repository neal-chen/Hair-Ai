# AI 换发型 / 换发色智能镜系统

美容美发智能镜配套系统，提供 AI 换发型、换发色、3D 视频生成功能，支持发型/发色库的云端管理与终端增量同步。

---

## 项目结构

```
Hair/
├── server/                              # API 服务端 (FastAPI + SQLite)
│   ├── main.py                          # FastAPI 应用 (20+ 端点)
│   ├── database.py                      # SQLAlchemy 连接管理
│   ├── models.py                        # 数据模型 (Hairstyle, HairColor, DeviceSyncLog)
│   ├── schemas.py                       # Pydantic 请求/响应模型
│   ├── seed.py                          # 数据库初始化 + 种子数据导入
│   ├── requirements.txt                 # Python 依赖
│   ├── seed_data/                       # 种子数据 (JSON)
│   │   ├── hairstyles_female.json       # 102 款女发
│   │   ├── hairstyles_male.json         # 24 款男发
│   │   └── hair_colors.json             # 134 种发色 (含详细染发流程)
│   └── images/                          # 图片存储 (gitignored)
│
├── hairstyle_processor_v2.py            # AI 处理引擎 (2473 行)
│                                        # RunningHub + 火山引擎 + PixVerse
│
├── hairstyle_android_pad-main/          # Android App (Kotlin)
│   ├── app/
│   │   ├── build.gradle                 # Android 构建配置
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/com/hairstyle/generator/
│   │       │   ├── bridge/              # AndroidBridge (JsInterface)
│   │       │   ├── config/              # 图片配置 (Assets/Remote/Local)
│   │       │   ├── data/
│   │       │   │   ├── api/             # Retrofit API 定义
│   │       │   │   ├── model/           # 数据模型
│   │       │   │   ├── repository/      # Repository 层
│   │       │   │   └── storage/         # 本地存储管理
│   │       │   ├── ui/                  # 10 个 Activity
│   │       │   │   ├── adapter/         # 5 个 Adapter
│   │       │   │   └── widget/          # 自定义控件
│   │       │   └── utils/               # 工具类
│   │       ├── assets/
│   │       │   ├── hair_colors/         # 发色样例图 (~200 张)
│   │       │   └── web/                 # WebView 前端完整源码
│   │       ├── res/                     # Android 资源
│   │       └── res/layout/             # 15 个布局文件
│   ├── build.gradle                     # 项目级构建配置
│   ├── settings.gradle
│   ├── gradle.properties
│   └── gradlew
│
├── faxin/                               # WebView 前端 (独立副本)
│   ├── index.html                       # SPA 入口 (Hash 路由)
│   ├── css/style.css                    # 样式 (2160×3840 设计)
│   ├── js/
│   │   ├── state.js                     # 全局状态
│   │   ├── router.js                    # 路由
│   │   ├── pages.js                     # 8 个页面渲染
│   │   ├── carousel.js                  # 轮播组件
│   │   └── colorwheel.js               # 色盘组件
│   └── images/                          # 前端图片资源
│
├── _archive/                            # 历史归档
├── .gitignore
└── CLAUDE.md                            # 本文件
```

---

## 核心架构

### 数据流

```
终端用户 → [Android App WebView] → AndroidBridge (JsInterface)
                                              ↓
                                     Retrofit API 调用
                                              ↓
                              ┌─────────────────────────┐
                              │  Railway API 服务端       │
                              │  POST /api/create-session │
                              │  POST /api/upload/{id}    │
                              │  POST /api/process/{id}   │
                              │  GET  /api/session/{id}   │
                              └─────────────┬─────────────┘
                                            ↓
                              AI 处理引擎 (RunningHub + 火山引擎 + PixVerse)
```

### 发型/发色库同步流

```
管理后台 → server/ API → SQLite DB
                              ↓
                   增量同步 API (version-based)
                              ↓
                   Android Room 本地缓存 (设计文档)
```

---

## 关键技术栈

| 层级 | 技术 |
|---|---|
| **服务端** | Python 3.14, FastAPI, SQLAlchemy, SQLite, Uvicorn |
| **AI 引擎** | RunningHub API, 火山引擎 (豆包), PixVerse, OpenAI, Gemini |
| **Android** | Kotlin, ViewBinding, Retrofit, OkHttp, Gson, ZXing |
| **前端** | HTML5, CSS3, JavaScript, jQuery, Swiper |
| **部署** | Railway (web-production-bingli.up.railway.app) |

---

## API 端点

### AI 处理 API (RunningHub 后端)

| 方法 | 端点 | 说明 |
|---|---|---|
| `POST` | `/api/create-session` | 创建处理会话 |
| `POST` | `/api/upload/{session_id}/{image_type}` | 上传图片 (user/hairstyle) |
| `POST` | `/api/process/{session_id}` | 启动换发型 |
| `POST` | `/api/process-color/{session_id}` | 启动换发色 |
| `POST` | `/api/process-3d/{session_id}` | 启动 3D 视频生成 |
| `GET` | `/api/session/{session_id}` | 查询会话状态 |
| `POST` | `/api/cancel-session/{session_id}` | 取消任务 |
| `POST` | `/api/device/activate` | 设备激活 |
| `POST` | `/api/device/check-subscription` | 检查订阅 |

### 发型/发色库 API (FastAPI)

| 方法 | 端点 | 说明 |
|---|---|---|
| `GET` | `/api/health` | 健康检查 |
| `GET` | `/api/hairstyles` | 发型库列表 (增量同步) |
| `GET` | `/api/hairstyles/{id}` | 单个发型详情 |
| `GET` | `/api/hairstyles/{id}/image` | 发型图片 |
| `POST` | `/api/hairstyles` | 新增发型 |
| `PUT` | `/api/hairstyles/{id}` | 更新发型 |
| `DELETE` | `/api/hairstyles/{id}` | 删除发型 (软删除) |
| `GET` | `/api/hair-colors` | 发色库列表 (增量同步) |
| `GET` | `/api/hair-colors/{id}` | 单个发色详情 |
| `GET` | `/api/hair-colors/{id}/image` | 发色图片 |
| `POST` | `/api/hair-colors` | 新增发色 |
| `PUT` | `/api/hair-colors/{id}` | 更新发色 |
| `DELETE` | `/api/hair-colors/{id}` | 删除发色 (软删除) |
| `POST` | `/api/library/sync` | 终端同步上报 |
| `GET` | `/api/admin/stats` | 运营统计 |
| `GET` | `/api/admin/devices` | 终端列表 |

---

## 快速启动

### 一键启动 (推荐)

```bash
cp .env.example .env          # 配置环境变量 (编辑 API Key 等)
./start.sh                    # 自动 venv → 安装依赖 → 初始化 DB → 启动
./start.sh --seed             # 强制重新导入种子数据
./start.sh --port 8080        # 自定义端口
```

启动后访问:
- 管理后台: http://localhost:8000/admin (默认 key: `hair-admin-dev-2026`)
- API 文档: http://localhost:8000/docs

### 服务端 (手动)

```bash
cd server
pip install -r requirements.txt
python3 seed.py              # 首次运行：初始化数据库 + 导入种子数据
python3 -m uvicorn main:app --host 0.0.0.0 --port 8000
```

### AI 处理引擎

```bash
# 需要配置环境变量
export RUNNINGHUB_API_KEY=your_key
export RUNNINGHUB_WEBAPP_ID=your_webapp_id
export RUNNINGHUB_COLOR_WEBAPP_ID=your_color_webapp_id
python3 hairstyle_processor_v2.py
```

### Android App

```bash
cd hairstyle_android_pad-main
./gradlew assembleDebug
```

---

## 环境变量参考

### AI 处理器 (`hairstyle_processor_v2.py`)

| 变量 | 说明 | 默认值 |
|---|---|---|
| `RUNNINGHUB_API_KEY` | RunningHub API 密钥 | — |
| `RUNNINGHUB_WEBAPP_ID` | 换发型 Webapp ID | — |
| `RUNNINGHUB_HAIRSTYLE_USER_NODE_ID` | 用户照片节点 ID | 77 |
| `RUNNINGHUB_HAIRSTYLE_HAIR_NODE_ID` | 发型图节点 ID | 24 |
| `RUNNINGHUB_COLOR_WEBAPP_ID` | 换发色 Webapp ID | — |
| `RUNNINGHUB_COLOR_PRE_WEBAPP_ID` | 发色预处理 Webapp ID | — |
| `RUNNINGHUB_3D_WEBAPP_ID` | 3D 视频 Webapp ID | — |
| `ARK_API_KEY` | 火山引擎 API 密钥 | — |
| `VOLCENGINE_3D_MODEL` | 3D 模型 | doubao-seedance-1-5-pro-251215 |
| `VIDEO_3D_PROVIDER` | 视频生成提供商 (auto/pixverse) | auto |
| `PAI_VIDEO_API_KEY` | PixVerse API 密钥 | — |

### 网络配置 (`NetworkConfig.kt`)

服务端地址可在 `NetworkConfig.kt` 中切换：

- **生产**: `https://web-production-bingli.up.railway.app/`
- **本地**: `http://10.0.2.2:5000/`

---

## Android 应用 Activities

| Activity | 方向 | 说明 |
|---|---|---|
| `WelcomeActivity` | 竖屏 | 启动页 (Launcher) |
| `WebViewActivity` | 竖屏 | 核心交互 (载入 web/index.html) |
| `CameraActivity` | 竖屏 | 拍照 |
| `PhotoUploadActivity` | 横屏 | 照片上传 |
| `HairstyleLibraryActivity` | 横屏 | 发型库选择 |
| `HairColorLibraryActivity` | 横屏 | 发色库选择 |
| `ResultsActivity` | 横屏 | AI 处理结果展示 |
| `QRScanActivity` | 横屏 | 二维码扫描 |
| `QRUploadActivity` | 横屏 | 二维码上传 |
| `HistoryActivity` | 横屏 | 历史记录 |

---

## 图片加载模式

`HairstyleImageConfig` / `HairColorImageConfig` 支持三种模式：

1. **ASSETS** — 从 App Assets 加载 (默认，离线可用)
2. **REMOTE** — 从远程服务器加载
3. **LOCAL** — 从本地存储加载

---

## 增量同步机制

- 客户端首次请求 `version=0` → 全量返回
- 客户端后续请求 `version=N` → 只返回版本号 > N 的新增/修改项 + `deleted_ids`
- 终端上报同步状态到 `POST /api/library/sync`

---

## 开发备忘录

### 前端同步（两个副本）

| 目录 | 角色 | 架构 |
|---|---|---|
| `faxin/` | **开发目录** (源码) | SPA，Hash 路由，6 个 JS 模块，无 jQuery |
| `assets/web/` | **部署目录** (Android) | 多页 HTML，jQuery，39 处 AndroidBridge 调用 |

**同步方式：**
```bash
./sync_frontend.sh           # 同步 CSS/JS/图片 到 assets/web/
./sync_frontend.sh --diff    # 只显示差异
```

> ⚠️ 两个版本架构不同，目前只同步共有资源（CSS、图片、`android-bridge.js`）。
> `faxin/` 是新一代 SPA 版本，`assets/web/` 是当前生产版本。
> 如需在 `faxin/` 中测试 Android 功能，已加入 `android-bridge.js`。

### 待改进项
- [ ] 发型图 Assets 文件尚未导入 (`assets/hairstyles_woman/`, `assets/hairstyles_man/`)
- [ ] AI 处理器单文件 2473 行，建议按职责拆分
- [ ] 数据库目前 SQLite，生产建议迁移 PostgreSQL
- [ ] 图片存储目前本地文件系统，生产建议上对象存储
- [ ] 管理后台 API 无认证鉴权
- [ ] 前端 assets/web/ 与 faxin/ 两个副本需保持同步
