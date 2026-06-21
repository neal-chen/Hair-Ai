package com.hairstyle.generator.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * API请求和响应数据模型
 */

// 创建会话请求响应
@Parcelize
data class CreateSessionResponse(
    val session_id: String,
    val user_upload_url: String,
    val hairstyle_upload_url: String,
    val status: String
) : Parcelable

// 上传图片响应
@Parcelize
data class UploadImageResponse(
    val success: Boolean,
    val message: String? = null,
    val image_url: String? = null,
    val error: String? = null
) : Parcelable

// 会话状态响应
@Parcelize
data class SessionStatusResponse(
    val session_id: String,
    val has_user_image: Boolean,
    val has_hairstyle_image: Boolean,
    val user_image_uploaded: Boolean? = null,
    val hairstyle_image_uploaded: Boolean? = null,
    val user_image_url: String? = null,
    val hairstyle_image_url: String? = null,
    val status: String,
    val ready_to_process: Boolean,
    val task_id: String? = null,
    val can_cancel: Boolean? = null,
    val result_urls: List<String>? = null // 当任务完成时包含生成结果
) : Parcelable

// 发型生成响应
@Parcelize
data class GenerateHairstyleResponse(
    val success: Boolean,
    val result_urls: List<String>? = null,
    val count: Int? = null,
    val error: String? = null,
    val taskId: String? = null
) : Parcelable

// 启动处理任务响应
@Parcelize
data class StartProcessResponse(
    val success: Boolean,
    val message: String? = null,
    val session_id: String? = null,
    val status: String? = null,
    val error: String? = null
) : Parcelable

// 重置图片响应
@Parcelize
data class ResetImageResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
) : Parcelable

// 设备激活请求
@Parcelize
data class DeviceActivationRequest(
    val device_id: String,
    val activation_code: String
) : Parcelable

// 设备激活响应
@Parcelize
data class DeviceActivationResponse(
    val success: Boolean,
    val message: String? = null,
    val subscription_type: String? = null,
    val expires_at: String? = null,
    val days_remaining: Int? = null,
    val error: String? = null,
    val device_already_activated: Boolean? = null,
    val current_subscription: String? = null
) : Parcelable

// 订阅检查请求
@Parcelize
data class SubscriptionCheckRequest(
    val device_id: String
) : Parcelable

// 订阅检查响应
@Parcelize
data class SubscriptionCheckResponse(
    val success: Boolean,
    val status: String? = null,
    val subscription_type: String? = null,
    val expires_at: String? = null,
    val days_remaining: Int? = null,
    val activated_at: String? = null,
    val error: String? = null,
    val requires_activation: Boolean? = null,
    val requires_renewal: Boolean? = null,
    val expired_at: String? = null
) : Parcelable

// 本地会话数据模型
@Parcelize
data class HairstyleSession(
    val sessionId: String,
    val userImageUrl: String? = null,
    val hairstyleImageUrl: String? = null,
    val resultUrls: List<String> = emptyList(),
    val status: SessionStatus = SessionStatus.CREATED,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

// 会话状态枚举
enum class SessionStatus {
    CREATED,
    UPLOADING,
    READY_TO_PROCESS,
    PROCESSING,
    COMPLETED,
    FAILED
}

// 发型库数据模型
@Parcelize
data class HairstyleTemplate(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val category: String,
    val gender: String = "女", // 男/女
    val tags: List<String> = emptyList()
) : Parcelable

// 发型分类
@Parcelize
data class HairstyleCategory(
    val id: String,
    val name: String,
    val gender: String, // 男/女
    val hairstyles: List<HairstyleTemplate> = emptyList()
) : Parcelable

// 历史记录数据模型
@Parcelize
data class GenerationHistory(
    val id: String,
    val sessionId: String,
    val userImageUrl: String,
    val hairstyleImageUrl: String,
    val resultUrls: List<String>,
    val generatedAt: Long,
    val isFavorite: Boolean = false
) : Parcelable

// 取消会话任务响应模型
data class CancelSessionResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
    val session_id: String? = null,
    val task_id: String? = null
)

// 发色模板
@Parcelize
data class HairColorTemplate(
    val id: String,
    val name: String,           // 发色名称
    val category: String,       // 色系
    val imageUrl: String,       // 图片路径
    val procedure: String = ""  // 标准流程操作（存储但暂不展示）
) : Parcelable

// 发色分类
@Parcelize
data class HairColorCategory(
    val id: String,
    val name: String,           // 色系名称
    val colors: List<HairColorTemplate> = emptyList()
) : Parcelable