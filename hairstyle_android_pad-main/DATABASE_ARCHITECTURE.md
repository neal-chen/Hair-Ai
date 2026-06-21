# 发型库 & 发色库 系统架构设计（服务端为主 · 终端同步缓存）

> 发型数据和发色数据统一存储在服务端，各终端设备通过 API 同步缓存到本地。

---

## 一、整体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                      管理后台 (Web)                                   │
│  发型/发色 CRUD · 图片上传 · 数据发布                                │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      API 服务端 (Railway)                            │
│                                                                     │
│  ┌─────────────────────┐  ┌─────────────────────────────────────┐  │
│  │   数据库 (PostgreSQL) │  │  文件存储 (对象存储 / 本地)          │  │
│  │                     │  │                                     │  │
│  │  hairstyles 表       │  │  /hairstyles/{id}.png               │  │
│  │  hair_colors 表      │  │  /hair_colors/{id}.png              │  │
│  │  device_sync 表      │  │                                     │  │
│  └─────────────────────┘  └─────────────────────────────────────┘  │
│                                                                     │
│  API:                                                                │
│  GET    /api/hairstyles?gender=&version=    ← 发型库列表 (增量同步)   │
│  GET    /api/hairstyles/{id}/image          ← 发型图片               │
│  GET    /api/hair-colors?version=           ← 发色库列表 (增量同步)   │
│  GET    /api/hair-colors/{id}/image         ← 发色图片               │
│  POST   /api/library/sync                   ← 终端上报同步状态       │
└──────────────────────────┬──────────────────────────────────────────┘
                           │ HTTPS / JSON
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Android App (终端设备)                            │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                   第一启动 / 每次启动                          │  │
│  │                                                              │  │
│  │  ① 检查本地缓存版本号                                          │  │
│  │  ② 调用 GET /api/hairstyles?version=X 获取增量数据              │  │
│  │  ③ 调用 GET /api/hair-colors?version=Y 获取增量数据             │  │
│  │  ④ 更新 Room 本地缓存 + 版本号                                  │  │
│  │  ⑤ 按需下载新/变更的图片到本地文件缓存                            │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                   本地缓存 (Room + 文件)                      │  │
│  │                                                              │  │
│  │  Room 表:                             文件缓存:                │  │
│  │  hairstyles_cache (device_id 无关)     /cache/hairstyles/     │  │
│  │  hair_colors_cache (device_id 无关)    /cache/hair_colors/    │  │
│  │  sync_metadata (版本/时间戳)           Glide 缓存亦可          │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                   终端数据 (device_id 关联)                   │  │
│  │                                                              │  │
│  │  收藏/历史/自定义标签等数据独立存储，按 device_id 隔离           │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 二、数据库设计

### 2.1 服务端 (PostgreSQL)

```sql
-- ==========================================
-- 发型库主表 (服务端唯一权威来源)
-- ==========================================
CREATE TABLE hairstyles (
    id            VARCHAR(36) PRIMARY KEY,       -- UUID
    category      VARCHAR(50)  NOT NULL,          -- 韩式/日式/甜酷风…
    name          VARCHAR(100) NOT NULL,           -- 发型名称
    description   TEXT         NOT NULL DEFAULT '', -- 详细描述
    gender        VARCHAR(4)   NOT NULL,           -- "男" / "女"
    tags          JSONB        NOT NULL DEFAULT '[]',  -- ["复古","卷发"]
    image_url     VARCHAR(500) NOT NULL DEFAULT '', -- 图片访问 URL
    sort_order    INTEGER      NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,  -- 启用/禁用（下架）
    version       BIGINT       NOT NULL DEFAULT 1,     -- 数据版本号（用于增量同步）
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hairstyles_gender ON hairstyles(gender);
CREATE INDEX idx_hairstyles_version ON hairstyles(version);
CREATE INDEX idx_hairstyles_active ON hairstyles(is_active);

-- ==========================================
-- 发色库主表 (服务端唯一权威来源)
-- ==========================================
CREATE TABLE hair_colors (
    id            VARCHAR(36) PRIMARY KEY,
    category      VARCHAR(50)  NOT NULL,          -- 温感色系/流光金系…
    name          VARCHAR(100) NOT NULL,           -- 蜂蜜茶色/巧克力棕…
    procedure     TEXT         NOT NULL DEFAULT '', -- 染发流程 Markdown
    color_hex     VARCHAR(7),                      -- #XXXXXX 色值
    image_url     VARCHAR(500) NOT NULL DEFAULT '',
    sort_order    INTEGER      NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    version       BIGINT       NOT NULL DEFAULT 1,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hair_colors_category ON hair_colors(category);
CREATE INDEX idx_hair_colors_version ON hair_colors(version);

-- ==========================================
-- 终端同步记录 (服务端追踪各终端同步状态)
-- ==========================================
CREATE TABLE device_sync_log (
    id              BIGSERIAL PRIMARY KEY,
    device_id       VARCHAR(64)  NOT NULL,
    device_name     VARCHAR(200),
    last_hairstyle_version  BIGINT NOT NULL DEFAULT 0,  -- 该终端已同步到哪个版本
    last_color_version      BIGINT NOT NULL DEFAULT 0,
    last_sync_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(device_id)
);
```

