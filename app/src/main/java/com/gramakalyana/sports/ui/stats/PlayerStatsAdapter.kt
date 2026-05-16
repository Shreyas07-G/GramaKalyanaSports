package com.gramakalyana.sports.ui.stats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gramakalyana.sports.data.models.Player
import com.gramakalyana.sports.databinding.ItemPlayerStatBinding

class PlayerStatsAdapter : ListAdapter<Player, PlayerStatsAdapter.VH>(DIFF) {

    inner class VH(val b: ItemPlayerStatBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPlayerStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = getItem(position)
        holder.b.tvRank.text = "#${position + 1}"
        holder.b.tvPlayerName.text = p.name
        holder.b.tvPoints.text = "${p.totalPoints} pts"
        holder.b.tvMatches.text = "${p.matchesPlayed} matches"
        holder.b.tvMom.text = if (p.manOfMatchCount > 0) "⭐ ×${p.manOfMatchCount}" else ""

        // Top 3 get trophy icons
        holder.b.tvRank.text = when (position) {
            0 -> "🥇"
            1 -> "🥈"
            2 -> "🥉"
            else -> "#${position + 1}"
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Player>() {
            override fun areItemsTheSame(a: Player, b: Player) = a.id == b.id
            override fun areContentsTheSame(a: Player, b: Player) = a == b
        }
    }
}
