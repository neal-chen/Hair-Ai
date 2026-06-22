package com.hairstyle.generator.data.sync

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 图片文件缓存管理器
 * 将远程发型/发色图片下载到本地缓存目录
 */
class ImageCacheManager(private val context: Context) {

    private val hairstyleCacheDir = File(context.cacheDir, "hairstyle_images")
    private val colorCacheDir = File(context.cacheDir, "hair_color_images")
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 下载单张图片到本地缓存
     */
    suspend fun downloadImage(imageUrl: String, targetFile: File): Boolean {
        if (targetFile.exists()) return true

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(imageUrl).build()
                val response = client.newCall(request).execute()
                response.body?.byteStream()?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * 后台静默下载新图片（启动时调用）
     */
    fun downloadNewImagesInBackground() {
        // Room 缓存中的图片会在 UI 层通过 Glide 加载时自动缓存
        // 此方法为预缓存，可选提前下载
        // 具体实现可根据需要从 Room 读取新数据后调用 downloadImage
    }

    fun clearCache() {
        hairstyleCacheDir.deleteRecursively()
        colorCacheDir.deleteRecursively()
    }
}