### 2.2 终端本地缓存 (Room + SQLite)

```kotlin
// ==========================================
// 发型缓存 Entity (终端只读缓存)
// ==========================================
@Entity(
    tableName = "hairstyle_cache",
    indices = [Index(value = ["gender", "category"])]
)
data class HairstyleCacheEntity(
    @PrimaryKey val id: String,
    val category: String,
    val name: String,
    val description: String,
    val gender: String,
    val tags: String = "[]",            // JSON array
    val imageUrl: String = "",           // 远程 URL
    val localImagePath: String = "",     // 本地缓存文件路径
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val version: Long = 0               // 服务端版本号
)

// ==========================================
// 发色缓存 Entity
// ==========================================
@Entity(
    tableName = "hair_color_cache",
    indices = [Index(value = ["category"])]
)
data class HairColorCacheEntity(
    @PrimaryKey val id: String,
    val category: String,
    val name: String,
    val procedure: String = "",
    val colorHex: String? = null,
    val imageUrl: String = "",
    val localImagePath: String = "",
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val version: Long = 0
)

// ==========================================
// 同步元数据 (记录本地缓存的版本状态)
// ==========================================
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String,         // "hairstyle_version" / "color_version" / "last_sync_at"
    val value: String
)
```

### 2.3 DAO

```kotlin
// 发型缓存 DAO
@Dao
interface HairstyleCacheDao {
    @Query("SELECT * FROM hairstyle_cache WHERE gender = :gender AND isActive = 1 ORDER BY sortOrder ASC")
    fun getByGender(gender: String): Flow<List<HairstyleCacheEntity>>

    @Query("SELECT * FROM hairstyle_cache WHERE gender = :gender AND category = :category AND isActive = 1 ORDER BY sortOrder ASC")
    fun getByCategory(gender: String, category: String): Flow<List<HairstyleCacheEntity>>

    @Query("SELECT * FROM hairstyle_cache WHERE isActive = 1 AND (name LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%')")
    fun search(q: String): Flow<List<HairstyleCacheEntity>>

    @Query("SELECT DISTINCT category FROM hairstyle_cache WHERE gender = :gender AND isActive = 1 ORDER BY category")
    fun getCategories(gender: String): Flow<List<String>>

    @Query("SELECT MAX(version) FROM hairstyle_cache")
    suspend fun getMaxVersion(): Long?       // 当前最高版本号

    @Upsert
    suspend fun upsertAll(items: List<HairstyleCacheEntity>)  // 批量写入缓存

    @Query("DELETE FROM hairstyle_cache WHERE id = :id")
    suspend fun deleteById(id: String)       // 删除下架的款式

    @Query("DELETE FROM hairstyle_cache WHERE id NOT IN (:activeIds)")
    suspend fun deleteNotIn(activeIds: List<String>)  // 全量替换
}

// 发色缓存 DAO（类似）
@Dao
interface HairColorCacheDao {
    @Query("SELECT * FROM hair_color_cache WHERE isActive = 1 ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<HairColorCacheEntity>>

    @Query("SELECT * FROM hair_color_cache WHERE category = :category AND isActive = 1 ORDER BY sortOrder ASC")
    fun getByCategory(category: String): Flow<List<HairColorCacheEntity>>

    @Query("SELECT DISTINCT category FROM hair_color_cache WHERE isActive = 1 ORDER BY category")
    fun getCategories(): Flow<List<String>>

    @Query("SELECT MAX(version) FROM hair_color_cache")
    suspend fun getMaxVersion(): Long?

    @Upsert
    suspend fun upsertAll(items: List<HairColorCacheEntity>)

    @Query("DELETE FROM hair_color_cache WHERE id = :id")
    suspend fun deleteById(id: String)
}

// 同步元数据 DAO
@Dao
interface SyncMetadataDao {
    @Query("SELECT value FROM sync_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Query("SELECT value FROM sync_metadata WHERE `key` = :key LIMIT 1")
    fun getValueFlow(key: String): Flow<String?>

    @Upsert
    suspend fun set(metadata: SyncMetadataEntity)
}
```

