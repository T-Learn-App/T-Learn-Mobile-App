package com.example.t_learnappmobile.data.game

import androidx.room.*
import androidx.room.RoomWarnings.Companion.CURSOR_MISMATCH
@Dao
interface GameResultDao {
    @Insert
    suspend fun insert(result: GameResultEntity)

    // ❌ УДАЛИЛИ getTopResults() - он ломает компиляцию
    // @Query("SELECT * FROM game_results ORDER BY totalScore DESC LIMIT 10")
    // suspend fun getTopResults(): List<GameResultEntity>

    // ✅ ✅ ✅ СУММА ВСЕХ игр пользователя
    @Query("SELECT COALESCE(SUM(sessionScore), 0) FROM game_results WHERE userId = :userId")
    suspend fun getUserTotalScore(userId: Int): Int

    @SuppressWarnings(CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT userId, 
               COALESCE(SUM(sessionScore), 0) as totalScore,
               (SELECT COUNT(DISTINCT r2.userId) + 1 
                FROM (SELECT userId, SUM(sessionScore) as userTotal FROM game_results GROUP BY userId) r2 
                WHERE r2.userTotal > COALESCE(SUM(game_results.sessionScore), 0)) as position
        FROM game_results 
        WHERE userId = :userId 
        GROUP BY userId
    """)
    suspend fun getUserLeaderboardInfo(userId: Int): UserLeaderboardInfo?

    @SuppressWarnings(CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT userId, COALESCE(SUM(sessionScore), 0) as totalScore 
        FROM game_results 
        GROUP BY userId 
        ORDER BY totalScore DESC 
        LIMIT 10
    """)
    suspend fun getLeaderboardWithUserIds(): List<SimpleLeaderboardEntry>
}


// ✅ Data классы для запросов (НЕ Entity!)
data class UserLeaderboardInfo(
    val userId: Int,
    val totalScore: Int,
    val position: Int
)

data class SimpleLeaderboardEntry(
    val userId: Int,
    val totalScore: Int
)
