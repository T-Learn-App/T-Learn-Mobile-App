package com.example.t_learnappmobile.data.game

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import android.content.Context

@Database(
    entities = [GameResultEntity::class],
    version = 2,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameResultDao(): GameResultDao
}

object GameDatabaseProvider {
    @Volatile
    private var INSTANCE: GameDatabase? = null

    fun getDatabase(context: Context): GameDatabase {
        return INSTANCE ?: synchronized(this) {
            // ✅ ФИКС: удаляем старую БД ПЕРЕД созданием
            context.deleteDatabase("game_database")

            val instance = Room.databaseBuilder(
                context.applicationContext,
                GameDatabase::class.java,
                "game_database"
            )
                .fallbackToDestructiveMigration()
                .build()

            INSTANCE = instance
            instance
        }
    }
}