---

## 三、API 接口设计

### 3.1 发型库 API

```
GET /api/hairstyles
```

**参数：**

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| gender | string | 否 | 全部 | 筛选性别：`"男"` / `"女"` |
| category | string | 否 | 全部 | 筛选分类 |
| version | long | 否 | 0 | 客户端当前版本号，服务端返回 > version 的数据 |
| device_id | string | 是 | — | 终端设备标识 |

**响应：**

```json
{
  "success": true,
  "data": {
    "hairstyles": [
      {
        "id": "uuid-xxx",
        "category": "韩式",
        "name": "女神大波浪",
        "description": "及腰长发，S型大卷...",
        "gender": "女",
        "tags": ["卷发", "长发", "女神"],
        "image_url": "https://.../hairstyles/uuid-xxx.png",
        "sort_order": 1,
        "is_active": true,
        "version": 5
      }
    ],
    "deleted_ids": ["id1", "id2"],       // 客户端需删除的 ID
    "current_version": 5,                 // 服务端当前总版本
    "server_time": "2026-06-17T10:00:00Z"
  }
}
```

**增量同步逻辑：**

```
客户端 version=0  → 返回全部数据（全量）
客户端 version=3  → 返回 version > 3 的数据 + deleted_ids
客户端 version=5  → 返回空（已最新）
```

### 3.2 发色库 API

```
GET /api/hair-colors
```

参数与响应结构同发型库，无 gender 字段。

### 3.3 图片 API

```
GET /api/hairstyles/{id}/image
GET /api/hair-colors/{id}/image
```

直接返回图片二进制（Content-Type: image/png），支持缓存。

### 3.4 终端同步上报

```
POST /api/library/sync
```

**请求体：**

```json
{
  "device_id": "abc123",
  "device_name": "Samsung Pad-01",
  "hairstyle_version": 5,
  "color_version": 3
}
```

**用途：** 服务端记录每个终端的最新同步版本，用于运营统计和问题排查。

---

## 四、终端同步流程

### 4.1 首次启动（全量同步）

```
App 安装 → 首次打开
    │
    ▼
┌─────────────────────────────────┐
│  1. 检查本地缓存    ──→ 空       │
│     sync_metadata               │
│     hairstyle_version = 0       │
│     color_version = 0           │
└──────────┬──────────────────────┘
           ▼
┌─────────────────────────────────┐
│  2. 请求发型库                   │
│     GET /api/hairstyles          │
│     ?version=0&device_id=xxx     │
│     ──→ 返回全部 126 条          │
└──────────┬──────────────────────┘
           ▼
┌─────────────────────────────────┐
│  3. 写入 Room 缓存              │
│     hairstyle_cache.upsertAll() │
│     更新 version → 5            │
└──────────┬──────────────────────┘
           ▼
┌─────────────────────────────────┐
│  4. 请求发色库                   │
│     GET /api/hair-colors         │
│     ?version=0&device_id=xxx     │
│     ──→ 返回全部发色数据          │
└──────────┬──────────────────────┘
           ▼
┌─────────────────────────────────┐
│  5. 写入 Room 缓存              │
│     hair_color_cache.upsertAll()│
│     更新 version → 3            │
└──────────┬──────────────────────┘
           ▼
┌─────────────────────────────────┐
│  6. 后台静默下载图片到文件缓存    │
│     Glide 或 OkHttp 逐一下载     │
│     存入 /data/.../cache/       │
│     完成后更新 localImagePath    │
└──────────┬──────────────────────┘
           ▼
┌─────────────────────────────────┐
│  7. 上报同步完成                 │
│     POST /api/library/sync      │
└─────────────────────────────────┘
```

