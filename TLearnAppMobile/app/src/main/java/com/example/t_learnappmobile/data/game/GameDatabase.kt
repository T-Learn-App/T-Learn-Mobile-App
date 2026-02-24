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

