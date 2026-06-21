package com.hairstyle.generator.bridge

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.hairstyle.generator.data.repository.HairstyleRepository
import com.hairstyle.generator.ui.WebViewActivity
import com.hairstyle.generator.utils.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Android JavaScript Bridge
 * 提供Web端调用原生功能的接口
 */
class AndroidBridge(
    private val activity: WebViewActivity,
    private val webView: WebView
) {
    private val repository = HairstyleRepository(activity)
    private val gson = Gson()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // 当前等待的回调
    private var currentPhotoCallback: String? = null
    private var currentGalleryCallback: String? = null

    /**
     * 拍照
     */
    @JavascriptInterface
    fun takePhoto(callbackName: String) {
        currentPhotoCallback = callbackName

        activity.runOnUiThread {
            if (activity.hasCameraPermission()) {
                activity.launchCamera()
            } else {
                activity.requestCameraPermission("camera_for_photo")
            }
        }
    }

    /**
     * 从相册选择图片
     */
    @JavascriptInterface
    fun pickImage(callbackName: String) {
        currentGalleryCallback = callbackName

        activity.runOnUiThread {
            activity.launchGallery()
        }
    }

    /**
     * 处理相机拍照结果
     */
    fun handleCameraResult(uri: Uri) {
        coroutineScope.launch {
            try {
                val base64 = withContext(Dispatchers.IO) {
                    uriToBase64(uri)
                }
                callJsCallback(currentPhotoCallback, mapOf(
                    "success" to true,
                    "base64" to base64
                ))
            } catch (e: Exception) {
                callJsCallback(currentPhotoCallback, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "拍照处理失败")
                ))
            }
            currentPhotoCallback = null
        }
    }

    /**
     * 处理相机拍照错误
     */
    fun handleCameraError(error: String) {
        callJsCallback(currentPhotoCallback, mapOf(
            "success" to false,
            "error" to error
        ))
        currentPhotoCallback = null
    }

    /**
     * 处理相册选择结果
     */
    fun handleGalleryResult(uri: Uri) {
        coroutineScope.launch {
            try {
                val base64 = withContext(Dispatchers.IO) {
                    uriToBase64(uri)
                }
                callJsCallback(currentGalleryCallback, mapOf(
                    "success" to true,
                    "base64" to base64
                ))
            } catch (e: Exception) {
                callJsCallback(currentGalleryCallback, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "图片处理失败")
                ))
            }
            currentGalleryCallback = null
        }
    }

    /**
     * 处理相册选择错误
     */
    fun handleGalleryError(error: String) {
        callJsCallback(currentGalleryCallback, mapOf(
            "success" to false,
            "error" to error
        ))
        currentGalleryCallback = null
    }

    /**
     * 处理权限请求结果
     */
    fun handlePermissionResult(callbackType: String?, granted: Boolean) {
        if (callbackType == "camera_for_photo" && granted) {
            // 权限获取成功，重新启动相机
            activity.launchCamera()
        } else if (!granted) {
            callJsCallback(currentPhotoCallback, mapOf(
                "success" to false,
                "error" to "权限被拒绝"
            ))
            currentPhotoCallback = null
        }
    }

    /**
     * 上传图片到服务器
     */
    @JavascriptInterface
    fun uploadImage(sessionId: String, imageType: String, base64Data: String, callbackName: String) {
        coroutineScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    base64ToBitmap(base64Data)
                }
                val result = withContext(Dispatchers.IO) {
                    repository.uploadBitmap(sessionId, imageType, bitmap)
                }

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    callJsCallback(callbackName, mapOf(
                        "success" to true,
                        "url" to (response.image_url ?: "")
                    ))
                } else {
                    callJsCallback(callbackName, mapOf(
                        "success" to false,
                        "error" to (result.exceptionOrNull()?.message ?: "上传失败")
                    ))
                }
            } catch (e: Exception) {
                callJsCallback(callbackName, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "上传失败")
                ))
            }
        }
    }

    /**
     * 创建新会话
     */
    @JavascriptInterface
    fun createSession(callbackName: String) {
        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.createSession()
                }

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    callJsCallback(callbackName, mapOf(
                        "success" to true,
                        "session_id" to response.session_id
                    ))
                } else {
                    callJsCallback(callbackName, mapOf(
                        "success" to false,
                        "error" to (result.exceptionOrNull()?.message ?: "创建会话失败")
                    ))
                }
            } catch (e: Exception) {
                callJsCallback(callbackName, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "创建会话失败")
                ))
            }
        }
    }

    /**
     * 获取会话状态
     */
    @JavascriptInterface
    fun getSessionStatus(sessionId: String, callbackName: String) {
        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getSessionStatus(sessionId)
                }

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    callJsCallback(callbackName, mapOf(
                        "success" to true,
                        "status" to response.status,
                        "ready_to_process" to response.ready_to_process,
                        "has_user_image" to response.has_user_image,
                        "has_hairstyle_image" to response.has_hairstyle_image,
                        "user_image_url" to (response.user_image_url ?: ""),
                        "hairstyle_image_url" to (response.hairstyle_image_url ?: ""),
                        "result_urls" to (response.result_urls ?: emptyList<String>())
                    ))
                } else {
                    callJsCallback(callbackName, mapOf(
                        "success" to false,
                        "error" to (result.exceptionOrNull()?.message ?: "获取状态失败")
                    ))
                }
            } catch (e: Exception) {
                callJsCallback(callbackName, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "获取状态失败")
                ))
            }
        }
    }

    /**
     * 开始AI处理
     * @param processType 处理类型: 'hairstyle', 'haircolor', '3d'
     */
    @JavascriptInterface
    fun startProcess(sessionId: String, processType: String, callbackName: String) {
        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    when (processType) {
                        "haircolor" -> repository.processColor(sessionId)
                        "3d" -> repository.process3D(sessionId)
                        else -> repository.processHairstyle(sessionId)
                    }
                }

                if (result.isSuccess) {
                    callJsCallback(callbackName, mapOf(
                        "success" to true
                    ))
                } else {
                    callJsCallback(callbackName, mapOf(
                        "success" to false,
                        "error" to (result.exceptionOrNull()?.message ?: "处理启动失败")
                    ))
                }
            } catch (e: Exception) {
                callJsCallback(callbackName, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "处理启动失败")
                ))
            }
        }
    }

    /**
     * 获取本地发型库列表
     */
    @JavascriptInterface
    fun getHairstyleList(gender: String, callbackName: String) {
        coroutineScope.launch {
            try {
                val folderName = if (gender == "男") "hairstyles_man" else "hairstyles_woman"
                val assetManager = activity.assets
                val files = assetManager.list(folderName) ?: emptyArray()

                val hairstyles = files.mapNotNull { fileName ->
                    if (!fileName.endsWith(".png") && !fileName.endsWith(".jpg")) {
                        return@mapNotNull null
                    }
                    // 解析文件名: category_name_description.png
                    val namePart = fileName.substringBeforeLast(".")
                    val parts = namePart.split("_")

                    mapOf(
                        "category" to (parts.getOrNull(0) ?: ""),
                        "name" to (parts.getOrNull(1) ?: ""),
                        "description" to (parts.getOrNull(2) ?: parts.getOrNull(1) ?: ""),
                        "fileName" to fileName,
                        "path" to "file:///android_asset/$folderName/$fileName"
                    )
                }

                callJsCallback(callbackName, mapOf(
                    "success" to true,
                    "hairstyles" to hairstyles
                ))
            } catch (e: Exception) {
                callJsCallback(callbackName, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "获取发型库失败")
                ))
            }
        }
    }

    /**
     * 获取发型图片Base64
     */
    @JavascriptInterface
    fun getHairstyleImage(path: String, callbackName: String) {
        coroutineScope.launch {
            try {
                val base64 = withContext(Dispatchers.IO) {
                    // path格式: file:///android_asset/hairstyles_xxx/filename.png
                    val assetPath = path.replace("file:///android_asset/", "")
                    val inputStream = activity.assets.open(assetPath)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    bitmapToBase64(bitmap)
                }

                callJsCallback(callbackName, mapOf(
                    "success" to true,
                    "base64" to base64
                ))
            } catch (e: Exception) {
                callJsCallback(callbackName, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "获取图片失败")
                ))
            }
        }
    }

    /**
     * 显示Toast消息
     */
    @JavascriptInterface
    fun showToast(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 检查权限
     */
    @JavascriptInterface
    fun checkPermission(permission: String, callbackName: String) {
        val granted = when (permission) {
            "camera" -> activity.hasCameraPermission()
            else -> false
        }

        callJsCallback(callbackName, mapOf(
            "success" to true,
            "granted" to granted
        ))
    }

    /**
     * 请求权限
     */
    @JavascriptInterface
    fun requestPermission(permission: String, callbackName: String) {
        activity.runOnUiThread {
            when (permission) {
                "camera" -> activity.requestCameraPermission(callbackName)
                else -> {
                    callJsCallback(callbackName, mapOf(
                        "success" to false,
                        "error" to "未知权限类型"
                    ))
                }
            }
        }
    }

    /**
     * 保存图片到相册
     */
    @JavascriptInterface
    fun saveImage(base64Data: String, fileName: String, callbackName: String) {
        coroutineScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    base64ToBitmap(base64Data)
                }
                // TODO: 实现保存到相册功能
                callJsCallback(callbackName, mapOf(
                    "success" to true
                ))
            } catch (e: Exception) {
                callJsCallback(callbackName, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "保存失败")
                ))
            }
        }
    }

    /**
     * 分享图片
     */
    @JavascriptInterface
    fun shareImage(base64Data: String, callbackName: String) {
        coroutineScope.launch {
            try {
                // TODO: 实现分享功能
                callJsCallback(callbackName, mapOf(
                    "success" to true
                ))
            } catch (e: Exception) {
                callJsCallback(callbackName, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "分享失败")
                ))
            }
        }
    }

    /**
     * 生成上传二维码
     * @param imageType 图片类型: 'user' 或 'hairstyle'
     */
    @JavascriptInterface
    fun generateUploadQRCode(imageType: String, callbackName: String) {
        coroutineScope.launch {
            try {
                // 创建会话
                val sessionResult = withContext(Dispatchers.IO) {
                    repository.createSession()
                }

                if (!sessionResult.isSuccess) {
                    callJsCallback(callbackName, mapOf(
                        "success" to false,
                        "error" to "创建会话失败"
                    ))
                    return@launch
                }

                val session = sessionResult.getOrThrow()
                val uploadUrl = if (imageType == "user") {
                    session.user_upload_url
                } else {
                    session.hairstyle_upload_url
                }

                // 生成二维码
                val qrCodeBase64 = withContext(Dispatchers.IO) {
                    generateQRCodeBitmap(uploadUrl)
                }

                callJsCallback(callbackName, mapOf(
                    "success" to true,
                    "session_id" to session.session_id,
                    "upload_url" to uploadUrl,
                    "qrcode_base64" to qrCodeBase64
                ))
            } catch (e: Exception) {
                callJsCallback(callbackName, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "生成二维码失败")
                ))
            }
        }
    }

    /**
     * 从URL获取图片并转换为Base64
     * @param imageUrl 图片URL
     */
    @JavascriptInterface
    fun fetchImageAsBase64(imageUrl: String, callbackName: String) {
        coroutineScope.launch {
            try {
                val base64 = withContext(Dispatchers.IO) {
                    // 确保使用HTTPS
                    val url = if (imageUrl.startsWith("http://")) {
                        imageUrl.replace("http://", "https://")
                    } else {
                        imageUrl
                    }

                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.doInput = true
                    connection.connect()

                    val inputStream = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    connection.disconnect()

                    if (bitmap != null) {
                        bitmapToBase64(bitmap)
                    } else {
                        throw Exception("无法解码图片")
                    }
                }

                callJsCallback(callbackName, mapOf(
                    "success" to true,
                    "base64" to base64
                ))
            } catch (e: Exception) {
                callJsCallback(callbackName, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "获取图片失败")
                ))
            }
        }
    }

    /**
     * 为已有会话生成上传二维码（不创建新会话）
     * @param sessionId 已有的会话ID
     * @param imageType 图片类型: user 或 hairstyle
     */
    @JavascriptInterface
    fun generateQRCodeForSession(sessionId: String, imageType: String, callbackName: String) {
        coroutineScope.launch {
            try {
                // 构建上传URL
                val baseUrl = "https://web-production-bingli.up.railway.app"
                val uploadUrl = "$baseUrl/upload/$sessionId/$imageType"

                // 生成二维码
                val qrCodeBase64 = withContext(Dispatchers.IO) {
                    generateQRCodeBitmap(uploadUrl)
                }

                callJsCallback(callbackName, mapOf(
                    "success" to true,
                    "session_id" to sessionId,
                    "upload_url" to uploadUrl,
                    "qrcode_base64" to qrCodeBase64
                ))
            } catch (e: Exception) {
                callJsCallback(callbackName, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "生成二维码失败")
                ))
            }
        }
    }

    /**
     * 获取发色库数据
     * @return JSON格式的发色数据
     */
    @JavascriptInterface
    fun getHairColorsData(callbackName: String) {
        coroutineScope.launch {
            try {
                val jsonData = withContext(Dispatchers.IO) {
                    val inputStream = activity.assets.open("hair_colors/hair_colors_data.json")
                    inputStream.bufferedReader().use { it.readText() }
                }

                // 直接将JSON字符串传回
                val script = "$callbackName($jsonData)"
                activity.evaluateJavascript(script)
            } catch (e: Exception) {
                callJsCallback(callbackName, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "获取发色数据失败")
                ))
            }
        }
    }

    /**
     * 取消会话任务
     * @param sessionId 会话ID
     */
    @JavascriptInterface
    fun cancelSession(sessionId: String, callbackName: String) {
        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.cancelSession(sessionId)
                }

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    callJsCallback(callbackName, mapOf(
                        "success" to true,
                        "message" to (response.message ?: "已取消")
                    ))
                } else {
                    callJsCallback(callbackName, mapOf(
                        "success" to false,
                        "error" to (result.exceptionOrNull()?.message ?: "取消失败")
                    ))
                }
            } catch (e: Exception) {
                callJsCallback(callbackName, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "取消失败")
                ))
            }
        }
    }

    /**
     * 获取Assets目录下图片的Base64
     * @param assetPath Assets中的相对路径，如 'hair_colors/温感色系_蜂蜜茶色.png'
     */
    @JavascriptInterface
    fun getAssetImageBase64(assetPath: String, callbackName: String) {
        coroutineScope.launch {
            try {
                val base64 = withContext(Dispatchers.IO) {
                    val inputStream = activity.assets.open(assetPath)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (bitmap != null) {
                        bitmapToBase64(bitmap)
                    } else {
                        throw Exception("无法解码图片: $assetPath")
                    }
                }

                callJsCallback(callbackName, mapOf(
                    "success" to true,
                    "base64" to base64
                ))
            } catch (e: Exception) {
                callJsCallback(callbackName, mapOf(
                    "success" to false,
                    "error" to (e.message ?: "获取图片失败")
                ))
            }
        }
    }

    /**
     * 生成二维码Bitmap并转为Base64
     */
    private fun generateQRCodeBitmap(content: String): String {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bitmapToBase64(bitmap)
    }

    /**
     * 回调JavaScript函数
     */
    private fun callJsCallback(callbackName: String?, data: Map<String, Any?>) {
        callbackName ?: return
        val jsonResult = gson.toJson(data).replace("'", "\\'")
        val script = "$callbackName('$jsonResult')"
        activity.evaluateJavascript(script)
    }

    /**
     * Uri转Base64
     */
    private fun uriToBase64(uri: Uri): String {
        val inputStream = activity.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        // 压缩图片
        val compressedBitmap = ImageUtils.compressBitmap(bitmap, 1024, 1024)

        // 裁剪为1:1正方形
        val croppedBitmap = ImageUtils.cropToSquare(compressedBitmap)

        return bitmapToBase64(croppedBitmap)
    }

    /**
     * Bitmap转Base64
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    // ========================================================================
    //  高级封装 —— 前端一键调用，自动完成完整流程
    // ========================================================================

    /**
     * 发送进度更新到 JS 端
     */
    private fun sendProgress(callbackName: String, status: String, progress: Int, message: String) {
        activity.runOnUiThread {
            callJsCallback(callbackName, mapOf(
                "status" to status,
                "progress" to progress,
                "message" to message
            ))
        }
    }

    /**
     * 一键换发型
     *
     * 自动完成：创建会话 → 上传用户照片 → 上传发型参考图 → 启动 AI 处理 → 轮询结果
     * 前端收到多次回调（progress 更新），最终收到 status="completed" 或 "failed"
     *
     * @param userBase64      用户照片 Base64（不含 data:image 前缀）
     * @param hairstyleBase64 发型参考图 Base64
     * @param callbackName    JS 回调函数名，每次进度更新都会调用，最终结果 status=completed/failed
     */
    @JavascriptInterface
    fun changeHairstyle(userBase64: String, hairstyleBase64: String, callbackName: String) {
        coroutineScope.launch {
            try {
                // 1. 创建会话
                sendProgress(callbackName, "uploading", 5, "正在创建会话...")
                val sessionResult = withContext(Dispatchers.IO) { repository.createSession() }
                if (!sessionResult.isSuccess) throw Exception(sessionResult.exceptionOrNull()?.message ?: "创建会话失败")
                val sessionId = sessionResult.getOrThrow().session_id

                // 2. 上传用户照片
                sendProgress(callbackName, "uploading", 20, "正在上传用户照片...")
                val userBitmap = withContext(Dispatchers.IO) { base64ToBitmap(userBase64) }
                val userResult = withContext(Dispatchers.IO) { repository.uploadBitmap(sessionId, "user", userBitmap) }
                if (!userResult.isSuccess) throw Exception(userResult.exceptionOrNull()?.message ?: "上传用户照片失败")

                // 3. 上传发型参考图
                sendProgress(callbackName, "uploading", 40, "正在上传发型参考图...")
                val hairBitmap = withContext(Dispatchers.IO) { base64ToBitmap(hairstyleBase64) }
                val hairResult = withContext(Dispatchers.IO) { repository.uploadBitmap(sessionId, "hairstyle", hairBitmap) }
                if (!hairResult.isSuccess) throw Exception(hairResult.exceptionOrNull()?.message ?: "上传发型参考图失败")

                // 4. 启动 AI 处理并轮询
                sendProgress(callbackName, "processing", 60, "AI 正在分析脸型和发型...")

                var pollCount = 0
                repository.processHairstyleAsync(sessionId).collect { status ->
                    when (status.status) {
                        "processing" -> {
                            pollCount++
                            val p = minOf(65 + pollCount * 5, 95)
                            val msg = when {
                                pollCount <= 3 -> "AI 正在分析脸型特征..."
                                pollCount <= 6 -> "AI 正在匹配发型风格..."
                                else -> "AI 智能合成新发型中..."
                            }
                            sendProgress(callbackName, "processing", p, msg)
                        }
                        "completed" -> {
                            sendProgress(callbackName, "completed", 100, "换发型完成！", mapOf(
                                "session_id" to sessionId,
                                "result_urls" to (status.result_urls ?: emptyList<String>()),
                                "user_image_url" to (status.user_image_url ?: ""),
                                "hairstyle_image_url" to (status.hairstyle_image_url ?: "")
                            ))
                        }
                        "failed" -> {
                            sendProgress(callbackName, "failed", 0, "AI 处理失败")
                        }
                        "cancelled" -> {
                            sendProgress(callbackName, "failed", 0, "任务已取消")
                        }
                    }
                }
            } catch (e: Exception) {
                sendProgress(callbackName, "failed", 0, e.message ?: "处理失败")
            }
        }
    }

    /**
     * 一键换发色
     *
     * 自动完成：创建会话 → 上传用户照片 → 上传发色参考图 → 启动 AI 处理 → 轮询结果
     *
     * @param userBase64  用户照片 Base64
     * @param colorBase64 目标发色参考图 Base64
     * @param callbackName JS 回调函数名
     */
    @JavascriptInterface
    fun changeHairColor(userBase64: String, colorBase64: String, callbackName: String) {
        coroutineScope.launch {
            try {
                // 1. 创建会话
                sendProgress(callbackName, "uploading", 5, "正在创建会话...")
                val sessionResult = withContext(Dispatchers.IO) { repository.createSession() }
                if (!sessionResult.isSuccess) throw Exception(sessionResult.exceptionOrNull()?.message ?: "创建会话失败")
                val sessionId = sessionResult.getOrThrow().session_id

                // 2. 上传用户照片
                sendProgress(callbackName, "uploading", 20, "正在上传用户照片...")
                val userBitmap = withContext(Dispatchers.IO) { base64ToBitmap(userBase64) }
                val userResult = withContext(Dispatchers.IO) { repository.uploadBitmap(sessionId, "user", userBitmap) }
                if (!userResult.isSuccess) throw Exception(userResult.exceptionOrNull()?.message ?: "上传用户照片失败")

                // 3. 上传发色参考图
                sendProgress(callbackName, "uploading", 40, "正在上传发色参考图...")
                val colorBitmap = withContext(Dispatchers.IO) { base64ToBitmap(colorBase64) }
                val colorResult = withContext(Dispatchers.IO) { repository.uploadBitmap(sessionId, "hairstyle", colorBitmap) }
                if (!colorResult.isSuccess) throw Exception(colorResult.exceptionOrNull()?.message ?: "上传发色参考图失败")

                // 4. 启动 AI 换发色并轮询
                sendProgress(callbackName, "processing", 60, "AI 正在分析肤色特征...")

                var pollCount = 0
                repository.processColorAsync(sessionId).collect { status ->
                    when (status.status) {
                        "processing" -> {
                            pollCount++
                            val p = minOf(65 + pollCount * 5, 95)
                            val msg = when {
                                pollCount <= 3 -> "AI 正在分析肤色与发色匹配..."
                                pollCount <= 6 -> "AI 正在应用发色风格..."
                                else -> "AI 智能合成新发色中..."
                            }
                            sendProgress(callbackName, "processing", p, msg)
                        }
                        "completed" -> {
                            sendProgress(callbackName, "completed", 100, "换发色完成！", mapOf(
                                "session_id" to sessionId,
                                "result_urls" to (status.result_urls ?: emptyList<String>()),
                                "user_image_url" to (status.user_image_url ?: ""),
                                "hairstyle_image_url" to (status.hairstyle_image_url ?: "")
                            ))
                        }
                        "failed" -> {
                            sendProgress(callbackName, "failed", 0, "AI 处理失败")
                        }
                        "cancelled" -> {
                            sendProgress(callbackName, "failed", 0, "任务已取消")
                        }
                    }
                }
            } catch (e: Exception) {
                sendProgress(callbackName, "failed", 0, e.message ?: "处理失败")
            }
        }
    }

    /**
     * 带额外数据的进度更新（最终结果用）
     */
    private fun sendProgress(callbackName: String, status: String, progress: Int, message: String, extra: Map<String, Any?>) {
        activity.runOnUiThread {
            val data = mutableMapOf<String, Any?>(
                "status" to status,
                "progress" to progress,
                "message" to message
            )
            data.putAll(extra)
            callJsCallback(callbackName, data)
        }
    }

    /**
     * Base64转Bitmap
     */
    private fun base64ToBitmap(base64: String): Bitmap {
        val cleanBase64 = if (base64.contains(",")) {
            base64.substringAfter(",")
        } else {
            base64
        }
        val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }
}
