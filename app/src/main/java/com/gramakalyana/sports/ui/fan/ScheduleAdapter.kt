package com.gramakalyana.sports.ui.fan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gramakalyana.sports.data.models.Match
import com.gramakalyana.sports.databinding.ItemScheduleBinding
import java.text.SimpleDateFormat
import java.util.*

class ScheduleAdapter : ListAdapter<Match, ScheduleAdapter.VH>(DIFF) {

    // Map of tournamentId -> (name, sport) passed from outside
    private var tournamentInfo: Map<String, Pair<String, String>> = emptyMap()

    fun setTournamentInfo(info: Map<String, Pair<String, String>>) {
        tournamentInfo = info
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemScheduleBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = getItem(position)
        holder.b.tvTeam1.text = m.team1Name.ifEmpty { "Team 1" }
        holder.b.tvTeam2.text = m.team2Name.ifEmpty { "Team 2" }

        // Show date if available, else show sport + tournament
        val info = tournamentInfo[m.tournamentId]
        holder.b.tvDateTime.text = when {
            m.scheduledAt > 0 -> {
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                sdf.format(Date(m.scheduledAt))
            }
            info != null -> "${info.second}  ·  ${info.first}"
            else -> "Upcoming Match"
        }

        val (statusText, statusColor) = when (m.status) {
            "live"      -> "● LIVE"   to 0xFFE53935.toInt()
            "completed" -> "✓ DONE"   to 0xFF757575.toInt()
            else        -> "UPCOMING" to 0xFF43A047.toInt()
        }
        holder.b.tvStatus.text = statusText
        holder.b.tvStatus.setTextColor(statusColor)
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Match>() {
            override fun areItemsTheSame(a: Match, b: Match) = a.id == b.id
            override fun areContentsTheSame(a: Match, b: Match) = a == b
        }
    }
}
