package com.hairstyle.generator.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.hairstyle.generator.data.api.LibraryApiService
import com.hairstyle.generator.data.api.NetworkConfig
import com.hairstyle.generator.data.db.AppDatabase
import com.hairstyle.generator.data.db.HairColorSyncRepository
import com.hairstyle.generator.data.db.HairstyleSyncRepository
import com.hairstyle.generator.utils.DeviceUtils

/**
 * 发型/发色库同步协调器
 * 在 App 启动时调用，自动判断全量/增量同步
 */
class LibrarySyncManager(private val context: Context) {

    private val api: LibraryApiService = NetworkConfig.apiService as LibraryApiService
    private val db = AppDatabase.getInstance(context)

    private val hairstyleRepo = HairstyleSyncRepository(
        api = api,
        cacheDao = db.hairstyleCacheDao(),
        syncMetaDao = db.syncMetadataDao(),
    )

    private val colorRepo = HairColorSyncRepository(
        api = api,
        cacheDao = db.hairColorCacheDao(),
        syncMetaDao = db.syncMetadataDao(),
    )

    private val imageCache = ImageCacheManager(context)

    /**
     * 在 App 启动时调用，自动执行增量同步
     */
    suspend fun syncOnLaunch(): SyncResult {
        if (!isNetworkAvailable()) {
            return SyncResult.NoNetwork
        }

        val deviceId = DeviceUtils.getDeviceId(context)

        // 并行同步发型 + 发色
        val hairstyleResult = hairstyleRepo.sync(deviceId)
        val colorResult = colorRepo.sync(deviceId)

        // 静默下载新图片
        val hasChanges = when {
            hairstyleResult is SyncResult.Success && hairstyleResult.hasChanges -> true
            colorResult is SyncResult.Success && colorResult.hasChanges -> true
            else -> false
        }
        if (hasChanges) {
            imageCache.downloadNewImagesInBackground()
        }

        return SyncResult.Success(
            count = (hairstyleResult as? SyncResult.Success)?.count ?: 0 +
                    (colorResult as? SyncResult.Success)?.count ?: 0,
            hasChanges = hasChanges,
        )
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
