package com.hairstyle.generator.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.zxing.Result
import com.hairstyle.generator.R
import com.hairstyle.generator.databinding.ActivityQrScanBinding
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult

/**
 * 二维码扫描Activity
 */
class QRScanActivity : AppCompatActivity(), BarcodeCallback {

    private lateinit var binding: ActivityQrScanBinding
    private var scanType: String? = null
    private var isFlashOn = false
    private var hasScanned = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScanning()
        } else {
            Toast.makeText(this, "需要相机权限才能扫描二维码", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getScanType()
        setupUI()
        checkPermissionAndStartScanning()
    }

    private fun getScanType() {
        scanType = intent.getStringExtra("scan_type")
        updateTitle()
    }

    private fun updateTitle() {
        val title = when (scanType) {
            "user" -> "扫描上传用户照片"
            "hairstyle" -> "扫描上传发型参考图"
            else -> "扫描二维码"
        }
        binding.tvTitle.text = title
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnFlash.setOnClickListener { toggleFlash() }
        binding.btnManualInput.setOnClickListener { showManualInputDialog() }
    }

    /**
     * 检查权限并开始扫描
     */
    private fun checkPermissionAndStartScanning() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startScanning()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * 开始扫描
     */
    private fun startScanning() {
        binding.barcodeScanner.decodeContinuous(this)
        binding.barcodeScanner.resume()
    }

    /**
     * 切换闪光灯
     */
    private fun toggleFlash() {
        isFlashOn = !isFlashOn
        if (isFlashOn) {
            binding.barcodeScanner.setTorchOn()
            binding.btnFlash.setImageResource(R.drawable.ic_flash_on)
        } else {
            binding.barcodeScanner.setTorchOff()
            binding.btnFlash.setImageResource(R.drawable.ic_flash_off)
        }
    }

    /**
     * 显示手动输入对话框
     */
    private fun showManualInputDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = "请输入二维码内容或图片链接"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("手动输入")
            .setMessage("请输入二维码内容或图片上传链接")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val content = editText.text.toString().trim()
                if (content.isNotEmpty()) {
                    handleScanResult(content)
                } else {
                    Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 扫描回调
     */
    override fun barcodeResult(result: BarcodeResult?) {
        if (!hasScanned && result != null) {
            hasScanned = true
            binding.barcodeScanner.pause()

            val content = result.text
            handleScanResult(content)
        }
    }

    override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
        // 可能的结果点，用于UI显示
    }

    /**
     * 处理扫描结果
     */
    private fun handleScanResult(content: String) {
        try {
            // 判断扫描内容类型
            when {
                content.startsWith("http://") || content.startsWith("https://") -> {
                    // 网络链接
                    if (isImageUrl(content)) {
                        returnImageResult(content)
                    } else if (isUploadPageUrl(content)) {
                        // 是上传页面链接，打开浏览器或WebView
                        openUploadPage(content)
                    } else {
                        showResultDialog("扫描到链接", content)
                    }
                }
                content.contains("upload") && content.contains("session") -> {
                    // 可能是服务器生成的上传链接
                    openUploadPage(content)
                }
                else -> {
                    // 其他类型内容
                    showResultDialog("扫描结果", content)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "处理扫描结果失败", Toast.LENGTH_SHORT).show()
            resetScanning()
        }
    }

    /**
     * 判断是否为图片URL
     */
    private fun isImageUrl(url: String): Boolean {
        val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp")
        return imageExtensions.any { url.lowercase().contains(it) }
    }

    /**
     * 判断是否为上传页面URL
     */
    private fun isUploadPageUrl(url: String): Boolean {
        return url.contains("/upload/") || url.contains("upload")
    }

    /**
     * 打开上传页面
     */
    private fun openUploadPage(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)

            Toast.makeText(this, "已打开上传页面，请在浏览器中上传图片", Toast.LENGTH_LONG).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show()
            resetScanning()
        }
    }

    /**
     * 返回图片结果
     */
    private fun returnImageResult(imageUrl: String) {
        val intent = Intent()
        intent.putExtra("qr_result", imageUrl)
        intent.putExtra("result_type", "image_url")
        setResult(RESULT_OK, intent)
        finish()
    }

    /**
     * 显示结果对话框
     */
    private fun showResultDialog(title: String, content: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton("使用此结果") { _, _ ->
                val intent = Intent()
                intent.putExtra("qr_result", content)
                intent.putExtra("result_type", "text")
                setResult(RESULT_OK, intent)
                finish()
            }
            .setNegativeButton("重新扫描") { _, _ ->
                resetScanning()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 重置扫描状态
     */
    private fun resetScanning() {
        hasScanned = false
        binding.barcodeScanner.resume()
    }

    override fun onResume() {
        super.onResume()
        if (!hasScanned) {
            binding.barcodeScanner.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScanner.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.barcodeScanner.pause()
    }
}