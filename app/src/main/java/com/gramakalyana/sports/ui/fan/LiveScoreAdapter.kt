package com.gramakalyana.sports.ui.fan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gramakalyana.sports.R
import com.gramakalyana.sports.data.models.LiveScore
import com.gramakalyana.sports.databinding.ItemLiveScoreBinding

class LiveScoreAdapter : ListAdapter<LiveScore, LiveScoreAdapter.VH>(DIFF) {

    inner class VH(val b: ItemLiveScoreBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemLiveScoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = getItem(position)
        holder.b.tvTeam1Name.text = s.team1Name.ifEmpty { "Team 1" }
        holder.b.tvTeam2Name.text = s.team2Name.ifEmpty { "Team 2" }
        holder.b.tvSport.text     = s.sport
        holder.b.tvPeriod.text    = s.period

        // ── Score display ─────────────────────────────────────────────────────
        when (s.sport) {
            "Cricket" -> {
                holder.b.tvTeam1Score.text = "${s.team1Score}/${s.team1Wickets}w\n${s.team1Overs} ov"
                holder.b.tvTeam2Score.text = "${s.team2Score}/${s.team2Wickets}w\n${s.team2Overs} ov"
            }
            else -> {
                holder.b.tvTeam1Score.text = s.team1Score.toString()
                holder.b.tvTeam2Score.text = s.team2Score.toString()
            }
        }

        // ── LIVE badge ────────────────────────────────────────────────────────
        if (s.isLive) {
            holder.b.tvLiveBadge.visibility = android.view.View.VISIBLE
            holder.b.tvLiveBadge.text = "● LIVE"
        } else {
            holder.b.tvLiveBadge.visibility = android.view.View.GONE
        }

        // ── Toss result ───────────────────────────────────────────────────────
        if (s.tossWinner.isNotEmpty()) {
            holder.b.tvTossResult.visibility = android.view.View.VISIBLE
            holder.b.tvTossResult.text = "🪙 ${s.tossWinner} won toss · ${s.tossDecision}"
        } else {
            holder.b.tvTossResult.visibility = android.view.View.GONE
        }

        // ── Cricket: striker / non-striker ────────────────────────────────────
        if (s.sport == "Cricket" && s.isLive && s.striker.isNotEmpty()) {
            holder.b.tvBatterInfo.visibility = android.view.View.VISIBLE
            holder.b.tvBatterInfo.text = "🏏 ${s.striker}*   🏃 ${s.nonStriker}"
        } else {
            holder.b.tvBatterInfo.visibility = android.view.View.GONE
        }

        // ── Man of Match ──────────────────────────────────────────────────────
        if (s.manOfMatch.isNotEmpty()) {
            holder.b.tvManOfMatch.visibility = android.view.View.VISIBLE
            holder.b.tvManOfMatch.text = "⭐ Man of Match: ${s.manOfMatch}"
        } else {
            holder.b.tvManOfMatch.visibility = android.view.View.GONE
        }

        // Highlight leading team
        when {
            s.team1Score > s.team2Score -> { holder.b.tvTeam1Score.alpha = 1f; holder.b.tvTeam2Score.alpha = 0.5f }
            s.team2Score > s.team1Score -> { holder.b.tvTeam1Score.alpha = 0.5f; holder.b.tvTeam2Score.alpha = 1f }
            else -> { holder.b.tvTeam1Score.alpha = 1f; holder.b.tvTeam2Score.alpha = 1f }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<LiveScore>() {
            override fun areItemsTheSame(a: LiveScore, b: LiveScore) = a.matchId == b.matchId
            override fun areContentsTheSame(a: LiveScore, b: LiveScore) = a == b
        }
    }
}
