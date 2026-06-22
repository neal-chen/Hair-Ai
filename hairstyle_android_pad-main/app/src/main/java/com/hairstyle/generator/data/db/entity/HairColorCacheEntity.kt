package com.hairstyle.generator.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 发色缓存 Entity
 * 对应服务端 hair_colors 表
 */
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
    val version: Long = 0,
)
