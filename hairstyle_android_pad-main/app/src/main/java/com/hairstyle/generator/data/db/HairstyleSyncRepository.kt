package com.hairstyle.generator.data.db

import com.hairstyle.generator.data.api.LibraryApiService
import com.hairstyle.generator.data.db.converter.toCacheEntity
import com.hairstyle.generator.data.db.dao.HairstyleCacheDao
import com.hairstyle.generator.data.db.dao.SyncMetadataDao
import com.hairstyle.generator.data.db.entity.SyncMetadataEntity
import com.hairstyle.generator.data.sync.SyncResult

/**
 * 发型同步 Repository
 * 从服务端获取增量数据并写入 Room 缓存
 */
class HairstyleSyncRepository(
    private val api: LibraryApiService,
    private val cacheDao: HairstyleCacheDao,
    private val syncMetaDao: SyncMetadataDao,
) {

    /**
     * 执行同步（自动判断全量/增量）
     */
    suspend fun sync(deviceId: String): SyncResult {
        val localVersion = syncMetaDao.getValue("hairstyle_version")?.toLongOrNull() ?: 0L

        // 请求增量数据
        val response = api.getHairstyles(
            version = localVersion,
            deviceId = deviceId,
        )

        if (!response.isSuccessful || response.body() == null) {
            return SyncResult.Error("API error: ${response.code()}")
        }

        val data = response.body()!!.data

        // 写入缓存
        if (data.hairstyles.isNotEmpty()) {
            val entities = data.hairstyles.map { it.toCacheEntity() }
            cacheDao.upsertAll(entities)
        }

        // 删除已下架的
        if (data.deleted_ids.isNotEmpty()) {
            data.deleted_ids.forEach { id -> cacheDao.deleteById(id) }
        }

        // 更新版本号
        syncMetaDao.set(
            SyncMetadataEntity("hairstyle_version", data.current_version.toString())
        )
        syncMetaDao.set(
            SyncMetadataEntity("last_sync_at", System.currentTimeMillis().toString())
        )

        return SyncResult.Success(
            count = data.hairstyles.size,
            deletedCount = data.deleted_ids.size,
            hasChanges = data.hairstyles.isNotEmpty() || data.deleted_ids.isNotEmpty(),
        )
    }
}