### 4.2 增量同步（后续启动）

```
App 启动（非首次）
    │
    ▼
┌─────────────────────────────────┐
│  1. 读取本地版本号               │
│     hairstyle_version = 5       │
│     color_version = 3           │
└──────────┬──────────────────────┘
           ▼
┌─────────────────────────────────┐
│  2. 增量请求                     │
│     GET /api/hairstyles          │
│     ?version=5&device_id=xxx     │
│                                  │
│  可能的结果：                     │
│  ┌── version=5 → 空（已最新）     │
│  ├── version=5 → 返回 2 条新增    │
│  │    + ["deleted_id_1"]         │
│  └── version=5 → 返回 1 条更新    │
└──────────┬──────────────────────┘
           ▼
┌─────────────────────────────────┐
│  3. 增量更新缓存                 │
│     upsertAll(新增/变更)         │
│     deleteById(已删除)           │
│     更新 version                 │
└──────────┬──────────────────────┘
           ▼
┌─────────────────────────────────┐
│  4. 按需下载新图片               │
│     只下载 image_url 变更的       │
└─────────────────────────────────┘
```

### 4.3 图片缓存策略

```
┌─────────────────────────────────────┐
│          图片加载流程                 │
│                                     │
│  请求 HairstyleEntity.imageUrl      │
│                                     │
│  ┌── localImagePath 存在?           │
│  │  ├── 是 → Glide.load(localFile)  │
│  │  └── 否 → Glide.load(imageUrl)   │
│  │           并缓存到本地文件         │
│  └───────────────────────────────   │
│                                     │
│  Glide 自身也有 LRU 内存缓存 +       │
│  磁盘缓存，作为第二层保护              │
└─────────────────────────────────────┘

缓存文件位置:
  context.cacheDir/hairstyle_images/{id}.png
  context.cacheDir/hair_color_images/{id}.png

优点:
  - 离线可用
  - 无网络时也不影响展示
  - 服务端图片更换后，本地文件自动失效
```

---

## 五、代码结构

### 5.1 新增文件清单

```
data/
├── api/
│   ├── HairstyleApiService.kt        ← 新增端点
│   └── LibraryApiService.kt          ← 新增：发型/发色专用 API
│
├── db/
│   ├── AppDatabase.kt                ← Room 数据库
│   ├── entity/
│   │   ├── HairstyleCacheEntity.kt
│   │   ├── HairColorCacheEntity.kt
│   │   └── SyncMetadataEntity.kt
│   ├── dao/
│   │   ├── HairstyleCacheDao.kt
│   │   ├── HairColorCacheDao.kt
│   │   └── SyncMetadataDao.kt
│   └── converter/
│       └── CacheConverters.kt         ← Entity ↔ Model 转换
│
├── repository/
│   ├── HairstyleSyncRepository.kt    ← 发型同步 + 缓存管理
│   └── HairColorSyncRepository.kt   ← 发色同步 + 缓存管理
│
├── sync/
│   ├── LibrarySyncManager.kt         ← 同步协调器 (触发全量/增量)
│   └── ImageCacheManager.kt          ← 图片文件缓存管理
│
└── model/
    ├── HairstyleItem.kt              ← 发型数据模型 (DSL 层)
    └── HairColorItem.kt              ← 发色数据模型
```

