package com.hairstyle.generator.data.api

import com.hairstyle.generator.data.db.converter.HairColorSyncResponse
import com.hairstyle.generator.data.db.converter.HairstyleSyncResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 发型/发色库 API 服务（增量同步端点）
 */
interface LibraryApiService {

    /**
     * 获取发型库列表（增量同步）
     */
    @GET("api/hairstyles")
    suspend fun getHairstyles(
        @Query("version") version: Long = 0,
        @Query("device_id") deviceId: String,
        @Query("gender") gender: String? = null,
        @Query("category") category: String? = null,
    ): retrofit2.Response<HairstyleSyncResponse>

    /**
     * 获取发色库列表（增量同步）
     */
    @GET("api/hair-colors")
    suspend fun getHairColors(
        @Query("version") version: Long = 0,
        @Query("device_id") deviceId: String,
        @Query("category") category: String? = null,
    ): retrofit2.Response<HairColorSyncResponse>
}
