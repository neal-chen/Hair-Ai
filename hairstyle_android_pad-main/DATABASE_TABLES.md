# 数据库结构表

> 服务端 PostgreSQL + 终端 Room (SQLite) 统一结构说明

---

## 一、发型库表

### 服务端：`hairstyles` / 终端：`hairstyle_cache`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID，服务端生成 |
| `category` | VARCHAR(50) | NOT NULL | 分类：韩式/日式/甜酷风/欧美/氛围感/短发/中等长度/长发 |
| `name` | VARCHAR(100) | NOT NULL | 发型名称（如"女神大波浪"） |
| `description` | TEXT | NOT NULL DEFAULT '' | 发型详细描述 |
| `gender` | VARCHAR(4) | NOT NULL | 适用性别：`"男"` / `"女"` |
| `tags` | JSON / TEXT | DEFAULT '[]' | 标签数组（如 ["卷发","长发","女神"]） |
| `image_url` | VARCHAR(500) | NOT NULL DEFAULT '' | 远程图片访问 URL |
| `local_image_path` | VARCHAR(500) | — | **终端专用**：本地缓存文件路径 |
| `sort_order` | INTEGER | DEFAULT 0 | 排序序号，升序排列 |
| `is_active` | BOOLEAN / INTEGER | DEFAULT TRUE | 启用状态；false = 已下架（终端自动删除） |
| `version` | BIGINT | DEFAULT 1 | 数据版本号，每次修改 +1，用于增量同步 |
| `created_at` | TIMESTAMP | DEFAULT NOW() | 创建时间 |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | 最后修改时间 |

**索引：**

| 索引名 | 字段 | 说明 |
|---|---|---|
| `idx_hairstyles_gender` | gender | 按性别查询 |
| `idx_hairstyles_version` | version | 增量同步过滤 |
| `idx_hairstyles_active` | is_active | 过滤已启用数据 |

---

## 二、发色库表

### 服务端：`hair_colors` / 终端：`hair_color_cache`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID，服务端生成 |
| `category` | VARCHAR(50) | NOT NULL | 色系：温感色系/流光金系/清冷色系/薄暮紫系/热情红系/元气橙系/中性灰阶系/静谧蓝调系 |
| `name` | VARCHAR(100) | NOT NULL | 发色名称（如"蜂蜜茶色"） |
| `procedure` | TEXT | NOT NULL DEFAULT '' | 染发标准流程（Markdown 格式） |
| `color_hex` | VARCHAR(7) | 可空 | 十六进制色值，如 `#C4925A`，用于 UI 色块预览 |
| `image_url` | VARCHAR(500) | NOT NULL DEFAULT '' | 发色色样图片 URL |
| `local_image_path` | VARCHAR(500) | — | **终端专用**：本地缓存文件路径 |
| `sort_order` | INTEGER | DEFAULT 0 | 排序序号 |
| `is_active` | BOOLEAN / INTEGER | DEFAULT TRUE | 启用状态 |
| `version` | BIGINT | DEFAULT 1 | 数据版本号，每次修改 +1 |
| `created_at` | TIMESTAMP | DEFAULT NOW() | 创建时间 |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | 最后修改时间 |

**索引：**

| 索引名 | 字段 | 说明 |
|---|---|---|
| `idx_hair_colors_category` | category | 按色系查询 |
| `idx_hair_colors_version` | version | 增量同步过滤 |

---

## 三、终端同步记录表

### 服务端：`device_sync_log`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | BIGSERIAL | PK | 自增 ID |
| `device_id` | VARCHAR(64) | UNIQUE NOT NULL | 终端设备标识 |
| `device_name` | VARCHAR(200) | 可空 | 设备型号名称 |
| `last_hairstyle_version` | BIGINT | DEFAULT 0 | 该终端已同步到的发型库版本号 |
| `last_color_version` | BIGINT | DEFAULT 0 | 该终端已同步到的发色库版本号 |
| `last_sync_at` | TIMESTAMP | DEFAULT NOW() | 最后同步时间 |

