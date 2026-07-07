package com.example.t_learnappmobile.data.leaderboard

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LeaderboardApi {
    @GET("leaderboard/{seasonId}")
    suspend fun getLeaderboard(
        @Path("seasonId") seasonId: String,
        @Query("userId") userId: Long? = null
    ): Response<LeaderboardResponse>
}


data class LeaderboardResponse(
    val leaderboard: List<LeaderboardPlayer>,
    val currentUser: LeaderboardPlayer?
)

