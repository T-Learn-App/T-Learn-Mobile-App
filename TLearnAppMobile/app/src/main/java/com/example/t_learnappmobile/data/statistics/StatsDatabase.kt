package com.example.t_learnappmobile.data.statistics

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DailyStatsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun dailyStatsDao(): DailyStatsDao
}