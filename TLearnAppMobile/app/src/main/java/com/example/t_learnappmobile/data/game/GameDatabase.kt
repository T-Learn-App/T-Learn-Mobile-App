package com.example.t_learnappmobile.data.game

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [GameResultEntity::class],
    version = 1,
    exportSchema = false  // ✅ ИСПРАВЛЕНО
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameResultDao(): GameResultDao
}

object GameDatabaseProvider {
    @Volatile
    private var INSTANCE: GameDatabase? = null

    fun getDatabase(context: Context): GameDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                GameDatabase::class.java,
                "game_database"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}
