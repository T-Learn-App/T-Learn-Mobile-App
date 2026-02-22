package com.example.t_learnappmobile.presentation.statistics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.data.leaderboard.LeaderboardPlayer

class LeaderboardAdapter(
    private val players: List<LeaderboardPlayer>
) : RecyclerView.Adapter<LeaderboardAdapter.PlayerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard_player, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(players[position])
    }

    override fun getItemCount(): Int = players.size

    class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val positionText: TextView = itemView.findViewById(R.id.positionText)
        private val avatarImage: ImageView = itemView.findViewById(R.id.avatarImage)
        private val playerNameText: TextView = itemView.findViewById(R.id.playerNameText)
        private val playerScoreText: TextView = itemView.findViewById(R.id.playerScoreText)

        fun bind(player: LeaderboardPlayer) {
            positionText.text = "#${player.position}"
            playerNameText.text = player.name
            playerScoreText.text = player.score.toString()
            avatarImage.setImageResource(android.R.drawable.ic_menu_gallery) // заглушка
        }
    }
}
