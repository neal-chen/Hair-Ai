package com.hairstyle.generator.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hairstyle.generator.databinding.ActivityCameraBinding
import com.hairstyle.generator.utils.ImageUtils
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 相机拍照Activity
 */
class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraExecutor: ExecutorService
    private var isFromSettings = false  // 标记是否从设置页面返回
    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null
    private var countdownValue = 0

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    returnBitmapResult(bitmap)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "选择图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        cameraExecutor = Executors.newSingleThreadExecutor()
        // 相机启动移到 onResume() 中
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCapture.setOnClickListener { takePhoto() }
        binding.btnSwitchCamera.setOnClickListener { switchCamera() }
        binding.btnGallery.setOnClickListener { openGallery() }
    }

    /**
     * 检查所有权限是否已授权
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 请求权限
     */
    private fun requestPermissions() {
        // 检查是否需要显示权限说明（用户之前拒绝过但没有选择"不再询问"）
        val shouldShowRationale = REQUIRED_PERMISSIONS.any {
            ActivityCompat.shouldShowRequestPermissionRationale(this, it)
        }

        // 检查是否是首次请求（之前从未请求过）
        val isFirstTimeRequest = REQUIRED_PERMISSIONS.all {
            !ActivityCompat.shouldShowRequestPermissionRationale(this, it) &&
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        when {
            shouldShowRationale -> {
                // 用户之前拒绝过，显示说明对话框
                showPermissionRationaleDialog()
            }
            isFirstTimeRequest -> {
                // 首次请求，直接请求权限
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
            else -> {
                // 用户选择了"不再询问"，引导去设置页面
                showGoToSettingsDialog()
            }
        }
    }

    /**
     * 显示权限说明对话框
     */
    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要相机权限")
            .setMessage("拍照功能需要使用相机权限，请授予权限以正常使用。")
            .setPositiveButton("授予权限") { _, _ ->
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
            .setNegativeButton("取消") { _, _ ->
                finish()
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
                openAppSettings()
            }
            .setNegativeButton("取消") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 打开应用设置页面
     */
    private fun openAppSettings() {
        isFromSettings = true
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "=== onResume() ===")

        // 每次 onResume 都重新启动相机
        if (allPermissionsGranted()) {
            startCamera()
        } else if (!isFromSettings) {
            requestPermissions()
        } else {
            // 从设置页面返回后，权限仍未开启
            isFromSettings = false
            Toast.makeText(this, "相机权限未开启，无法使用拍照功能", Toast.LENGTH_LONG).show()
            finish()
        }

        // 重置标记
        if (isFromSettings) {
            isFromSettings = false
        }
    }

    /**
     * 在 Activity 暂停时释放相机资源
     */
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "=== onPause() ===")

        // 只取消倒计时，让 CameraX 自动管理相机生命周期
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
        binding.tvCountdown.visibility = View.GONE

        // 恢复按钮状态
        binding.btnCapture.isEnabled = true
        binding.btnSwitchCamera.isEnabled = true
        binding.btnGallery.isEnabled = true

        // 不手动调用 unbindAll()，让 CameraX 通过 bindToLifecycle 自动管理
    }

    /**
     * 启动相机
     */
    private fun startCamera() {
        Log.d(TAG, "=== startCamera() 开始 ===")

        // 不手动调用 unbindAll()，让 CameraX 自动管理

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "CameraProvider 获取成功")

                // 打印设备相机信息
                logCameraInfo()

                lensFacing = getAvailableCameraLensFacing()
                Log.d(TAG, "选择的摄像头方向: ${if (lensFacing == CameraSelector.LENS_FACING_BACK) "后置" else "前置"}")

                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "CameraProvider 获取失败", e)
                Toast.makeText(this, "相机初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 检测可用的摄像头，优先使用后置摄像头
     */
    private fun getAvailableCameraLensFacing(): Int {
        val cameraProvider = cameraProvider ?: return CameraSelector.LENS_FACING_BACK

        // 优先使用后置摄像头
        if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
            return CameraSelector.LENS_FACING_BACK
        }
        // 如果没有后置，使用前置
        if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            return CameraSelector.LENS_FACING_FRONT
        }

        // 如果 CameraX 的标准选择器无法识别（某些 Rockchip 设备），通过 Camera2 API 检测
        Log.d(TAG, "CameraX 标准选择器无法识别相机，尝试使用 Camera2 API")
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraIds = cameraManager.cameraIdList
            for (id in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                when (facing) {
                    android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK -> {
                        Log.d(TAG, "Camera2 API 检测到后置摄像头 ID=$id")
                        return CameraSelector.LENS_FACING_BACK
                    }
                    android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT -> {
                        Log.d(TAG, "Camera2 API 检测到前置摄像头 ID=$id")
                        return CameraSelector.LENS_FACING_FRONT
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Camera2 API 检测失败", e)
        }

        return CameraSelector.LENS_FACING_BACK
    }

    /**
     * 打印设备相机信息（调试用）
     */
    private fun logCameraInfo() {
        val cameraProvider = cameraProvider ?: return

        Log.d(TAG, "=== 设备相机信息 ===")
        Log.d(TAG, "是否有后置摄像头: ${cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)}")
        Log.d(TAG, "是否有前置摄像头: ${cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)}")

        // 获取可用的相机列表
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraIds = cameraManager.cameraIdList
            Log.d(TAG, "相机数量: ${cameraIds.size}")
            cameraIds.forEachIndexed { index, id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                val facingStr = when (facing) {
                    android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT -> "前置"
                    android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK -> "后置"
                    android.hardware.camera2.CameraCharacteristics.LENS_FACING_EXTERNAL -> "外置"
                    else -> "未知"
                }
                Log.d(TAG, "相机[$index] ID=$id, 方向=$facingStr")
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取相机信息失败", e)
        }
    }

    /**
     * 通过相机ID创建 CameraSelector
     * 用于处理非标准相机ID的设备（如 Rockchip 平台）
     */
    @androidx.camera.camera2.interop.ExperimentalCamera2Interop
    private fun createCameraSelectorById(): CameraSelector? {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraIds = cameraManager.cameraIdList

            if (cameraIds.isEmpty()) {
                Log.e(TAG, "没有可用的相机")
                return null
            }

            // 根据 lensFacing 优先选择对应方向的相机
            var targetCameraId: String? = null
            val targetLensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
            } else {
                android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT
            }

            for (id in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                if (facing == targetLensFacing) {
                    targetCameraId = id
                    break
                }
            }

            // 如果找不到目标方向的相机，使用第一个可用相机
            if (targetCameraId == null) {
                targetCameraId = cameraIds[0]
                Log.d(TAG, "未找到目标方向相机，使用第一个可用相机")
            }

            Log.d(TAG, "使用自定义相机选择器, cameraId=$targetCameraId")

            return CameraSelector.Builder()
                .addCameraFilter { cameraInfos ->
                    cameraInfos.filter { cameraInfo ->
                        Camera2CameraInfo.from(cameraInfo).cameraId == targetCameraId
                    }
                }
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "创建自定义 CameraSelector 失败", e)
            return null
        }
    }

    /**
     * 绑定相机用例
     */
    @androidx.camera.camera2.interop.ExperimentalCamera2Interop
    private fun bindCameraUseCases() {
        Log.d(TAG, "=== bindCameraUseCases() 开始 ===")

        val cameraProvider = cameraProvider
        if (cameraProvider == null) {
            Log.e(TAG, "cameraProvider 为 null")
            Toast.makeText(this, "相机未初始化", Toast.LENGTH_LONG).show()
            return
        }

        // 创建 CameraSelector，优先使用标准方式，失败则使用自定义选择器
        val cameraSelector: CameraSelector
        val hasStandardCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
            || cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)

        if (hasStandardCamera) {
            cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            Log.d(TAG, "使用标准 CameraSelector, lensFacing=$lensFacing")
        } else {
            // 标准选择器无法识别相机（如 Rockchip 平台的非标准相机ID）
            Log.d(TAG, "标准选择器无法识别相机，尝试使用自定义选择器")
            val customSelector = createCameraSelectorById()
            if (customSelector == null) {
                Log.e(TAG, "创建自定义选择器失败")
                Toast.makeText(this, "无法找到可用的相机", Toast.LENGTH_LONG).show()
                return
            }
            cameraSelector = customSelector
            Log.d(TAG, "使用自定义 CameraSelector")
        }

        // 预览用例 - 使用默认4:3比例
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
            .also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
        Log.d(TAG, "Preview 用例创建成功")

        // 图像捕获用例 - 使用默认4:3比例，拍照后裁剪为1:1
        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        Log.d(TAG, "ImageCapture 用例创建成功")

        try {
            Log.d(TAG, "执行 unbindAll()...")
            cameraProvider.unbindAll()
            Log.d(TAG, "unbindAll() 完成")

            Log.d(TAG, "执行 bindToLifecycle()...")
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )
            Log.d(TAG, "bindToLifecycle() 成功!")

            // 单摄像头设备隐藏切换按钮
            val hasMultipleCameras = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            binding.btnSwitchCamera.visibility = if (hasMultipleCameras) View.VISIBLE else View.GONE
        } catch (exc: Exception) {
            Log.e(TAG, "=== 相机绑定失败 ===")
            Log.e(TAG, "异常类型: ${exc.javaClass.simpleName}")
            Log.e(TAG, "异常信息: ${exc.message}")
            Log.e(TAG, "堆栈跟踪:", exc)

            // 显示详细错误信息
            val errorMsg = "相机启动失败\n类型: ${exc.javaClass.simpleName}\n详情: ${exc.message}"
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 拍照（带倒计时）
     */
    private fun takePhoto() {
        startCountdown()
    }

    /**
     * 开始倒计时（使用 Handler.postDelayed 实现）
     */
    private fun startCountdown() {
        binding.tvCountdown.visibility = View.VISIBLE
        countdownValue = 3
        binding.tvCountdown.text = countdownValue.toString()
        binding.btnCapture.isEnabled = false
        binding.btnSwitchCamera.isEnabled = false
        binding.btnGallery.isEnabled = false

        countdownRunnable = object : Runnable {
            override fun run() {
                countdownValue--
                if (countdownValue > 0) {
                    binding.tvCountdown.text = countdownValue.toString()
                    handler.postDelayed(this, 1000)
                } else {
                    binding.tvCountdown.visibility = View.GONE
                    capturePhoto()
                }
            }
        }
        handler.postDelayed(countdownRunnable!!, 1000)
    }

    /**
     * 执行拍照
     */
    private fun capturePhoto() {
        val imageCapture = imageCapture ?: run {
            showError("相机未就绪")
            resetCaptureState()
            return
        }

        showLoading(true)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            ImageUtils.createImageFile(this, "CAMERA")
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri
                    if (savedUri != null) {
                        try {
                            val bitmap = ImageUtils.loadBitmapFromFile(savedUri.path!!)
                            if (bitmap != null) {
                                val compressedBitmap = ImageUtils.compressBitmap(bitmap)
                                returnBitmapResult(compressedBitmap)
                            } else {
                                showError("处理图片失败")
                            }
                        } catch (e: Exception) {
                            showError("处理图片失败: ${e.message}")
                        }
                    } else {
                        showError("保存图片失败")
                    }
                    showLoading(false)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    showError("拍照失败: ${exception.message}")
                    showLoading(false)
                }
            }
        )
    }

    /**
     * 重置拍照状态
     */
    private fun resetCaptureState() {
        binding.tvCountdown.visibility = View.GONE
        binding.btnCapture.isEnabled = true
        binding.btnSwitchCamera.isEnabled = true
        binding.btnGallery.isEnabled = true
    }

    /**
     * 切换摄像头
     */
    private fun switchCamera() {
        val cameraProvider = cameraProvider ?: return
        val newLensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }

        // 检查目标摄像头是否存在
        val targetSelector = if (newLensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        if (cameraProvider.hasCamera(targetSelector)) {
            lensFacing = newLensFacing
            bindCameraUseCases()
        } else {
            Toast.makeText(this, "设备没有其他摄像头", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 打开图库
     */
    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    /**
     * 返回拍照结果（裁剪为1:1正方形）
     */
    private fun returnBitmapResult(bitmap: Bitmap) {
        try {
            // 先裁剪为1:1正方形
            val croppedBitmap = ImageUtils.cropToSquare(bitmap)
            // 保存裁剪后的bitmap到临时文件并返回URI
            val tempFile = ImageUtils.bitmapToFile(this, croppedBitmap, "captured_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                tempFile
            )

            val intent = Intent()
            intent.data = uri
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setResult(RESULT_OK, intent)

            // 不在这里释放相机，让 onDestroy 统一处理
            // 避免再次打开 CameraActivity 时出现 "Camera is closed" 错误

            finish()
        } catch (e: Exception) {
            showError("处理结果失败: ${e.message}")
        }
    }

    /**
     * 显示/隐藏加载指示器
     */
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnCapture.isEnabled = !show
        binding.btnSwitchCamera.isEnabled = !show
        binding.btnGallery.isEnabled = !show
    }

    /**
     * 显示错误信息
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * 手动释放相机资源
     * 用于解决 Rockchip 平台相机资源不能正确自动释放的问题
     */
    private fun releaseCamera() {
        try {
            Log.d(TAG, "=== releaseCamera() 手动释放相机 ===")
            cameraProvider?.unbindAll()
            imageCapture = null
            Log.d(TAG, "相机资源已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放相机失败: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // 检查用户是否选择了"不再询问"
                val shouldShowRationale = permissions.any {
                    ActivityCompat.shouldShowRequestPermissionRationale(this, it)
                }

                if (shouldShowRationale) {
                    // 用户只是拒绝了，没有选择"不再询问"，显示说明后再试
                    showPermissionRationaleDialog()
                } else {
                    // 用户选择了"不再询问"，引导去设置
                    showGoToSettingsDialog()
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "=== onDestroy() ===")

        // 取消倒计时
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null

        // 手动释放相机资源（确保 Rockchip 设备正确释放）
        releaseCamera()

        // 关闭执行器
        cameraExecutor.shutdown()

        super.onDestroy()
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}