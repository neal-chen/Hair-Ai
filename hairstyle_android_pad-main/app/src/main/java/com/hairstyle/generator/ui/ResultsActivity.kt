package com.hairstyle.generator.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.hairstyle.generator.R
import com.hairstyle.generator.data.repository.HairstyleRepository
import com.hairstyle.generator.databinding.ActivityResultsBinding
import com.hairstyle.generator.ui.adapter.LoadingAdapter
import com.hairstyle.generator.ui.adapter.ResultsAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 结果展示页面Activity
 */
class ResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultsBinding
    private lateinit var repository: HairstyleRepository

    private var sessionId: String? = null
    private var userImageUrl: String? = null
    private var hairstyleImageUrl: String? = null
    private var resultUrls: List<String> = emptyList()
    private var isGenerating: Boolean = false
    private var processType: String = "hairstyle"  // 默认为发型生成，可以是 "hairstyle" 或 "color"

    private lateinit var loadingAdapter: LoadingAdapter
    private var resultsAdapter: ResultsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRepository()
        getIntentData()
        setupUI()

        // 只有非历史记录模式才需要启动生成流程
        if (!intent.getBooleanExtra("history_mode", false)) {
            loadSessionDataAndStartGeneration()
        }
    }

    private fun setupRepository() {
        repository = HairstyleRepository(this)
    }

    private fun getIntentData() {
        val isHistoryMode = intent.getBooleanExtra("history_mode", false)
        Log.d("ResultsActivity", "getIntentData - isHistoryMode: $isHistoryMode")

        if (isHistoryMode) {
            // 历史记录模式
            userImageUrl = intent.getStringExtra("user_image_url")
            hairstyleImageUrl = intent.getStringExtra("hairstyle_image_url")
            val resultArray = intent.getStringArrayExtra("result_urls")
            resultUrls = resultArray?.toList() ?: emptyList()

            Log.d("ResultsActivity", "History mode - userImageUrl: '$userImageUrl'")

            // 直接加载图片和显示结果
            loadOriginalImages()
            if (resultUrls.isNotEmpty()) {
                showResults()
            }
        } else {
            // 正常生成模式
            sessionId = intent.getStringExtra("session_id")
            userImageUrl = intent.getStringExtra("user_image_url")
            hairstyleImageUrl = intent.getStringExtra("hairstyle_image_url")
            processType = intent.getStringExtra("process_type") ?: "hairstyle"

            Log.d("ResultsActivity", "Generation mode - sessionId: $sessionId, userImageUrl: '$userImageUrl'")

            if (sessionId == null) {
                showError("无效的会话ID")
                finish()
                return
            }

            // 直接加载Intent传入的图片URL
            loadOriginalImages()
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { onBackPressed() }

        val isHistoryMode = intent.getBooleanExtra("history_mode", false)
        if (isHistoryMode) {
            // 历史记录模式下隐藏重新生成按钮
            binding.btnRegenerate.visibility = View.GONE
            binding.btnRegenerateBottom.visibility = View.GONE
            binding.btnBackEdit.visibility = View.GONE
        } else {
            binding.btnRegenerate.setOnClickListener { regenerateHairstyle() }
            binding.btnRegenerateBottom.setOnClickListener { regenerateHairstyle() }
            binding.btnBackEdit.setOnClickListener { backToEdit() }
        }

        // 取消生成按钮
        binding.btnCancelGeneration.setOnClickListener { cancelGeneration() }

        // 选中图片后的三个操作按钮
        binding.btnRegisterSave.setOnClickListener { onRegisterSaveClick() }
        binding.btnChangeColor.setOnClickListener { onChangeColorClick() }
        binding.btn3DView.setOnClickListener { on3DViewClick() }

        setupRecyclerViews()
    }

    /**
     * 注册并保存按钮点击
     */
    private fun onRegisterSaveClick() {
        val selectedUrl = resultsAdapter?.getSelectedImageUrl()
        if (selectedUrl != null) {
            onRegisterSaveClick(selectedUrl)
        } else {
            Toast.makeText(this, "请先选择一张图片", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 注册并保存按钮点击（带URL参数）
     */
    private fun onRegisterSaveClick(imageUrl: String) {
        // TODO: 实现注册并保存功能
        Toast.makeText(this, "注册并保存功能开发中", Toast.LENGTH_SHORT).show()
    }

    /**
     * 换发色按钮点击
     */
    private fun onChangeColorClick() {
        val selectedUrl = resultsAdapter?.getSelectedImageUrl()
        if (selectedUrl != null) {
            onChangeColorClick(selectedUrl)
        } else {
            Toast.makeText(this, "请先选择一张图片", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 换发色按钮点击（带URL参数）
     */
    private fun onChangeColorClick(imageUrl: String) {
        // TODO: 跳转到换发色页面，传递选中的图片
        Toast.makeText(this, "换发色功能开发中", Toast.LENGTH_SHORT).show()
    }

    /**
     * 3D展示按钮点击
     */
    private fun on3DViewClick() {
        val selectedUrl = resultsAdapter?.getSelectedImageUrl()
        if (selectedUrl != null) {
            on3DViewClick(selectedUrl)
        } else {
            Toast.makeText(this, "请先选择一张图片", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 3D展示按钮点击（带URL参数）
     */
    private fun on3DViewClick(imageUrl: String) {
        // TODO: 跳转到3D展示页面
        Toast.makeText(this, "3D展示功能开发中", Toast.LENGTH_SHORT).show()
    }

    private fun setupRecyclerViews() {
        // 设置加载状态RecyclerView
        loadingAdapter = LoadingAdapter()
        binding.rvLoadingCards.layoutManager = GridLayoutManager(this, 2)
        binding.rvLoadingCards.adapter = loadingAdapter

        // 设置结果RecyclerView
        binding.rvResults.layoutManager = GridLayoutManager(this, 2)
    }

    /**
     * 加载会话数据并开始生成
     */
    private fun loadSessionDataAndStartGeneration() {
        lifecycleScope.launch {
            try {
                val sessionId = sessionId ?: return@launch

                // 如果没有图片URL，先获取会话状态
                if (userImageUrl == null || hairstyleImageUrl == null) {
                    val statusResult = repository.getSessionStatus(sessionId)
                    if (statusResult.isSuccess) {
                        val status = statusResult.getOrThrow()
                        if (userImageUrl == null) {
                            userImageUrl = status.user_image_url
                        }
                        if (hairstyleImageUrl == null) {
                            hairstyleImageUrl = status.hairstyle_image_url
                        }
                        runOnUiThread {
                            loadOriginalImages()
                        }
                    }
                }

                startGeneration()
            } catch (e: Exception) {
                runOnUiThread {
                    showError("加载会话数据失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 加载原始图片
     */
    private fun loadOriginalImages() {
        userImageUrl?.let { url ->
            Glide.with(this@ResultsActivity)
                .load(url.replace("http://", "https://"))
                .placeholder(R.drawable.ic_camera)
                .error(R.drawable.ic_camera)
                .into(binding.ivOriginalPhoto)
        }

        hairstyleImageUrl?.let { url ->
            Glide.with(this@ResultsActivity)
                .load(url.replace("http://", "https://"))
                .placeholder(R.drawable.ic_scissors)
                .error(R.drawable.ic_scissors)
                .into(binding.ivReferenceHairstyle)
        }
    }

    /**
     * 开始生成过程（在现有协程中调用）
     */
    private suspend fun startGeneration() {
        runOnUiThread {
            showLoadingState()
        }

        try {
            val sessionId = sessionId ?: return

            val processFlow = if (processType == "color") {
                repository.processColorAsync(sessionId)
            } else {
                repository.processHairstyleAsync(sessionId)
            }

            processFlow.collect { status ->
                when (status.status) {
                    "completed" -> {
                        // 任务完成
                        if (status.result_urls != null && status.result_urls.isNotEmpty()) {
                            resultUrls = status.result_urls

                            // 保存到历史记录
                            userImageUrl?.let { userUrl ->
                                hairstyleImageUrl?.let { hairstyleUrl ->
                                    repository.saveGenerationHistory(
                                        sessionId = sessionId,
                                        userImageUrl = userUrl,
                                        hairstyleImageUrl = hairstyleUrl,
                                        resultUrls = status.result_urls
                                    )
                                }
                            }

                            runOnUiThread {
                                showResults()
                            }
                        } else {
                            runOnUiThread {
                                isGenerating = false
                                showError("生成完成但没有结果图片")
                            }
                        }
                    }
                    "failed" -> {
                        runOnUiThread {
                            isGenerating = false
                            showError("生成失败")
                        }
                    }
                    "cancelled" -> {
                        runOnUiThread {
                            isGenerating = false
                            showError("任务已被取消")
                        }
                    }
                    "processing" -> {
                        // 继续等待，不需要特殊处理
                    }
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                isGenerating = false
                showError("网络错误: ${e.message}")
            }
        }
    }

    /**
     * 显示加载状态
     */
    private fun showLoadingState() {
        isGenerating = true
        val statusText = if (processType == "color") "AI智能换发色中..." else getString(R.string.ai_generating)
        binding.tvGenerationStatus.text = statusText
        binding.loadingLayout.visibility = View.VISIBLE
        binding.rvResults.visibility = View.GONE
        binding.bottomActionsLayout.visibility = View.GONE

        // 启动步骤动画
        startProcessingSteps()
    }

    /**
     * 启动处理步骤动画
     */
    private fun startProcessingSteps() {
        // 重置所有步骤状态
        resetAllSteps()

        // 开始第一步
        lifecycleScope.launch {
            if (processType == "color") {
                // 换发色步骤
                showStep(1, "正在分析用户肤色...")
                val step1Duration = (15000..30000).random()
                delay(step1Duration.toLong())
                completeStep(1)

                if (isGenerating) {
                    showStep(2, "正在分析发色风格...")
                    val step2Duration = (15000..30000).random()
                    delay(step2Duration.toLong())
                    completeStep(2)

                    if (isGenerating) {
                        showStep(3, "正在智能合成新发色...")
                    }
                }
            } else {
                // 发型生成步骤
                showStep(1, "正在分析用户脸型...")
                val step1Duration = (15000..30000).random()
                delay(step1Duration.toLong())
                completeStep(1)

                if (isGenerating) {
                    showStep(2, "正在分析发型风格...")
                    val step2Duration = (15000..30000).random()
                    delay(step2Duration.toLong())
                    completeStep(2)

                    if (isGenerating) {
                        showStep(3, "正在智能合成新发型...")
                    }
                }
            }
        }
    }

    /**
     * 重置所有步骤状态
     */
    private fun resetAllSteps() {
        // 步骤1
        binding.step1Progress.visibility = View.GONE
        binding.step1Check.visibility = View.GONE
        binding.tvStep1.alpha = 0.5f

        // 步骤2
        binding.step2Progress.visibility = View.GONE
        binding.step2Check.visibility = View.GONE
        binding.tvStep2.alpha = 0.5f

        // 步骤3
        binding.step3Progress.visibility = View.GONE
        binding.step3Check.visibility = View.GONE
        binding.tvStep3.alpha = 0.5f
    }

    /**
     * 显示指定步骤
     */
    private fun showStep(step: Int, message: String) {
        when (step) {
            1 -> {
                binding.tvStep1.text = message
                binding.tvStep1.alpha = 1.0f
                binding.step1Progress.visibility = View.VISIBLE
                binding.step1Check.visibility = View.GONE
            }
            2 -> {
                binding.tvStep2.text = message
                binding.tvStep2.alpha = 1.0f
                binding.step2Progress.visibility = View.VISIBLE
                binding.step2Check.visibility = View.GONE
            }
            3 -> {
                binding.tvStep3.text = message
                binding.tvStep3.alpha = 1.0f
                binding.step3Progress.visibility = View.VISIBLE
                binding.step3Check.visibility = View.GONE
            }
        }
    }

    /**
     * 完成指定步骤
     */
    private fun completeStep(step: Int) {
        when (step) {
            1 -> {
                binding.step1Progress.visibility = View.GONE
                binding.step1Check.visibility = View.VISIBLE
                binding.tvStep1.text = if (processType == "color") "用户肤色分析完成" else "用户脸型分析完成"
            }
            2 -> {
                binding.step2Progress.visibility = View.GONE
                binding.step2Check.visibility = View.VISIBLE
                binding.tvStep2.text = if (processType == "color") "发色风格分析完成" else "发型风格分析完成"
            }
            3 -> {
                binding.step3Progress.visibility = View.GONE
                binding.step3Check.visibility = View.VISIBLE
                binding.tvStep3.text = if (processType == "color") "新发色合成完成" else "新发型合成完成"
            }
        }
    }

    /**
     * 显示生成结果
     */
    private fun showResults() {
        isGenerating = false

        // 完成第三步
        completeStep(3)

        // 等待一下再隐藏加载界面，让用户看到最后一步完成
        lifecycleScope.launch {
            delay(1000)
            runOnUiThread {
                val completeText = if (processType == "color") "换发色完成！" else getString(R.string.generation_complete)
                binding.tvGenerationStatus.text = completeText
                binding.loadingLayout.visibility = View.GONE
                binding.rvResults.visibility = View.VISIBLE
                binding.bottomActionsLayout.visibility = View.VISIBLE
            }
        }

        // 确保原始图片也重新加载
        loadOriginalImages()

        // 记录传递给 ResultsAdapter 的 userImageUrl
        Log.d("ResultsActivity", "Creating ResultsAdapter:")
        Log.d("ResultsActivity", "  userImageUrl: '$userImageUrl'")
        Log.d("ResultsActivity", "  resultUrls count: ${resultUrls.size}")

        resultsAdapter = ResultsAdapter(
            results = resultUrls, // 直接使用服务端返回的URLs
            userImageUrl = userImageUrl ?: "",
            onResultClick = { position ->
                // 当点击选中图片时，显示三个操作按钮
                updateSelectedActionsVisibility()
            },
            onRegisterSaveClick = { imageUrl ->
                onRegisterSaveClick(imageUrl)
            },
            onChangeColorClick = { imageUrl ->
                onChangeColorClick(imageUrl)
            },
            on3DViewClick = { imageUrl ->
                on3DViewClick(imageUrl)
            }
        )
        binding.rvResults.adapter = resultsAdapter
    }

    /**
     * 更新选中操作按钮的可见性
     */
    private fun updateSelectedActionsVisibility() {
        val hasSelection = resultsAdapter?.getSelectedPosition() != -1
        binding.selectedActionsLayout.visibility = if (hasSelection) View.VISIBLE else View.GONE
    }

    /**
     * 重新生成
     */
    private fun regenerateHairstyle() {
        resultUrls = emptyList()
        lifecycleScope.launch {
            startGeneration()
        }
    }

    /**
     * 返回编辑页面
     */
    private fun backToEdit() {
        // 直接finish()，返回到栈中的PhotoUploadActivity
        finish()
    }

    /**
     * 显示错误信息
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * 取消生成
     */
    private fun cancelGeneration() {
        val currentSessionId = sessionId
        if (!isGenerating || currentSessionId == null) {
            // 没有正在进行的任务，直接返回
            backToEdit()
            return
        }

        // 立即停止动画
        isGenerating = false

        lifecycleScope.launch {
            try {
                val result = repository.cancelSession(currentSessionId)
                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    runOnUiThread {
                        Toast.makeText(this@ResultsActivity, "任务已取消", Toast.LENGTH_SHORT).show()
                        backToEdit()
                    }
                } else {
                    runOnUiThread {
                        val error = result.exceptionOrNull()?.message ?: "取消失败"
                        Toast.makeText(this@ResultsActivity, "取消失败: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ResultsActivity, "取消失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        // 如果有正在进行的任务，先取消任务
        if (isGenerating) {
            cancelGeneration()
        } else {
            backToEdit()
        }
    }
}