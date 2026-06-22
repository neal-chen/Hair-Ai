package com.hairstyle.generator.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hairstyle.generator.data.db.converter.CacheConverters
import com.hairstyle.generator.data.db.dao.HairColorCacheDao
import com.hairstyle.generator.data.db.dao.HairstyleCacheDao
import com.hairstyle.generator.data.db.dao.SyncMetadataDao
import com.hairstyle.generator.data.db.entity.HairColorCacheEntity
import com.hairstyle.generator.data.db.entity.HairstyleCacheEntity
import com.hairstyle.generator.data.db.entity.SyncMetadataEntity

/**
 * Room 数据库
 * 用于缓存服务端的发型/发色数据，支持离线使用和增量同步
 */
@Database(
    entities = [
        HairstyleCacheEntity::class,
        HairColorCacheEntity::class,
        SyncMetadataEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(CacheConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun hairstyleCacheDao(): HairstyleCacheDao
    abstract fun hairColorCacheDao(): HairColorCacheDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        private const val DB_NAME = "hair_library_cache.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME,
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
