package com.hairstyle.generator.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hairstyle.generator.R
import com.hairstyle.generator.data.repository.HairstyleRepository
import com.hairstyle.generator.databinding.ActivityWelcomeBinding
import com.hairstyle.generator.utils.DeviceUtils
import kotlinx.coroutines.launch

/**
 * 欢迎页面Activity
 */
class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var repository: HairstyleRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRepository()
        checkSubscriptionStatus()
    }

    private fun setupUI() {
        binding.btnGetStarted.setOnClickListener {
            // 跳转到WebView页面
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }

        // 添加一些动画效果可以在这里实现
        animateWelcomeScreen()
    }

    private fun setupRepository() {
        repository = HairstyleRepository(this)
    }

    private fun animateWelcomeScreen() {
        // 简单的淡入动画
        binding.root.alpha = 0f
        binding.root.animate()
            .alpha(1f)
            .setDuration(800)
            .start()
    }

    /**
     * 检查订阅状态
     */
    private fun checkSubscriptionStatus() {
        val deviceId = DeviceUtils.getDeviceId(this)

        if (DeviceUtils.shouldCheckSubscription(this)) {
            lifecycleScope.launch {
                try {
                    val result = repository.checkSubscription(deviceId)
                    if (result.isSuccess) {
                        val response = result.getOrThrow()
                        if (response.success) {
                            // 订阅有效，保存信息
                            DeviceUtils.saveActivationInfo(
                                this@WelcomeActivity,
                                response.subscription_type ?: "",
                                response.expires_at ?: "",
                                response.days_remaining ?: 0
                            )
                        } else {
                            // 需要激活或续费
                            if (response.requires_activation == true) {
                                showActivationDialog()
                            } else if (response.requires_renewal == true) {
                                showRenewalDialog()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // 网络错误或服务器问题，使用本地缓存信息
                    handleSubscriptionCheckError(e)
                }
            }
        }
    }

    /**
     * 显示激活对话框
     */
    private fun showActivationDialog() {
        // TODO: 实现激活对话框
        // 这里可以显示一个对话框提示用户输入激活码
    }

    /**
     * 显示续费对话框
     */
    private fun showRenewalDialog() {
        // TODO: 实现续费对话框
        // 这里可以显示一个对话框提示用户订阅已过期
    }

    /**
     * 处理订阅检查错误
     */
    private fun handleSubscriptionCheckError(error: Exception) {
        // 记录错误日志
        error.printStackTrace()

        // 检查本地是否有有效的激活信息
        val activationInfo = DeviceUtils.getActivationInfo(this)
        val subscriptionType = activationInfo["subscription_type"] as? String

        if (subscriptionType.isNullOrEmpty()) {
            // 本地没有激活信息，提示用户激活
            showActivationDialog()
        }
    }

    /**
     * 导航到照片上传页面
     */
    private fun navigateToPhotoUpload() {
        // 检查是否已激活
        val activationInfo = DeviceUtils.getActivationInfo(this)
        val subscriptionType = activationInfo["subscription_type"] as? String

        if (!subscriptionType.isNullOrEmpty()) {
            // 已激活，直接进入主功能
            val intent = Intent(this, PhotoUploadActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // 未激活，显示激活界面
            showActivationDialog()
        }
    }

    override fun onBackPressed() {
        // 在欢迎页面按返回键直接退出应用
        super.onBackPressed()
        finish()
    }
}