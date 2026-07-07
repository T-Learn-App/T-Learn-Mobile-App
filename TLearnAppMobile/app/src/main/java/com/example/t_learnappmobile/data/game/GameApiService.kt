package com.example.t_learnappmobile.data.game

import retrofit2.Response
import retrofit2.http.*

data class GameResultRequest(
    val sessionScore: Int,
    val wordsCount: Int,
    val timestamp: Long
)

data class GameResultResponse(
    val id: Long,
    val userId: Int,
    val sessionScore: Int,
    val wordsCount: Int,
    val timestamp: Long
)

data class LeaderboardResponse(
    val players: List<LeaderboardPlayerResponse>
)

data class LeaderboardPlayerResponse(
    val id: Int,
    val name: String,
    val score: Int,
    val position: Int
)

data class UserLeaderboardInfo(
    val userId: Int,
    val totalScore: Int,
    val position: Int
)

interface GameApiService {
    @POST("games/result")
    suspend fun saveGameResult(
        @Header("Authorization") token: String,
        @Body request: GameResultRequest
    ): Response<GameResultResponse>

    @GET("leaderboard/{seasonId}")
    suspend fun getLeaderboard(
        @Header("Authorization") token: String,
        @Path("seasonId") seasonId: String
    ): Response<LeaderboardResponse>

}