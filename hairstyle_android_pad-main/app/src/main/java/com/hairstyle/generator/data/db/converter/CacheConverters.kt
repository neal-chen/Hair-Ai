package com.hairstyle.generator.data.db.converter

import androidx.room.TypeConverter
import com.hairstyle.generator.data.db.entity.HairstyleCacheEntity
import com.hairstyle.generator.data.db.entity.HairColorCacheEntity
import com.hairstyle.generator.data.model.HairstyleTemplate
import com.hairstyle.generator.data.model.HairColorTemplate

/**
 * Room TypeConverter 与 Entity ↔ Model 转换
 */
class CacheConverters {

    // ── Boolean ↔ Int (SQLite 没有原生 Boolean) ──

    @TypeConverter
    fun fromBoolean(value: Boolean): Int = if (value) 1 else 0

    @TypeConverter
    fun toBoolean(value: Int): Boolean = value == 1
}

/**
 * 将 API 返回的发型数据转换为缓存 Entity
 */
fun HairstyleSyncItem.toCacheEntity(): HairstyleCacheEntity {
    return HairstyleCacheEntity(
        id = id,
        category = category,
        name = name,
        description = description,
        gender = gender,
        tags = tags,
        imageUrl = imageUrl,
        sortOrder = sortOrder,
        isActive = isActive,
        version = version,
    )
}

/**
 * 将缓存 Entity 转换为 UI 层使用的发型模板
 */
fun HairstyleCacheEntity.toTemplate(): HairstyleTemplate {
    return HairstyleTemplate(
        id = id,
        name = name,
        description = description,
        imageUrl = imageUrl,
        category = category,
        gender = gender,
    )
}

/**
 * 将 API 返回的发色数据转换为缓存 Entity
 */
fun HairColorSyncItem.toCacheEntity(): HairColorCacheEntity {
    return HairColorCacheEntity(
        id = id,
        category = category,
        name = name,
        procedure = procedure,
        colorHex = colorHex,
        imageUrl = imageUrl,
        sortOrder = sortOrder,
        isActive = isActive,
        version = version,
    )
}

/**
 * 将缓存 Entity 转换为 UI 层使用的发色模板
 */
fun HairColorCacheEntity.toTemplate(): HairColorTemplate {
    return HairColorTemplate(
        id = id,
        name = name,
        category = category,
        imageUrl = imageUrl,
        procedure = procedure,
    )
}

// ── 同步 API 数据模型（来自服务端响应） ──

data class HairstyleSyncItem(
    val id: String,
    val category: String,
    val name: String,
    val description: String,
    val gender: String,
    val tags: String,
    val imageUrl: String,
    val sortOrder: Int,
    val isActive: Boolean,
    val version: Long,
)

data class HairColorSyncItem(
    val id: String,
    val category: String,
    val name: String,
    val procedure: String,
    val colorHex: String?,
    val imageUrl: String,
    val sortOrder: Int,
    val isActive: Boolean,
    val version: Long,
)

data class HairstyleSyncResponse(
    val success: Boolean,
    val data: HairstyleSyncPayload,
)

data class HairstyleSyncPayload(
    val hairstyles: List<HairstyleSyncItem>,
    val deleted_ids: List<String>,
    val current_version: Long,
)

data class HairColorSyncResponse(
    val success: Boolean,
    val data: HairColorSyncPayload,
)

data class HairColorSyncPayload(
    val colors: List<HairColorSyncItem>,
    val deleted_ids: List<String>,
    val current_version: Long,
)
