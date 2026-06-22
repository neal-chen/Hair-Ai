package com.hairstyle.generator.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hairstyle.generator.data.db.entity.HairColorCacheEntity
import kotlinx.coroutines.flow.Flow

/**
 * 发色缓存 DAO
 */
@Dao
interface HairColorCacheDao {

    @Query("SELECT * FROM hair_color_cache WHERE isActive = 1 ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<HairColorCacheEntity>>

    @Query("SELECT * FROM hair_color_cache WHERE category = :category AND isActive = 1 ORDER BY sortOrder ASC")
    fun getByCategory(category: String): Flow<List<HairColorCacheEntity>>

    @Query("SELECT DISTINCT category FROM hair_color_cache WHERE isActive = 1 ORDER BY category")
    fun getCategories(): Flow<List<String>>

    @Query("SELECT MAX(version) FROM hair_color_cache")
    suspend fun getMaxVersion(): Long?

    @Upsert
    suspend fun upsertAll(items: List<HairColorCacheEntity>)

    @Query("DELETE FROM hair_color_cache WHERE id = :id")
    suspend fun deleteById(id: String)
}
