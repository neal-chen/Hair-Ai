package com.hairstyle.generator.data.db

import com.hairstyle.generator.data.api.LibraryApiService
import com.hairstyle.generator.data.db.converter.toCacheEntity
import com.hairstyle.generator.data.db.dao.HairColorCacheDao
import com.hairstyle.generator.data.db.dao.SyncMetadataDao
import com.hairstyle.generator.data.db.entity.SyncMetadataEntity
import com.hairstyle.generator.data.sync.SyncResult

/**
 * 发色同步 Repository
 */
class HairColorSyncRepository(
    private val api: LibraryApiService,
    private val cacheDao: HairColorCacheDao,
    private val syncMetaDao: SyncMetadataDao,
) {

    suspend fun sync(deviceId: String): SyncResult {
        val localVersion = syncMetaDao.getValue("color_version")?.toLongOrNull() ?: 0L

        val response = api.getHairColors(version = localVersion, deviceId = deviceId)
        if (!response.isSuccessful || response.body() == null) {
            return SyncResult.Error("API error: ${response.code()}")
        }

        val data = response.body()!!.data
        if (data.colors.isNotEmpty()) {
            cacheDao.upsertAll(data.colors.map { it.toCacheEntity() })
        }
        if (data.deleted_ids.isNotEmpty()) {
            data.deleted_ids.forEach { id -> cacheDao.deleteById(id) }
        }

        syncMetaDao.set(SyncMetadataEntity("color_version", data.current_version.toString()))
        syncMetaDao.set(SyncMetadataEntity("last_sync_at", System.currentTimeMillis().toString()))

        return SyncResult.Success(
            count = data.colors.size,
            deletedCount = data.deleted_ids.size,
            hasChanges = data.colors.isNotEmpty() || data.deleted_ids.isNotEmpty(),
        )
    }
}
