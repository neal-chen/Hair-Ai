package com.hairstyle.generator.data.api

import com.hairstyle.generator.data.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * 发型生成API服务接口
 */
interface HairstyleApiService {

    /**
     * 创建新的上传会话
     */
    @POST("api/create-session")
    suspend fun createSession(): Response<CreateSessionResponse>

    /**
     * 上传图片
     * @param sessionId 会话ID
     * @param imageType 图片类型: "user" 或 "hairstyle"
     * @param image 图片文件
     */
    @Multipart
    @POST("api/upload/{session_id}/{image_type}")
    suspend fun uploadImage(
        @Path("session_id") sessionId: String,
        @Path("image_type") imageType: String,
        @Part image: MultipartBody.Part
    ): Response<UploadImageResponse>

    /**
     * 获取会话状态
     */
    @GET("api/session/{session_id}")
    suspend fun getSessionStatus(
        @Path("session_id") sessionId: String
    ): Response<SessionStatusResponse>

    /**
     * 启动发型转换处理（异步）
     */
    @POST("api/process/{session_id}")
    suspend fun startProcessHairstyle(
        @Path("session_id") sessionId: String
    ): Response<StartProcessResponse>

    /**
     * 启动换发色处理（异步）
     */
    @POST("api/process-color/{session_id}")
    suspend fun startProcessColor(
        @Path("session_id") sessionId: String
    ): Response<StartProcessResponse>

    /**
     * 启动3D照片转视频处理（异步）
     */
    @POST("api/process-3d/{session_id}")
    suspend fun startProcess3D(
        @Path("session_id") sessionId: String
    ): Response<StartProcessResponse>

    /**
     * 获取上传的图片
     */
    @GET("api/image/{session_id}/{image_type}")
    suspend fun getImage(
        @Path("session_id") sessionId: String,
        @Path("image_type") imageType: String,
        @Query("t") timestamp: Long = System.currentTimeMillis()
    ): Response<ResponseBody>

    /**
     * 重置指定类型的图片
     */
    @POST("api/reset-image/{session_id}/{image_type}")
    suspend fun resetImage(
        @Path("session_id") sessionId: String,
        @Path("image_type") imageType: String
    ): Response<ResetImageResponse>

    /**
     * 设备激活
     */
    @POST("api/device/activate")
    suspend fun activateDevice(
        @Body request: DeviceActivationRequest
    ): Response<DeviceActivationResponse>

    /**
     * 检查订阅状态
     */
    @POST("api/device/check-subscription")
    suspend fun checkSubscription(
        @Body request: SubscriptionCheckRequest
    ): Response<SubscriptionCheckResponse>

    /**
     * 取消任务 (基于sessionId)
     */
    @POST("api/cancel-session/{session_id}")
    suspend fun cancelSession(
        @Path("session_id") sessionId: String
    ): Response<CancelSessionResponse>
}