package com.hairstyle.generator.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.hairstyle.generator.R
import com.hairstyle.generator.config.HairColorImageConfig
import com.hairstyle.generator.config.HairstyleImageConfig
import com.hairstyle.generator.data.model.HairColorTemplate
import com.hairstyle.generator.data.model.HairstyleTemplate
import com.hairstyle.generator.data.repository.HairstyleRepository
import com.hairstyle.generator.databinding.ActivityPhotoUploadBinding
import com.hairstyle.generator.utils.ImageUtils
import kotlinx.coroutines.launch

/**
 * 照片上传页面Activity
 */
class PhotoUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoUploadBinding
    private lateinit var repository: HairstyleRepository

    private var currentSessionId: String? = null
    private var userPhotoUri: Uri? = null
    private var hairstylePhotoUri: Uri? = null
    private var hasUserPhoto = false
    private var hasHairstylePhoto = false

    // Activity Result Launchers
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var qrScanLauncher: ActivityResultLauncher<Intent>
    private lateinit var hairstyleLibraryLauncher: ActivityResultLauncher<Intent>
    private lateinit var hairColorLibraryLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRepository()
        initActivityLaunchers()
        setupUI()
        createSession()
    }

    private fun setupRepository() {
        repository = HairstyleRepository(this)
    }

    private fun initActivityLaunchers() {
        // 相机拍照启动器
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    handleCameraResult(uri)
                } else {
                    showError("获取照片失败，请重试")
                }
            }
        }

        // 图片选择启动器
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                handleImageSelected(it)
            }
        }

        // 二维码上传启动器
        qrScanLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val sessionId = result.data?.getStringExtra("session_id")
                val uploadType = result.data?.getStringExtra("upload_type")
                val success = result.data?.getBooleanExtra("success", false) ?: false

                if (success && sessionId != null && uploadType != null) {
                    handleQRUploadSuccess(sessionId, uploadType)
                }
            }
        }

        // 发型库选择启动器
        hairstyleLibraryLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedHairstyle = result.data?.getParcelableExtra<HairstyleTemplate>(HairstyleLibraryActivity.EXTRA_SELECTED_HAIRSTYLE)
                selectedHairstyle?.let { handleHairstyleSelected(it) }
            }
        }

        // 发色库选择启动器
        hairColorLibraryLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedColor = result.data?.getParcelableExtra<HairColorTemplate>(HairColorLibraryActivity.EXTRA_SELECTED_COLOR)
                selectedColor?.let { handleHairColorSelected(it) }
            }
        }
    }

    private fun setupUI() {
        // 返回按钮
        binding.btnBack.setOnClickListener { finish() }

        // 用户照片相关按钮
        binding.btnTakePhoto.setOnClickListener { openCamera() }
        binding.btnScanUserPhoto.setOnClickListener { openQRScanner("user") }
        binding.btnRetakePhoto.setOnClickListener { resetUserPhoto() }
        binding.btnRescanUserPhoto.setOnClickListener { openQRScanner("user") }

        // 发型参考图相关按钮
        binding.btnSelectFromLibrary.setOnClickListener { openHairstyleLibrary() }
        binding.btnScanHairstyle.setOnClickListener { openQRScanner("hairstyle") }
        binding.btnReselect.setOnClickListener { openHairstyleLibrary() }
        binding.btnRescanHairstyle.setOnClickListener { openQRScanner("hairstyle") }

        // 生成按钮
        binding.btnGenerate.setOnClickListener { generateHairstyle() }

        // 换发色按钮
        binding.btnChangeColor.setOnClickListener { changeHairColor() }

        // 历史记录按钮
        binding.btnHistory.setOnClickListener { openHistory() }

        updateGenerateButton()
    }

    /**
     * 创建会话
     */
    private fun createSession() {
        lifecycleScope.launch {
            try {
                val result = repository.createSession()
                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    currentSessionId = response.session_id
                } else {
                    showError("创建会话失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showError("网络错误: ${e.message}")
            }
        }
    }

    /**
     * 打开相机
     */
    private fun openCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        cameraLauncher.launch(intent)
    }

    /**
     * 处理相机拍照结果
     */
    private fun handleCameraResult(uri: Uri) {
        lifecycleScope.launch {
            try {
                val sessionId = currentSessionId
                if (sessionId == null) {
                    showError("会话未创建，请稍后重试")
                    return@launch
                }

                // 加载图片、裁剪为1:1正方形
                val bitmap = ImageUtils.decodeSampledBitmapFromUri(this@PhotoUploadActivity, uri, 1024, 1024)
                if (bitmap == null) {
                    showError("加载图片失败")
                    return@launch
                }
                val croppedBitmap = ImageUtils.cropToSquare(bitmap)

                // 上传裁剪后的图片
                val result = repository.uploadBitmap(sessionId, "user", croppedBitmap)

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    if (response.success) {
                        hasUserPhoto = true
                        showUserPhotoPreview(croppedBitmap)
                        updateGenerateButton()
                        Toast.makeText(this@PhotoUploadActivity, "照片上传成功", Toast.LENGTH_SHORT).show()
                    } else {
                        showError("上传失败: ${response.error}")
                    }
                } else {
                    showError("上传失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showError("处理图片失败: ${e.message}")
            }
        }
    }

    /**
     * 处理图片选择结果
     */
    private fun handleImageSelected(uri: Uri) {
        lifecycleScope.launch {
            try {
                val sessionId = currentSessionId
                if (sessionId == null) {
                    showError("会话未创建，请稍后重试")
                    return@launch
                }

                // 加载图片、裁剪为1:1正方形
                val bitmap = ImageUtils.decodeSampledBitmapFromUri(this@PhotoUploadActivity, uri, 1024, 1024)
                if (bitmap == null) {
                    showError("加载图片失败")
                    return@launch
                }
                val croppedBitmap = ImageUtils.cropToSquare(bitmap)

                // 显示预览
                userPhotoUri = uri
                showUserPhotoPreview(croppedBitmap)

                // 上传裁剪后的图片
                val result = repository.uploadBitmap(sessionId, "user", croppedBitmap)

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    if (response.success) {
                        hasUserPhoto = true
                        updateGenerateButton()
                        Toast.makeText(this@PhotoUploadActivity, "照片上传成功", Toast.LENGTH_SHORT).show()
                    } else {
                        showError("上传失败: ${response.error}")
                    }
                } else {
                    showError("上传失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showError("上传失败: ${e.message}")
            }
        }
    }

    /**
     * 上传用户照片
     */
    private fun uploadUserPhoto(uri: Uri) {
        lifecycleScope.launch {
            try {
                val sessionId = currentSessionId ?: return@launch
                val result = repository.uploadImage(sessionId, "user", uri)

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    if (response.success) {
                        hasUserPhoto = true
                        updateGenerateButton()
                        Toast.makeText(this@PhotoUploadActivity, "照片上传成功", Toast.LENGTH_SHORT).show()
                    } else {
                        showError("上传失败: ${response.error}")
                    }
                } else {
                    showError("上传失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showError("上传失败: ${e.message}")
            }
        }
    }

    /**
     * 显示用户照片预览
     */
    private fun showUserPhotoPreview(bitmap: Bitmap) {
        binding.userUploadArea.visibility = View.GONE
        binding.userPhotoPreview.visibility = View.VISIBLE
        binding.ivUserPhoto.setImageBitmap(bitmap)
    }

    /**
     * 显示用户照片预览
     */
    private fun showUserPhotoPreview(uri: Uri) {
        binding.userUploadArea.visibility = View.GONE
        binding.userPhotoPreview.visibility = View.VISIBLE
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.ivUserPhoto)
    }

    /**
     * 重置用户照片
     */
    private fun resetUserPhoto() {
        binding.userUploadArea.visibility = View.VISIBLE
        binding.userPhotoPreview.visibility = View.GONE
        hasUserPhoto = false
        userPhotoUri = null
        updateGenerateButton()

        // 通知服务器重置
        currentSessionId?.let { sessionId ->
            lifecycleScope.launch {
                repository.resetImage(sessionId, "user")
            }
        }
    }

    /**
     * 打开二维码上传
     */
    private fun openQRScanner(type: String) {
        val intent = Intent(this, QRUploadActivity::class.java)
        intent.putExtra("upload_type", type)
        intent.putExtra("session_id", currentSessionId) // 传递当前会话ID
        qrScanLauncher.launch(intent)
    }

    /**
     * 处理二维码上传成功
     */
    private fun handleQRUploadSuccess(sessionId: String, uploadType: String) {
        // 总是更新当前会话ID以确保一致性
        currentSessionId = sessionId

        when (uploadType) {
            "user" -> {
                hasUserPhoto = true
                showUserPhotoUploaded(sessionId)
            }
            "hairstyle" -> {
                hasHairstylePhoto = true
                showHairstyleUploaded(sessionId)
            }
        }

        updateGenerateButton()
        Toast.makeText(this, "图片上传成功", Toast.LENGTH_SHORT).show()
    }

    /**
     * 显示用户照片已上传状态
     */
    private fun showUserPhotoUploaded(sessionId: String) {
        binding.userUploadArea.visibility = View.GONE
        binding.userPhotoPreview.visibility = View.VISIBLE

        // 先获取会话状态以获取正确的图片URL
        lifecycleScope.launch {
            try {
                val result = repository.getSessionStatus(sessionId)
                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    val imageUrl = response.user_image_url

                    if (!imageUrl.isNullOrEmpty()) {
                        // 确保URL使用HTTPS（Railway强制HTTPS）
                        val httpsUrl = imageUrl.replace("http://", "https://")

                        Glide.with(this@PhotoUploadActivity)
                            .load(httpsUrl)
                            .placeholder(R.drawable.ic_camera)
                            .error(R.drawable.ic_camera)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                            .into(binding.ivUserPhoto)
                    } else {
                        // 如果没有image_url，使用默认URL（确保HTTPS）
                        val fallbackUrl = "${repository.getBaseUrl()}/api/image/$sessionId/user"
                        val httpsUrl = fallbackUrl.replace("http://", "https://")
                        Glide.with(this@PhotoUploadActivity)
                            .load(httpsUrl)
                            .placeholder(R.drawable.ic_camera)
                            .error(R.drawable.ic_camera)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                            .into(binding.ivUserPhoto)
                    }
                } else {
                    // 如果获取会话状态失败，使用默认URL（确保HTTPS）
                    val imageUrl = "${repository.getBaseUrl()}/api/image/$sessionId/user"
                    val httpsUrl = imageUrl.replace("http://", "https://")
                    Glide.with(this@PhotoUploadActivity)
                        .load(httpsUrl)
                        .placeholder(R.drawable.ic_camera)
                        .error(R.drawable.ic_camera)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                        .into(binding.ivUserPhoto)
                }
            } catch (e: Exception) {
                // 出错时使用默认URL（确保HTTPS）
                val imageUrl = "${repository.getBaseUrl()}/api/image/$sessionId/user"
                val httpsUrl = imageUrl.replace("http://", "https://")
                Glide.with(this@PhotoUploadActivity)
                    .load(httpsUrl)
                    .placeholder(R.drawable.ic_camera)
                    .error(R.drawable.ic_camera)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .into(binding.ivUserPhoto)
            }
        }
    }

    /**
     * 显示发型图片已上传状态
     */
    private fun showHairstyleUploaded(sessionId: String) {
        binding.hairstyleSelectArea.visibility = View.GONE
        binding.hairstylePreview.visibility = View.VISIBLE

        // 先获取会话状态以获取正确的图片URL
        lifecycleScope.launch {
            try {
                val result = repository.getSessionStatus(sessionId)
                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    val imageUrl = response.hairstyle_image_url

                    if (!imageUrl.isNullOrEmpty()) {
                        // 确保URL使用HTTPS（Railway强制HTTPS）
                        val httpsUrl = imageUrl.replace("http://", "https://")
                        Glide.with(this@PhotoUploadActivity)
                            .load(httpsUrl)
                            .placeholder(R.drawable.ic_scissors)
                            .error(R.drawable.ic_scissors)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                            .into(binding.ivHairstyle)
                    } else {
                        // 如果没有image_url，使用默认URL（确保HTTPS）
                        val fallbackUrl = "${repository.getBaseUrl()}/api/image/$sessionId/hairstyle"
                        val httpsUrl = fallbackUrl.replace("http://", "https://")
                        Glide.with(this@PhotoUploadActivity)
                            .load(httpsUrl)
                            .placeholder(R.drawable.ic_scissors)
                            .error(R.drawable.ic_scissors)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                            .into(binding.ivHairstyle)
                    }
                } else {
                    // 如果获取会话状态失败，使用默认URL（确保HTTPS）
                    val imageUrl = "${repository.getBaseUrl()}/api/image/$sessionId/hairstyle"
                    val httpsUrl = imageUrl.replace("http://", "https://")
                    Glide.with(this@PhotoUploadActivity)
                        .load(httpsUrl)
                        .placeholder(R.drawable.ic_scissors)
                        .error(R.drawable.ic_scissors)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                        .into(binding.ivHairstyle)
                }
            } catch (e: Exception) {
                // 出错时使用默认URL（确保HTTPS）
                val imageUrl = "${repository.getBaseUrl()}/api/image/$sessionId/hairstyle"
                val httpsUrl = imageUrl.replace("http://", "https://")
                Glide.with(this@PhotoUploadActivity)
                    .load(httpsUrl)
                    .placeholder(R.drawable.ic_scissors)
                    .error(R.drawable.ic_scissors)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .into(binding.ivHairstyle)
            }
        }
    }

    /**
     * 处理二维码扫描结果（保留兼容性）
     */
    private fun handleQRResult(qrResult: String) {
        // TODO: 处理二维码扫描结果
        // 可能是图片URL或上传链接
        Toast.makeText(this, "扫描结果: $qrResult", Toast.LENGTH_SHORT).show()
    }

    /**
     * 打开发型库
     */
    private fun openHairstyleLibrary() {
        val intent = Intent(this, HairstyleLibraryActivity::class.java)
        hairstyleLibraryLauncher.launch(intent)
    }

    /**
     * 处理发型选择结果
     */
    private fun handleHairstyleSelected(hairstyle: HairstyleTemplate) {
        val imagePath = HairstyleImageConfig.buildImagePath(
            hairstyle.category,
            hairstyle.name,
            hairstyle.description,
            hairstyle.gender
        )
        showHairstylePreview(imagePath)
        uploadHairstyleFromAssets(hairstyle)
    }

    /**
     * 从Assets上传发型图片
     */
    private fun uploadHairstyleFromAssets(hairstyle: HairstyleTemplate) {
        lifecycleScope.launch {
            try {
                val sessionId = currentSessionId ?: return@launch

                // 从Assets加载图片转换为Bitmap
                val fileName = "${hairstyle.category}_${hairstyle.name}_${hairstyle.description}.png"
                val folderName = if (hairstyle.gender == "男") "hairstyles_man" else "hairstyles_woman"
                val inputStream = assets.open("$folderName/$fileName")
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                // 上传Bitmap到服务器
                val result = repository.uploadBitmap(sessionId, "hairstyle", bitmap)

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    if (response.success) {
                        hasHairstylePhoto = true
                        updateGenerateButton()
                        Toast.makeText(this@PhotoUploadActivity, "发型选择成功", Toast.LENGTH_SHORT).show()
                    } else {
                        showError("上传失败: ${response.error}")
                    }
                } else {
                    showError("上传失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showError("选择发型失败: ${e.message}")
            }
        }
    }

    /**
     * 显示发型预览
     */
    private fun showHairstylePreview(imagePath: String) {
        binding.hairstyleSelectArea.visibility = View.GONE
        binding.hairstylePreview.visibility = View.VISIBLE
        Glide.with(this)
            .load(imagePath)
            .fitCenter() // 使用fitCenter保证完整显示
            .placeholder(R.drawable.ic_scissors)
            .error(R.drawable.ic_scissors)
            .into(binding.ivHairstyle)
    }

    /**
     * 生成发型
     */
    private fun generateHairstyle() {
        if (!hasUserPhoto || !hasHairstylePhoto) {
            showError("请先上传用户照片和选择发型参考图")
            return
        }

        val sessionId = currentSessionId ?: return

        // 先获取会话状态获取图片URL，然后启动ResultsActivity
        lifecycleScope.launch {
            try {
                val result = repository.getSessionStatus(sessionId)
                if (result.isSuccess) {
                    val response = result.getOrThrow()

                    val intent = Intent(this@PhotoUploadActivity, ResultsActivity::class.java)
                    intent.putExtra("session_id", sessionId)
                    intent.putExtra("user_image_url", response.user_image_url)
                    intent.putExtra("hairstyle_image_url", response.hairstyle_image_url)
                    intent.putExtra("process_type", "hairstyle")  // 标记处理类型
                    startActivity(intent)
                } else {
                    showError("获取会话信息失败")
                }
            } catch (e: Exception) {
                showError("获取会话信息失败: ${e.message}")
            }
        }
    }

    /**
     * 换发色 - 打开发色库选择发色
     */
    private fun changeHairColor() {
        if (!hasUserPhoto) {
            showError("请先上传用户照片")
            return
        }
        openHairColorLibrary()
    }

    /**
     * 打开发色库
     */
    private fun openHairColorLibrary() {
        val intent = Intent(this, HairColorLibraryActivity::class.java)
        hairColorLibraryLauncher.launch(intent)
    }

    /**
     * 处理发色选择结果
     */
    private fun handleHairColorSelected(hairColor: HairColorTemplate) {
        val imagePath = HairColorImageConfig.buildImagePath(
            hairColor.category,
            hairColor.name
        )
        showHairstylePreview(imagePath)
        uploadHairColorFromAssets(hairColor)
    }

    /**
     * 从Assets上传发色图片并启动换发色处理
     */
    private fun uploadHairColorFromAssets(hairColor: HairColorTemplate) {
        lifecycleScope.launch {
            try {
                val sessionId = currentSessionId ?: return@launch

                // 从Assets加载发色图片
                val fileName = "${hairColor.category}_${hairColor.name}.png"
                val inputStream = assets.open("hair_colors/$fileName")
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                // 上传发色图片到服务器作为参考图
                val result = repository.uploadBitmap(sessionId, "hairstyle", bitmap)

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    if (response.success) {
                        hasHairstylePhoto = true
                        updateGenerateButton()
                        Toast.makeText(this@PhotoUploadActivity, "发色选择成功", Toast.LENGTH_SHORT).show()

                        // 获取会话状态并启动换发色处理
                        val statusResult = repository.getSessionStatus(sessionId)
                        if (statusResult.isSuccess) {
                            val statusResponse = statusResult.getOrThrow()
                            val intent = Intent(this@PhotoUploadActivity, ResultsActivity::class.java)
                            intent.putExtra("session_id", sessionId)
                            intent.putExtra("user_image_url", statusResponse.user_image_url)
                            intent.putExtra("hairstyle_image_url", statusResponse.hairstyle_image_url)
                            intent.putExtra("process_type", "color")  // 标记处理类型为换发色
                            startActivity(intent)
                        } else {
                            showError("获取会话信息失败")
                        }
                    } else {
                        showError("上传失败: ${response.error}")
                    }
                } else {
                    showError("上传失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showError("选择发色失败: ${e.message}")
            }
        }
    }

    /**
     * 打开历史记录
     */
    private fun openHistory() {
        val intent = Intent(this, HistoryActivity::class.java)
        startActivity(intent)
    }

    /**
     * 更新生成按钮状态
     */
    private fun updateGenerateButton() {
        val canGenerate = hasUserPhoto && hasHairstylePhoto
        binding.btnGenerate.isEnabled = canGenerate
        binding.btnGenerate.alpha = if (canGenerate) 1.0f else 0.5f

        // 换发色按钮也需要同样的条件
        binding.btnChangeColor.isEnabled = canGenerate
        binding.btnChangeColor.alpha = if (canGenerate) 1.0f else 0.5f
    }

    /**
     * 显示错误信息
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // 直接finish()，让系统自然回退到栈中的WelcomeActivity
    }
}