package com.hairstyle.generator.config

/**
 * 发色图片配置类
 * 用于管理发色图片的加载方式
 */
object HairColorImageConfig {

    // 图片加载模式
    enum class ImageLoadMode {
        ASSETS,    // 从App Assets加载
        REMOTE     // 从远程服务器加载
    }

    // 当前加载模式 - 默认使用Assets
    var currentMode = ImageLoadMode.ASSETS

    // 远程服务器配置
    const val REMOTE_BASE_URL = "http://139.224.222.90:8003/hair_colors/"
    const val IMAGE_EXTENSION = ".png"

    /**
     * 根据当前模式构建图片路径
     */
    fun buildImagePath(category: String, name: String): String {
        val fileName = "${category}_${name}${IMAGE_EXTENSION}"

        return when (currentMode) {
            ImageLoadMode.ASSETS -> "file:///android_asset/hair_colors/$fileName"
            ImageLoadMode.REMOTE -> "$REMOTE_BASE_URL$fileName"
        }
    }

    /**
     * 切换到远程加载模式
     */
    fun useRemoteImages() {
        currentMode = ImageLoadMode.REMOTE
    }

    /**
     * 切换到Assets加载模式
     */
    fun useAssetsImages() {
        currentMode = ImageLoadMode.ASSETS
    }
}
