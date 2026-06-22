package com.hairstyle.generator.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 同步元数据 Entity
 * 记录本地缓存的版本状态，用于增量同步
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String,    // "hairstyle_version" / "color_version" / "last_sync_at"
    val value: String
)
