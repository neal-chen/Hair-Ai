package com.hairstyle.generator.data.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络配置类
 */
object NetworkConfig {

    // Railway服务器基础URL
    private const val PRODUCTION_URL = "https://web-production-bingli.up.railway.app/"

    // 本地测试URL
    private const val LOCAL_URL = "http://10.0.2.2:5000/"

    // 服务器模式配置
    enum class ServerMode {
        PRODUCTION,  // 生产环境 - Railway HTTPS
        LOCAL       // 本地开发 - HTTP
    }

    // 当前服务器模式 - 可根据需要切换
    private val currentMode = ServerMode.PRODUCTION

    val baseUrl: String
        get() = when (currentMode) {
            ServerMode.PRODUCTION -> PRODUCTION_URL.removeSuffix("/")
            ServerMode.LOCAL -> LOCAL_URL.removeSuffix("/")
        }

    val isHttps: Boolean
        get() = currentMode == ServerMode.PRODUCTION

    /**
     * OkHttpClient配置
     */
    private fun createOkHttpClient(): OkHttpClient {
        val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val builder = OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // 普通API请求
            .writeTimeout(120, TimeUnit.SECONDS) // 图片上传需要更长时间

        // 根据服务器模式配置SSL
        when (currentMode) {
            ServerMode.PRODUCTION -> {
                // Railway提供标准SSL证书，使用默认验证
                builder.hostnameVerifier { hostname, _ ->
                    hostname == "web-production-bingli.up.railway.app"
                }
            }
            ServerMode.LOCAL -> {
                // 本地开发环境，无需SSL配置
            }
        }

        return builder.build()
    }

    /**
     * 创建长时间超时的OkHttpClient（用于AI生成）
     */
    private fun createLongTimeoutOkHttpClient(): OkHttpClient {
        val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val builder = OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.MINUTES) // AI生成最多需要10分钟，设置12分钟
            .writeTimeout(120, TimeUnit.SECONDS)

        // 根据服务器模式配置SSL
        when (currentMode) {
            ServerMode.PRODUCTION -> {
                builder.hostnameVerifier { hostname, _ ->
                    hostname == "web-production-bingli.up.railway.app"
                }
            }
            ServerMode.LOCAL -> {
                // 本地开发环境，无需SSL配置
            }
        }

        return builder.build()
    }

    /**
     * Retrofit配置
     */
    private fun createRetrofit(): Retrofit {
        val gson = GsonBuilder()
            .setLenient()
            .create()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * 创建长时间超时的Retrofit（用于AI生成）
     */
    private fun createLongTimeoutRetrofit(): Retrofit {
        val gson = GsonBuilder()
            .setLenient()
            .create()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createLongTimeoutOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * API服务实例
     */
    val apiService: HairstyleApiService by lazy {
        createRetrofit().create(HairstyleApiService::class.java)
    }

    /**
     * AI生成专用API服务实例（长时间超时）
     */
    val aiGenerationApiService: HairstyleApiService by lazy {
        createLongTimeoutRetrofit().create(HairstyleApiService::class.java)
    }

    /**
     * 发型/发色库同步 API 实例
     */
    val libraryApiService: LibraryApiService by lazy {
        createRetrofit().create(LibraryApiService::class.java)
    }
}