package com.example.t_learnappmobile.data.game

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GameResultDao {
    @Insert
    suspend fun insert(result: GameResultEntity)

    @Query("SELECT * FROM game_results ORDER BY score DESC LIMIT 10")
    suspend fun getTopResults(): List<GameResultEntity>

    @Query("SELECT * FROM game_results WHERE userId = :userId ORDER BY score DESC LIMIT 1")
    suspend fun getUserBestScore(userId: Int): GameResultEntity?
}
