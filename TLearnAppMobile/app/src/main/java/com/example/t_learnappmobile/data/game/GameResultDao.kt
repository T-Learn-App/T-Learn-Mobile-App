package com.example.t_learnappmobile.data.game

import androidx.room.*

import androidx.room.RoomWarnings.Companion.CURSOR_MISMATCH

@Dao
interface GameResultDao {
    @Insert
    suspend fun insert(result: GameResultEntity)

    @Query("SELECT * FROM game_results ORDER BY score DESC LIMIT 10")
    suspend fun getTopResults(): List<GameResultEntity>

    @Query("SELECT * FROM game_results WHERE userId = :userId ORDER BY score DESC LIMIT 1")
    suspend fun getUserBestScore(userId: Int): GameResultEntity?

    @SuppressWarnings(CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT *, 
        (SELECT COUNT(*) + 1 FROM game_results r2 
         WHERE r2.score > game_results.score) as position
        FROM game_results 
        WHERE userId = :userId 
        ORDER BY score DESC LIMIT 1
    """)
    suspend fun getUserPosition(userId: Int): GameResultEntity?
}
