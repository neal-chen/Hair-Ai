package com.hairstyle.generator.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hairstyle.generator.data.db.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

/**
 * 同步元数据 DAO
 */
@Dao
interface SyncMetadataDao {

    @Query("SELECT value FROM sync_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Query("SELECT value FROM sync_metadata WHERE `key` = :key LIMIT 1")
    fun getValueFlow(key: String): Flow<String?>

    @Upsert
    suspend fun set(metadata: SyncMetadataEntity)
}