**索引：**

| 索引名 | 字段 | 说明 |
|---|---|---|
| `idx_device_sync_device` | device_id | 按设备查询同步状态 |

---

## 四、终端同步元数据表

### 终端 Room 本地：`sync_metadata`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `key` | VARCHAR(64) | PK | 元数据键名 |
| `value` | VARCHAR(255) | NOT NULL | 元数据值 |

**预定义 key：**

| key | value 示例 | 说明 |
|---|---|---|
| `hairstyle_version` | `"5"` | 本地已同步的发型库版本号 |
| `color_version` | `"3"` | 本地已同步的发色库版本号 |
| `last_sync_at` | `"1718612345678"` | 最后同步时间戳（毫秒） |
| `last_sync_result` | `"success"` | 上次同步结果 |

---

## 五、关系图

```
┌──────────────────────────────────────────────────────────────────┐
│                        服务端 PostgreSQL                          │
│                                                                  │
│  ┌──────────────────────┐    ┌──────────────────────────┐       │
│  │     hairstyles        │    │      hair_colors          │       │
│  ├──────────────────────┤    ├──────────────────────────┤       │
│  │ id (PK)              │    │ id (PK)                  │       │
│  │ category             │    │ category                 │       │
│  │ name                 │    │ name                     │       │
│  │ description          │    │ procedure                │       │
│  │ gender               │    │ color_hex ?              │       │
│  │ tags (JSON)          │    │ image_url                │       │
│  │ image_url            │    │ sort_order               │       │
│  │ sort_order           │    │ is_active                │       │
│  │ is_active            │    │ version                  │       │
│  │ version              │    │ created_at               │       │
│  │ created_at           │    │ updated_at               │       │
│  │ updated_at           │    └──────────────────────────┘       │
│  └──────────────────────┘                                       │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    device_sync_log                        │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │ id (PK) │ device_id (UNIQUE) │ device_name │             │   │
│  │ last_hairstyle_version │ last_color_version │ last_sync_at│   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
                              │ 同步
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                        终端 Room (SQLite)                        │
│                                                                  │
│  ┌──────────────────────────┐    ┌──────────────────────────┐   │
│  │    hairstyle_cache        │    │    hair_color_cache       │   │
│  ├──────────────────────────┤    ├──────────────────────────┤   │
│  │ id (PK)                  │    │ id (PK)                  │   │
│  │ category                 │    │ category                 │   │
│  │ name                     │    │ name                     │   │
│  │ description              │    │ procedure                │   │
│  │ gender                   │    │ color_hex ?              │   │
│  │ tags                     │    │ image_url                │   │
│  │ image_url                │    │ local_image_path          │   │
│  │ local_image_path          │    │ sort_order               │   │
│  │ sort_order               │    │ is_active                │   │
│  │ is_active                │    │ version                  │   │
│  │ version                  │    └──────────────────────────┘   │
│  └──────────────────────────┘                                   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    sync_metadata                          │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │ key (PK)       │  value                                  │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │ hairstyle_version │ "5"                                  │   │
│  │ color_version     │ "3"                                  │   │
│  │ last_sync_at      │ "1718612345678"                      │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

---

## 六、终端缓存 Entity（Room 代码结构）

```kotlin
// 发型缓存
@Entity(tableName = "hairstyle_cache",
    indices = [Index(value = ["gender", "category"])])
data class HairstyleCacheEntity(
    @PrimaryKey val id: String,
    val category: String,
    val name: String,
    val description: String,
    val gender: String,
    val tags: String = "[]",
    val imageUrl: String = "",
    val localImagePath: String = "",
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val version: Long = 0
)

// 发色缓存
@Entity(tableName = "hair_color_cache",
    indices = [Index(value = ["category"])])
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

// 同步元数据
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String,
    val value: String
)
```
