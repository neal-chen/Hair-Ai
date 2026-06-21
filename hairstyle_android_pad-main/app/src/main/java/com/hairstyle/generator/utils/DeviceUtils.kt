package com.hairstyle.generator.utils

import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * 设备工具类
 */
object DeviceUtils {

    /**
     * 获取设备唯一ID
     */
    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        var deviceId = prefs.getString("device_id", null)

        if (deviceId == null) {
            deviceId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                UUID.randomUUID().toString()
            }

            // 保存到SharedPreferences
            prefs.edit().putString("device_id", deviceId).apply()
        }

        return deviceId ?: UUID.randomUUID().toString()
    }

    /**
     * 保存激活信息
     */
    fun saveActivationInfo(
        context: Context,
        subscriptionType: String,
        expiresAt: String,
        daysRemaining: Int
    ) {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("subscription_type", subscriptionType)
            putString("expires_at", expiresAt)
            putInt("days_remaining", daysRemaining)
            putLong("last_check", System.currentTimeMillis())
            apply()
        }
    }

    /**
     * 获取激活信息
     */
    fun getActivationInfo(context: Context): Map<String, Any?> {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        return mapOf(
            "subscription_type" to prefs.getString("subscription_type", null),
            "expires_at" to prefs.getString("expires_at", null),
            "days_remaining" to prefs.getInt("days_remaining", 0),
            "last_check" to prefs.getLong("last_check", 0)
        )
    }

    /**
     * 清除激活信息
     */
    fun clearActivationInfo(context: Context) {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            remove("subscription_type")
            remove("expires_at")
            remove("days_remaining")
            remove("last_check")
            apply()
        }
    }

    /**
     * 检查是否需要重新验证订阅
     */
    fun shouldCheckSubscription(context: Context): Boolean {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong("last_check", 0)
        val now = System.currentTimeMillis()
        val oneHour = 60 * 60 * 1000L

        return (now - lastCheck) > oneHour
    }
}