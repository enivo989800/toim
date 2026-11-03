package com.toimg.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.toimg.app.data.local.dao.FolderDao
import com.toimg.app.data.local.dao.ImageDao
import com.toimg.app.data.local.dao.MoveHistoryDao
import com.toimg.app.data.local.dao.RecognitionDao
import com.toimg.app.data.local.dao.SmartRuleDao
import com.toimg.app.data.local.entity.FolderEntity
import com.toimg.app.data.local.entity.ImageEntity
import com.toimg.app.data.local.entity.MoveHistoryEntity
import com.toimg.app.data.local.entity.RecognitionEntity
import com.toimg.app.data.local.entity.SmartRuleEntity

@Database(
    entities = [
        ImageEntity::class,
        FolderEntity::class,
        RecognitionEntity::class,
        MoveHistoryEntity::class,
        SmartRuleEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ToImgDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
    abstract fun folderDao(): FolderDao
    abstract fun recognitionDao(): RecognitionDao
    abstract fun moveHistoryDao(): MoveHistoryDao
    abstract fun smartRuleDao(): SmartRuleDao

    companion object {
        @Volatile
        private var INSTANCE: ToImgDatabase? = null

        fun getInstance(context: Context): ToImgDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ToImgDatabase::class.java,
                    "toimg_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
