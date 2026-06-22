package com.hairstyle.generator.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 发型缓存 Entity
 * 对应服务端 hairstyles 表，终端只读缓存
 */
@Entity(
    tableName = "hairstyle_cache",
    indices = [
        Index(value = ["gender", "isActive"]),
        Index(value = ["category"]),
    ]
)
data class HairstyleCacheEntity(
    @PrimaryKey val id: String,
    val category: String,
    val name: String,
    val description: String = "",
    val gender: String = "女",
    val tags: String = "[]",               // JSON array
    val imageUrl: String = "",              // 远程 URL
    val localImagePath: String = "",        // 本地缓存路径
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val version: Long = 0,                  // 服务端版本号
)
