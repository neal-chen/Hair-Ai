package com.hairstyle.generator.utils

import android.content.Context
import android.util.DisplayMetrics

/**
 * 屏幕适配工具类
 * 用于适配 1920x1080 和 3840x2160 两种分辨率
 */
object ScreenAdapter {

    // 基准屏幕宽度（44寸 1920x1080 竖屏）
    private const val BASE_SCREEN_WIDTH_DP = 600f  // 约44寸竖屏的dp宽度

    /**
     * 获取屏幕宽度缩放比例
     * 针对竖屏大屏幕设备优化
     * @return 缩放比例
     */
    fun getScaleFactor(context: Context): Float {
        val screenWidthDp = getScreenWidthDp(context)
        val screenHeightDp = getScreenHeightDp(context)

        // 对于竖屏设备，使用宽高中较小值作为参考
        val minDimension = minOf(screenWidthDp, screenHeightDp)

        // 计算缩放比例：以600dp为基准
        return (minDimension / BASE_SCREEN_WIDTH_DP).coerceIn(1f, 2.5f)
    }

    /**
     * 获取屏幕宽度（dp）
     */
    fun getScreenWidthDp(context: Context): Float {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.widthPixels / displayMetrics.density
    }

    /**
     * 获取屏幕高度（dp）
     */
    fun getScreenHeightDp(context: Context): Float {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.heightPixels / displayMetrics.density
    }

    /**
     * 缩放尺寸（dp 转 px）
     * @param baseValueDp 基准 dp 值（以 1920x1080 为准）
     * @return 缩放后的 px 值
     */
    fun scaleDimension(context: Context, baseValueDp: Float): Int {
        val scaleFactor = getScaleFactor(context)
        val scaledDp = baseValueDp * scaleFactor
        return dpToPx(context, scaledDp)
    }

    /**
     * 获取缩放后的字体大小
     * @param baseSp 基准 sp 值（以 1920x1080 为准）
     * @return 缩放后的 sp 值
     */
    fun getScaledTextSize(context: Context, baseSp: Float): Float {
        return baseSp * getScaleFactor(context)
    }

    /**
     * dp 转 px
     */
    fun dpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }

    /**
     * px 转 dp
     */
    fun pxToDp(context: Context, px: Int): Float {
        return px / context.resources.displayMetrics.density
    }

    /**
     * 判断是否为高分辨率屏幕（4K）
     */
    fun isHighResolutionScreen(context: Context): Boolean {
        return getScaleFactor(context) >= 1.5f
    }
}
