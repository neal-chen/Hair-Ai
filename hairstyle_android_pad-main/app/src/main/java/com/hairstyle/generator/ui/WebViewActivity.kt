package com.hairstyle.generator.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.hairstyle.generator.bridge.AndroidBridge
import com.hairstyle.generator.databinding.ActivityWebviewBinding
import com.hairstyle.generator.utils.ImageUtils
import java.io.File

/**
 * WebView Activity - 加载Web前端页面
 */
class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebviewBinding
    private lateinit var androidBridge: AndroidBridge

    // 文件选择回调
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // 相机拍照临时文件
    private var cameraPhotoUri: Uri? = null

    // 当前等待的权限回调
    private var pendingPermissionCallback: String? = null

    // 标记是否从设置页面返回
    private var isFromSettings = false

    // 文件选择器的相机Uri（用于onShowFileChooser触发的相机）
    private var fileChooserCameraUri: Uri? = null

    // Activity Result Launchers
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraPhotoUri != null) {
            androidBridge.handleCameraResult(cameraPhotoUri!!)
        } else {
            androidBridge.handleCameraError("拍照取消或失败")
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            androidBridge.handleGalleryResult(uri)
        } else {
            androidBridge.handleGalleryError("选择图片取消")
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        androidBridge.handlePermissionResult(pendingPermissionCallback, allGranted)
        pendingPermissionCallback = null
    }

    // 用于文件选择器的相机启动器（使用自定义CameraActivity）
    private val fileChooserCameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                // 加载图片、裁剪为1:1正方形
                val bitmap = ImageUtils.decodeSampledBitmapFromUri(this, uri, 1024, 1024)
                if (bitmap != null) {
                    val croppedBitmap = ImageUtils.cropToSquare(bitmap)
                    val croppedFile = ImageUtils.bitmapToFile(this, croppedBitmap, "cropped_${System.currentTimeMillis()}.jpg")
                    val croppedUri = FileProvider.getUriForFile(this, "${packageName}.provider", croppedFile)
                    filePathCallback?.onReceiveValue(arrayOf(croppedUri))
                } else {
                    filePathCallback?.onReceiveValue(arrayOf(uri))
                }
            } else {
                filePathCallback?.onReceiveValue(null)
            }
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    // 用于AndroidBridge拍照的启动器（使用自定义CameraActivity，带倒计时和裁剪）
    private val bridgeCameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                androidBridge.handleCameraResult(uri)
            } else {
                androidBridge.handleCameraError("获取照片失败")
            }
        } else {
            androidBridge.handleCameraError("拍照取消")
        }
    }

    // 用于文件选择器的相册启动器
    private val fileChooserGalleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            filePathCallback?.onReceiveValue(arrayOf(uri))
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        loadWebContent()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                // 启用JavaScript
                javaScriptEnabled = true

                // 启用DOM Storage
                domStorageEnabled = true

                // 允许文件访问
                allowFileAccess = true
                allowContentAccess = true

                // 允许从file://协议访问网络资源（用于加载远程图片）
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = true

                // 允许混合内容（HTTP和HTTPS）
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                // 缓存设置
                cacheMode = WebSettings.LOAD_DEFAULT

                // 关键：useWideViewPort = true, loadWithOverviewMode = false
                // loadWithOverviewMode 必须设为 false，否则会覆盖 setInitialScale
                useWideViewPort = true
                loadWithOverviewMode = false

                // 禁止用户缩放
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false

                // 媒体自动播放
                mediaPlaybackRequiresUserGesture = false

                // 默认编码
                defaultTextEncodingName = "UTF-8"
            }

            // 设置WebViewClient处理页面加载
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false
                    // 本地assets URL不拦截
                    if (url.startsWith("file:///android_asset/")) {
                        return false
                    }
                    // 其他URL也在WebView中加载
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                }
            }

            // 设置WebChromeClient处理JS对话框等
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    binding.progressBar.progress = newProgress
                }

                // 处理文件选择（用于HTML input file）
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    this@WebViewActivity.filePathCallback?.onReceiveValue(null)
                    this@WebViewActivity.filePathCallback = filePathCallback
                    // 显示选择对话框：拍照或从相册选择
                    showImageSourceDialog()
                    return true
                }
            }

            // 添加JavaScript Bridge
            androidBridge = AndroidBridge(this@WebViewActivity, this)
            addJavascriptInterface(androidBridge, "Android")
        }
    }

    private fun loadWebContent() {
        // 直接加载上传照片页面，跳过网页欢迎页
        binding.webView.loadUrl("file:///android_asset/web/home.html")
    }

    /**
     * 直接启动相机拍照
     */
    private fun showImageSourceDialog() {
        // 直接检查权限并启动相机
        checkCameraPermissionAndLaunch()
    }

    /**
     * 检查相机权限并启动相机
     */
    private fun checkCameraPermissionAndLaunch() {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
            launchFileChooserCamera()
        } else {
            // 检查是否需要显示权限说明
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.CAMERA
            )

            if (shouldShowRationale) {
                showCameraPermissionRationaleDialog()
            } else {
                // 首次请求或用户选择了"不再询问"
                val hasRequestedBefore = getSharedPreferences("permissions", MODE_PRIVATE)
                    .getBoolean("camera_requested", false)

                if (!hasRequestedBefore) {
                    // 首次请求
                    getSharedPreferences("permissions", MODE_PRIVATE)
                        .edit()
                        .putBoolean("camera_requested", true)
                        .apply()
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_REQUEST_CODE
                    )
                } else {
                    // 用户选择了"不再询问"，引导去设置
                    showGoToSettingsDialog()
                }
            }
        }
    }

    /**
     * 显示相机权限说明对话框
     */
    private fun showCameraPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要相机权限")
            .setMessage("拍照功能需要使用相机权限，请授予权限以正常使用。")
            .setPositiveButton("授予权限") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("取消") { _, _ ->
                filePathCallback?.onReceiveValue(null)
                filePathCallback = null
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 显示引导去设置页面的对话框
     */
    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要相机权限")
            .setMessage("您之前拒绝了相机权限，请在设置中手动开启相机权限以使用拍照功能。")
            .setPositiveButton("去设置") { _, _ ->
                isFromSettings = true
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("取消") { _, _ ->
                filePathCallback?.onReceiveValue(null)
                filePathCallback = null
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 启动文件选择器相机（使用自定义CameraActivity，带倒计时）
     */
    private fun launchFileChooserCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        fileChooserCameraLauncher.launch(intent)
    }

    override fun onResume() {
        super.onResume()
        // 从设置页面返回后，重新检查权限
        if (isFromSettings) {
            isFromSettings = false
            val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
                launchFileChooserCamera()
            } else {
                Toast.makeText(this, "相机权限未开启，无法使用拍照功能", Toast.LENGTH_LONG).show()
                filePathCallback?.onReceiveValue(null)
                filePathCallback = null
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchFileChooserCamera()
            } else {
                // 检查用户是否选择了"不再询问"
                val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.CAMERA
                )

                if (shouldShowRationale) {
                    showCameraPermissionRationaleDialog()
                } else {
                    showGoToSettingsDialog()
                }
            }
        }
    }

    /**
     * 打开图片选择器（保留兼容性）
     */
    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "选择图片"), FILE_CHOOSER_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            val result = if (resultCode == RESULT_OK && data?.data != null) {
                arrayOf(data.data!!)
            } else {
                null
            }
            filePathCallback?.onReceiveValue(result)
            filePathCallback = null
        }
    }

    /**
     * 启动相机拍照（使用自定义CameraActivity，带倒计时功能）
     */
    fun launchCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        bridgeCameraLauncher.launch(intent)
    }

    /**
     * 启动相册选择
     */
    fun launchGallery() {
        galleryLauncher.launch("image/*")
    }

    /**
     * 请求相机权限
     */
    fun requestCameraPermission(callbackName: String) {
        pendingPermissionCallback = callbackName

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        permissionLauncher.launch(permissions)
    }

    /**
     * 检查相机权限
     */
    fun hasCameraPermission(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                storagePermission == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 创建图片临时文件
     */
    private fun createImageFile(): File {
        val fileName = "IMG_${System.currentTimeMillis()}"
        val storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDir)
    }

    /**
     * 执行JavaScript代码
     */
    fun evaluateJavascript(script: String) {
        runOnUiThread {
            binding.webView.evaluateJavascript(script, null)
        }
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        binding.webView.apply {
            stopLoading()
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }

    companion object {
        private const val FILE_CHOOSER_REQUEST_CODE = 1001
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1002
    }
}