### 5.2 关键类职责

```kotlin
// ==========================================
// 同步协调器 — 触发同步的总入口
// ==========================================
class LibrarySyncManager(
    private val context: Context,
    private val hairstyleRepo: HairstyleSyncRepository,
    private val colorRepo: HairColorSyncRepository,
    private val imageCache: ImageCacheManager
) {
    /**
     * 在 App 启动时调用
     * 自动判断全量/增量，并触发后台同步
     */
    suspend fun syncOnLaunch(): SyncResult {
        // 1. 检查网络
        if (!isNetworkAvailable()) return SyncResult.NoNetwork

        // 2. 并行同步发型 + 发色
        val hairstyleResult = hairstyleRepo.sync()
        val colorResult = colorRepo.sync()

        // 3. 静默下载新图片
        if (hairstyleResult.hasChanges || colorResult.hasChanges) {
            imageCache.downloadNewImagesInBackground()
        }

        return SyncResult.Success(
            hairstyleCount = hairstyleResult.count,
            colorCount = colorResult.count
        )
    }
}

// ==========================================
// 发型同步 Repository
// ==========================================
class HairstyleSyncRepository(
    private val api: LibraryApiService,
    private val cacheDao: HairstyleCacheDao,
    private val syncMetaDao: SyncMetadataDao
) {
    /**
     * 执行同步（自动判断全量/增量）
     */
    suspend fun sync(): SyncResult {
        val localVersion = syncMetaDao.getValue("hairstyle_version")?.toLongOrNull() ?: 0L

        // 请求增量数据
        val response = api.getHairstyles(
            version = localVersion,
            deviceId = getDeviceId()
        )

        if (!response.isSuccessful || response.body() == null) {
            return SyncResult.Error("API error")
        }

        val data = response.body()!!.data

        // 写入缓存
        if (data.hairstyles.isNotEmpty()) {
            cacheDao.upsertAll(data.hairstyles.map { it.toEntity() })
        }

        // 删除已下架的
        if (data.deleted_ids.isNotEmpty()) {
            data.deleted_ids.forEach { id -> cacheDao.deleteById(id) }
        }

        // 更新版本号
        syncMetaDao.set(SyncMetadataEntity("hairstyle_version", data.current_version.toString()))
        syncMetaDao.set(SyncMetadataEntity("last_sync_at", System.currentTimeMillis().toString()))

        return SyncResult.Success(
            count = data.hairstyles.size,
            deletedCount = data.deleted_ids.size,
            hasChanges = data.hairstyles.isNotEmpty() || data.deleted_ids.isNotEmpty()
        )
    }
}

// ==========================================
// 图片缓存管理器
// ==========================================
class ImageCacheManager(private val context: Context) {
    private val hairstyleCacheDir = File(context.cacheDir, "hairstyle_images")
    private val colorCacheDir = File(context.cacheDir, "hair_color_images")

    /**
     * 下载图片到本地缓存
     */
    suspend fun downloadImage(imageUrl: String, targetFile: File): Boolean {
        if (targetFile.exists()) return true  // 已缓存

        return withContext(Dispatchers.IO) {
            try {
                val response = OkHttpClient().newCall(Request.Builder()
                    .url(imageUrl)
                    .build()).execute()

                response.body?.byteStream()?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * 批量下载新发型图片
     */
    suspend fun downloadNewHairstyleImages(hairstyles: List<HairstyleCacheEntity>) {
        hairstyleCacheDir.mkdirs()
        hairstyles
            .filter { it.imageUrl.isNotBlank() }
            .forEach { hairstyle ->
                val targetFile = File(hairstyleCacheDir, "${hairstyle.id}.png")
                if (downloadImage(hairstyle.imageUrl, targetFile)) {
                    // 更新缓存中的 localImagePath
                    hairstyleDao.updateLocalPath(hairstyle.id, targetFile.absolutePath)
                }
            }
    }
}
```

---

## 六、API 接口文档

### 6.1 发型库列表

```
GET /api/hairstyles
```

**请求示例：**

```
GET /api/hairstyles?gender=女&version=0&device_id=pad-001
```

