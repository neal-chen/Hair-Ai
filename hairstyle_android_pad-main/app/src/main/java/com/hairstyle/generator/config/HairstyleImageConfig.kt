package com.hairstyle.generator.config

/**
 * 发型图片配置类
 * 用于管理发型图片的加载方式
 */
object HairstyleImageConfig {

    // 图片加载模式
    enum class ImageLoadMode {
        ASSETS,    // 从App Assets加载
        REMOTE,    // 从远程服务器加载
        LOCAL      // 从本地存储加载
    }

    // 当前加载模式 - 默认使用Assets
    var currentMode = ImageLoadMode.ASSETS

    // 远程服务器配置
    const val REMOTE_BASE_URL = "http://139.224.222.90:8003/std_hairstyle/"
    const val IMAGE_EXTENSION = ".png"

    /**
     * 根据当前模式构建图片路径
     */
    fun buildImagePath(category: String, name: String, description: String, gender: String = "女"): String {
        val fileName = "${category}_${name}_${description}${IMAGE_EXTENSION}"
        val folderName = if (gender == "男") "hairstyles_man" else "hairstyles_woman"

        return when (currentMode) {
            ImageLoadMode.ASSETS -> "file:///android_asset/$folderName/$fileName"
            ImageLoadMode.REMOTE -> "$REMOTE_BASE_URL$folderName/$fileName"
            ImageLoadMode.LOCAL -> "file:///sdcard/$folderName/$fileName"
        }
    }

    /**
     * 切换到远程加载模式
     */
    fun useRemoteImages(baseUrl: String = REMOTE_BASE_URL) {
        currentMode = ImageLoadMode.REMOTE
        // 可以动态设置远程URL
    }

    /**
     * 切换到Assets加载模式
     */
    fun useAssetsImages() {
        currentMode = ImageLoadMode.ASSETS
    }
}