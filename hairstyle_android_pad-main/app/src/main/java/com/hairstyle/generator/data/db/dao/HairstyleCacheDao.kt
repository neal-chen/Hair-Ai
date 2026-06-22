package com.hairstyle.generator.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hairstyle.generator.data.db.entity.HairstyleCacheEntity
import kotlinx.coroutines.flow.Flow

/**
 * 发型缓存 DAO
 */
@Dao
interface HairstyleCacheDao {

    @Query("SELECT * FROM hairstyle_cache WHERE gender = :gender AND isActive = 1 ORDER BY sortOrder ASC")
    fun getByGender(gender: String): Flow<List<HairstyleCacheEntity>>

    @Query("SELECT * FROM hairstyle_cache WHERE gender = :gender AND category = :category AND isActive = 1 ORDER BY sortOrder ASC")
    fun getByCategory(gender: String, category: String): Flow<List<HairstyleCacheEntity>>

    @Query("SELECT * FROM hairstyle_cache WHERE isActive = 1 AND (name LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%')")
    fun search(q: String): Flow<List<HairstyleCacheEntity>>

    @Query("SELECT DISTINCT category FROM hairstyle_cache WHERE gender = :gender AND isActive = 1 ORDER BY category")
    fun getCategories(gender: String): Flow<List<String>>

    @Query("SELECT MAX(version) FROM hairstyle_cache")
    suspend fun getMaxVersion(): Long?

    @Query("SELECT * FROM hairstyle_cache WHERE isActive = 1 ORDER BY sortOrder ASC")
    fun getAllActive(): Flow<List<HairstyleCacheEntity>>

    @Upsert
    suspend fun upsertAll(items: List<HairstyleCacheEntity>)

    @Query("DELETE FROM hairstyle_cache WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM hairstyle_cache WHERE id NOT IN (:activeIds)")
    suspend fun deleteNotIn(activeIds: List<String>)
}
