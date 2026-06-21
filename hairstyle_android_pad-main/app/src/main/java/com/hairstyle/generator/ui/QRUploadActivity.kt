package com.hairstyle.generator.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.hairstyle.generator.data.repository.HairstyleRepository
import com.hairstyle.generator.databinding.ActivityQruploadBinding
import com.hairstyle.generator.utils.DeviceUtils
import kotlinx.coroutines.launch

/**
 * 二维码上传页面
 */
class QRUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQruploadBinding
    private lateinit var repository: HairstyleRepository
    private var sessionId: String? = null
    private var uploadType: String = "user" // "user" or "hairstyle"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQruploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取传递的参数
        uploadType = intent.getStringExtra("upload_type") ?: "user"
        sessionId = intent.getStringExtra("session_id") // 获取传递的会话ID

        setupUI()
        setupRepository()

        if (sessionId != null) {
            // 使用已有会话ID生成二维码
            generateQRCodeForSession()
        } else {
            // 如果没有会话ID，创建新会话
            createSession()
        }
    }

    private fun setupUI() {
        // 设置标题
        val title = if (uploadType == "user") "扫码上传您的照片" else "扫码上传参考发型"
        binding.titleText.text = title

        val description = if (uploadType == "user") {
            "请使用手机扫描下方二维码上传您的照片"
        } else {
            "请使用手机扫描下方二维码上传参考发型图片"
        }
        binding.descriptionText.text = description

        // 返回按钮
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 刷新按钮
        binding.btnRefresh.setOnClickListener {
            createSession()
        }
    }

    private fun setupRepository() {
        repository = HairstyleRepository(this)
    }

    /**
     * 使用已有会话ID生成二维码
     */
    private fun generateQRCodeForSession() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.qrCodeImage.visibility = android.view.View.GONE
        binding.statusText.text = "正在生成二维码..."

        lifecycleScope.launch {
            try {
                val id = sessionId!!
                val result = repository.getSessionStatus(id)

                if (result.isSuccess) {
                    val response = result.getOrThrow()

                    // 根据上传类型选择对应的URL - 需要构造上传URL
                    val baseUrl = repository.getBaseUrl()
                    val uploadUrl = if (uploadType == "user") {
                        "$baseUrl/upload/$id/user"
                    } else {
                        "$baseUrl/upload/$id/hairstyle"
                    }

                    // 生成二维码
                    generateQRCode(uploadUrl)
                } else {
                    showError("获取会话状态失败")
                }
            } catch (e: Exception) {
                showError("网络错误：${e.message}")
            }
        }
    }

    /**
     * 创建上传会话并生成二维码
     */
    private fun createSession() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.qrCodeImage.visibility = android.view.View.GONE
        binding.statusText.text = "正在生成二维码..."

        lifecycleScope.launch {
            try {
                val deviceId = DeviceUtils.getDeviceId(this@QRUploadActivity)
                val result = repository.createUploadSession(deviceId)

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    sessionId = response.session_id

                    // 使用后端返回的URL
                    val uploadUrl = if (uploadType == "user") {
                        response.user_upload_url
                    } else {
                        response.hairstyle_upload_url
                    }

                    // 生成二维码
                    generateQRCode(uploadUrl)
                } else {
                    showError("创建上传会话失败")
                }
            } catch (e: Exception) {
                showError("网络错误：${e.message}")
            }
        }
    }

    /**
     * 生成二维码
     */
    private fun generateQRCode(url: String) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, 512, 512)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            binding.progressBar.visibility = android.view.View.GONE
            binding.qrCodeImage.visibility = android.view.View.VISIBLE
            binding.qrCodeImage.setImageBitmap(bitmap)
            binding.statusText.text = "扫描二维码上传图片"

            // 开始检查上传状态
            checkUploadStatus()

        } catch (e: Exception) {
            showError("生成二维码失败：${e.message}")
        }
    }

    /**
     * 检查上传状态
     */
    private fun checkUploadStatus() {
        sessionId?.let { id ->
            lifecycleScope.launch {
                // 每2秒检查一次上传状态
                while (!isFinishing) {
                    try {
                        val result = repository.getUploadSession(id)
                        if (result.isSuccess) {
                            val response = result.getOrThrow()

                            // 检查对应类型的图片是否已上传
                            val isUploaded = if (uploadType == "user") {
                                response.has_user_image || response.user_image_uploaded == true
                            } else {
                                response.has_hairstyle_image || response.hairstyle_image_uploaded == true
                            }

                            if (isUploaded) {
                                binding.statusText.text = "✓ 图片上传成功！"

                                // 返回结果给调用页面
                                val intent = android.content.Intent()
                                intent.putExtra("session_id", id)
                                intent.putExtra("upload_type", uploadType)
                                intent.putExtra("success", true)
                                setResult(RESULT_OK, intent)

                                Toast.makeText(this@QRUploadActivity, "图片上传成功", Toast.LENGTH_SHORT).show()
                                finish()
                                return@launch
                            } else {
                                binding.statusText.text = "等待上传中..."
                            }
                        }

                        kotlinx.coroutines.delay(2000) // 等待2秒后再次检查

                    } catch (e: Exception) {
                        // 忽略检查错误，继续尝试
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = android.view.View.GONE
        binding.statusText.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}