// data/local/AppDatabase.kt
package com.example.t_learnappmobile.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.t_learnappmobile.data.local.dao.WordDao
import com.example.t_learnappmobile.data.local.entities.*

@Database(
    entities = [WordEntity::class, UserWordEntity::class, DictionaryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "t_learn_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}