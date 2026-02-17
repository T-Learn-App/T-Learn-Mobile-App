package com.example.t_learnappmobile.data.statistics

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: DailyStatsEntity)

    @Query("SELECT * FROM daily_stats WHERE userId = :userId AND dictionaryId = :dictionaryId AND date = :date")
    suspend fun getByDate(userId: Int, dictionaryId: Int, date: String): DailyStatsEntity?

    @Query("""
        SELECT * FROM daily_stats 
        WHERE userId = :userId 
          AND dictionaryId = :dictionaryId 
          AND date BETWEEN :startDate AND :endDate 
        ORDER BY date ASC
    """)
    suspend fun getStatsForPeriod(
        userId: Int,
        dictionaryId: Int,
        startDate: String,
        endDate: String
    ): List<DailyStatsEntity>

    @Query("DELETE FROM daily_stats WHERE userId = :userId AND dictionaryId = :dictionaryId")
    suspend fun deleteForDictionary(userId: Int, dictionaryId: Int)

    @Query("DELETE FROM daily_stats")
    suspend fun deleteAll()
}