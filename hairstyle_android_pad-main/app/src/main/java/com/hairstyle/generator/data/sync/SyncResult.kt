package com.hairstyle.generator.data.sync

/**
 * 同步结果密封类
 */
sealed class SyncResult {
    data object NoNetwork : SyncResult()
    data class Success(
        val count: Int = 0,
        val deletedCount: Int = 0,
        val hasChanges: Boolean = false,
    ) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
