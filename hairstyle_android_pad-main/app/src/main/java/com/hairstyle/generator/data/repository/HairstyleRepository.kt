package com.hairstyle.generator.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.hairstyle.generator.data.api.HairstyleApiService
import com.hairstyle.generator.data.api.NetworkConfig
import com.hairstyle.generator.data.model.*
import com.hairstyle.generator.data.storage.HistoryManager
import com.hairstyle.generator.utils.ImageUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File

/**
 * 发型生成数据仓库
 */
class HairstyleRepository(
    private val context: Context,
    private val apiService: HairstyleApiService = NetworkConfig.apiService
) {
    private val historyManager = HistoryManager(context)

    /**
     * 创建新会话
     */
    suspend fun createSession(): Result<CreateSessionResponse> {
        return try {
            val response = apiService.createSession()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create session: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 创建上传会话（专门用于扫码上传）
     */
    suspend fun createUploadSession(deviceId: String): Result<CreateSessionResponse> {
        return createSession()
    }

    /**
     * 获取上传会话状态
     */
    suspend fun getUploadSession(sessionId: String): Result<SessionStatusResponse> {
        return getSessionStatus(sessionId)
    }

    /**
     * 上传图片
     */
    suspend fun uploadImage(
        sessionId: String,
        imageType: String,
        imageUri: Uri
    ): Result<UploadImageResponse> {
        return try {
            // 将Uri转换为文件
            val imageFile = ImageUtils.uriToFile(context, imageUri)
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

            val response = apiService.uploadImage(sessionId, imageType, body)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to upload image: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 上传Bitmap图片
     */
    suspend fun uploadBitmap(
        sessionId: String,
        imageType: String,
        bitmap: Bitmap
    ): Result<UploadImageResponse> {
        return try {
            // 将Bitmap保存为临时文件
            val imageFile = ImageUtils.bitmapToFile(context, bitmap, "${imageType}_${System.currentTimeMillis()}.jpg")
            val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

            val response = apiService.uploadImage(sessionId, imageType, body)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to upload bitmap: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取会话状态
     */
    suspend fun getSessionStatus(sessionId: String): Result<SessionStatusResponse> {
        return try {
            val response = apiService.getSessionStatus(sessionId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get session status: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 启动发型生成处理（异步）
     */
    suspend fun processHairstyle(sessionId: String): Result<StartProcessResponse> {
        return try {
            // 现在使用异步方式，不需要长时间超时
            val response = apiService.startProcessHairstyle(sessionId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to start hairstyle processing: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 启动换发色处理（异步）
     */
    suspend fun processColor(sessionId: String): Result<StartProcessResponse> {
        return try {
            val response = apiService.startProcessColor(sessionId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to start color processing: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 启动3D照片转视频处理（异步）
     */
    suspend fun process3D(sessionId: String): Result<StartProcessResponse> {
        return try {
            val response = apiService.startProcess3D(sessionId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to start 3D processing: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 重置图片
     */
    suspend fun resetImage(sessionId: String, imageType: String): Result<ResetImageResponse> {
        return try {
            val response = apiService.resetImage(sessionId, imageType)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to reset image: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 设备激活
     */
    suspend fun activateDevice(deviceId: String, activationCode: String): Result<DeviceActivationResponse> {
        return try {
            val request = DeviceActivationRequest(deviceId, activationCode)
            val response = apiService.activateDevice(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to activate device: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 检查订阅状态
     */
    suspend fun checkSubscription(deviceId: String): Result<SubscriptionCheckResponse> {
        return try {
            val request = SubscriptionCheckRequest(deviceId)
            val response = apiService.checkSubscription(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to check subscription: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 轮询会话状态直到准备就绪
     */
    fun pollSessionStatus(sessionId: String): Flow<SessionStatusResponse> = flow {
        while (true) {
            val result = getSessionStatus(sessionId)
            if (result.isSuccess) {
                val status = result.getOrThrow()
                emit(status)
                if (status.ready_to_process || status.status == "failed") {
                    break
                }
            } else {
                throw result.exceptionOrNull() ?: Exception("Unknown error")
            }
            kotlinx.coroutines.delay(2000) // 每2秒检查一次
        }
    }

    /**
     * 异步处理发型生成（启动任务后轮询状态）
     */
    fun processHairstyleAsync(sessionId: String): Flow<SessionStatusResponse> = flow {
        try {
            // 1. 先启动处理任务（现在是异步的，会立即返回）
            val response = apiService.startProcessHairstyle(sessionId)
            if (!response.isSuccessful || response.body() == null) {
                throw Exception("启动任务失败: ${response.message()}")
            }
            val startResult = response.body()!!
            if (!startResult.success) {
                throw Exception("启动任务失败: ${startResult.error ?: "未知错误"}")
            }

            // 2. 轮询会话状态直到完成
            while (true) {
                val statusResult = getSessionStatus(sessionId)
                if (statusResult.isSuccess) {
                    val status = statusResult.getOrThrow()
                    emit(status)

                    // 检查是否完成、失败或取消
                    when (status.status) {
                        "completed" -> break
                        "failed", "cancelled" -> break
                    }
                } else {
                    throw statusResult.exceptionOrNull() ?: Exception("获取状态失败")
                }

                kotlinx.coroutines.delay(3000) // 每3秒检查一次
            }
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * 异步处理换发色（启动任务后轮询状态）
     */
    fun processColorAsync(sessionId: String): Flow<SessionStatusResponse> = flow {
        try {
            // 1. 先启动换发色任务（现在是异步的，会立即返回）
            val response = apiService.startProcessColor(sessionId)
            if (!response.isSuccessful || response.body() == null) {
                throw Exception("启动换发色任务失败: ${response.message()}")
            }
            val startResult = response.body()!!
            if (!startResult.success) {
                throw Exception("启动换发色任务失败: ${startResult.error ?: "未知错误"}")
            }

            // 2. 轮询会话状态直到完成
            while (true) {
                val statusResult = getSessionStatus(sessionId)
                if (statusResult.isSuccess) {
                    val status = statusResult.getOrThrow()
                    emit(status)

                    // 检查是否完成、失败或取消
                    when (status.status) {
                        "completed" -> break
                        "failed", "cancelled" -> break
                    }
                } else {
                    throw statusResult.exceptionOrNull() ?: Exception("获取状态失败")
                }

                kotlinx.coroutines.delay(3000) // 每3秒检查一次
            }
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * 获取API基础URL
     */
    fun getBaseUrl(): String {
        return NetworkConfig.baseUrl
    }

    /**
     * 保存生成历史记录
     */
    fun saveGenerationHistory(
        sessionId: String,
        userImageUrl: String,
        hairstyleImageUrl: String,
        resultUrls: List<String>
    ) {
        val history = GenerationHistory(
            id = "history_${System.currentTimeMillis()}",
            sessionId = sessionId,
            userImageUrl = userImageUrl,
            hairstyleImageUrl = hairstyleImageUrl,
            resultUrls = resultUrls,
            generatedAt = System.currentTimeMillis()
        )
        historyManager.saveHistory(history)
    }

    /**
     * 获取历史记录
     */
    fun getHistory(): List<GenerationHistory> {
        return historyManager.getHistory()
    }

    /**
     * 删除历史记录
     */
    fun deleteHistory(historyId: String) {
        historyManager.deleteHistory(historyId)
    }

    /**
     * 清空历史记录
     */
    fun clearHistory() {
        historyManager.clearHistory()
    }

    /**
     * 更新收藏状态
     */
    fun updateFavoriteStatus(historyId: String, isFavorite: Boolean) {
        historyManager.updateFavoriteStatus(historyId, isFavorite)
    }

    /**
     * 取消任务 (基于sessionId)
     */
    suspend fun cancelSession(sessionId: String): Result<CancelSessionResponse> {
        return try {
            val response = apiService.cancelSession(sessionId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to cancel session: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}