**响应示例（全量）：**

```json
{
  "success": true,
  "data": {
    "hairstyles": [
      {
        "id": "f-hair-001",
        "category": "韩式",
        "name": "云朵烫",
        "description": "像云朵一样柔软蓬松的卷发...",
        "gender": "女",
        "tags": ["卷发", "蓬松", "韩式"],
        "image_url": "https://web-production-bingli.up.railway.app/api/hairstyles/f-hair-001/image",
        "sort_order": 1,
        "is_active": true,
        "version": 1
      }
    ],
    "deleted_ids": [],
    "current_version": 5,
    "server_time": "2026-06-17T10:00:00Z"
  }
}
```

### 6.2 发色库列表

```
GET /api/hair-colors
```

**请求示例：**

```
GET /api/hair-colors?version=0&device_id=pad-001
```

**响应示例：**

```json
{
  "success": true,
  "data": {
    "colors": [
      {
        "id": "c-color-001",
        "category": "温感色系",
        "name": "蜂蜜茶色",
        "procedure": "# 染发流程\n\n...",
        "color_hex": "#C4925A",
        "image_url": "https://.../api/hair-colors/c-color-001/image",
        "sort_order": 1,
        "is_active": true,
        "version": 1
      }
    ],
    "deleted_ids": [],
    "current_version": 3,
    "server_time": "2026-06-17T10:00:00Z"
  }
}
```

### 6.3 图片资源

```
GET /api/hairstyles/{id}/image
GET /api/hair-colors/{id}/image
```

直接返回图片二进制数据，`Content-Type: image/png`。支持 `ETag` / `If-None-Match` 缓存协商。

### 6.4 同步状态上报

```
POST /api/library/sync
Content-Type: application/json

{
  "device_id": "pad-001",
  "device_name": "Samsung Galaxy Tab S8",
  "hairstyle_version": 5,
  "color_version": 3
}
```

**响应：**

```json
{
  "success": true,
  "message": "sync recorded"
}
```

---

## 七、管理后台（Web）

服务端需配套一个简单的发型/发色管理后台，建议功能：

| 功能 | 说明 |
|---|---|
| **发型管理** | 增删改查发型数据 + 上传发型图片 |
| **发色管理** | 增删改查发色数据 + 上传发色色样 + 填写染发流程 |
| **分类管理** | 自定义发型分类（韩式/日式/甜酷风…）和发色色系 |
| **批量操作** | 批量导入/导出（JSON/CSV） |
| **终端监控** | 查看各设备同步状态、版本号 |

---

## 八、与现有系统的关系

```
现有 API (已存在)                       新增 API (本方案)
─────────────────                     ─────────────────
POST /api/create-session               GET  /api/hairstyles
POST /api/upload/{id}/{type}           GET  /api/hair-colors
POST /api/process/{id}                 GET  /api/hairstyles/{id}/image
POST /api/process-color/{id}           GET  /api/hair-colors/{id}/image
GET  /api/session/{id}                 POST /api/library/sync
POST /api/device/activate              
POST /api/device/check-subscription    

现有 API 保持不动，新增 API 独立部署。
```

---

## 九、实施步骤

| 步骤 | 内容 | 工作量 |
|---|---|---|
| **1. 服务端建表** | 在 Railway 项目中创建 hairstyles / hair_colors / device_sync_log 表 | 小 |
| **2. 服务端 API** | 实现上述 4 个 API 端点 + 图片存储 | 中 |
| **3. 管理后台** | 简单的 CRUD 页面 + 图片上传 | 中 |
| **4. 导入数据** | 将当前 126 款发型 + 150 种发色迁移到服务端数据库 | 小 |
| **5. Android Room** | 新增 Entity/DAO/Database | 中 |
| **6. Android 同步层** | LibrarySyncManager + ImageCacheManager | 中 |
| **7. 接入 UI** | Activity/Adapter 从 Flow 读取 Room 缓存数据 | 中 |
| **8. 移除旧代码** | 删除 HairstyleLibraryManager 等硬编码 | 小 |
