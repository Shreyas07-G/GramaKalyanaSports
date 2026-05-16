package com.gramakalyana.sports.ui.stats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gramakalyana.sports.R
import com.gramakalyana.sports.data.models.Player

class TwoColumnStatsAdapter(
    private val team1Name: String,
    private val team2Name: String,
    private val team1Id: String,
    private val team2Id: String,
    private val sport: String = "Kabaddi"
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var team1Players: List<Player> = emptyList()
    private var team2Players: List<Player> = emptyList()

    // ✅ Toggle: true = show career stats, false = show match stats
    var showCareer: Boolean = true
        set(value) { field = value; notifyDataSetChanged() }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW    = 1
    }

    fun setPlayers(t1: List<Player>, t2: List<Player>) {
        team1Players = t1.sortedByDescending { it.totalPoints }
        team2Players = t2.sortedByDescending { it.totalPoints }
        notifyDataSetChanged()
    }

    override fun getItemCount() = maxOf(team1Players.size, team2Players.size) + 1
    override fun getItemViewType(position: Int) = if (position == 0) TYPE_HEADER else TYPE_ROW

    inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTeam1: TextView = view.findViewById(R.id.tv_col_team1)
        val tvTeam2: TextView = view.findViewById(R.id.tv_col_team2)
    }

    inner class RowVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank1: TextView = view.findViewById(R.id.tv_rank1)
        val tvName1: TextView = view.findViewById(R.id.tv_name1)
        val tvPts1:  TextView = view.findViewById(R.id.tv_pts1)
        val tvMom1:  TextView = view.findViewById(R.id.tv_mom1)
        val tvRank2: TextView = view.findViewById(R.id.tv_rank2)
        val tvName2: TextView = view.findViewById(R.id.tv_name2)
        val tvPts2:  TextView = view.findViewById(R.id.tv_pts2)
        val tvMom2:  TextView = view.findViewById(R.id.tv_mom2)
        val divider: View     = view.findViewById(R.id.view_divider)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER)
            HeaderVH(inf.inflate(R.layout.item_stats_header, parent, false))
        else
            RowVH(inf.inflate(R.layout.item_stats_row, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderVH) {
            holder.tvTeam1.text = team1Name
            holder.tvTeam2.text = team2Name
            return
        }
        holder as RowVH
        val i = position - 1
        fun rankLabel(idx: Int) = when (idx) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "#${idx + 1}" }

        // ✅ Build stat line based on career vs match toggle
        fun statLine(p: Player): String = when (sport) {
            "Cricket" -> if (showCareer) {
                "🏏 ${p.totalPoints} runs  ⚾ ${p.wicketsTaken}W  (${p.matchesPlayed} matches)"
            } else {
                "🏏 ${p.matchPoints} runs  ⚾ ${p.matchWickets}W  (this match)"
            }
            "Kabaddi" -> if (showCareer) {
                val raids   = p.raidPoints.takeIf { it > 0 } ?: (p.totalPoints / 2)
                val tackles = p.tacklePoints
                "⚡ $raids raids  💪 $tackles tackles  (${p.matchesPlayed} matches)"
            } else {
                "⚡ ${p.matchRaidPoints} raids  💪 ${p.matchTacklePoints} tackles  (this match)"
            }
            "Volleyball" -> if (showCareer) buildString {
                append("🏐 ${p.totalPoints} pts")
                if (p.acePoints > 0)   append("  🎯 ${p.acePoints} aces")
                if (p.blockPoints > 0) append("  🛡️ ${p.blockPoints} blocks")
                append("  (${p.matchesPlayed} matches)")
            } else buildString {
                append("🏐 ${p.matchPoints} pts")
                if (p.matchAcePoints > 0)   append("  🎯 ${p.matchAcePoints} aces")
                if (p.matchBlockPoints > 0) append("  🛡️ ${p.matchBlockPoints} blocks")
                append("  (this match)")
            }
            else -> if (showCareer) "${p.totalPoints} pts (${p.matchesPlayed} matches)" else "${p.matchPoints} pts (this match)"
        }

        // Sort differently for match vs career
        val sortedT1 = if (showCareer) team1Players.sortedByDescending { it.totalPoints }
                       else            team1Players.sortedByDescending { it.matchPoints }
        val sortedT2 = if (showCareer) team2Players.sortedByDescending { it.totalPoints }
                       else            team2Players.sortedByDescending { it.matchPoints }

        val p1 = sortedT1.getOrNull(i)
        val p2 = sortedT2.getOrNull(i)

        // MOM display
        fun momLine(p: Player) = if (showCareer && p.manOfMatchCount > 0)
            "⭐ MOM ×${p.manOfMatchCount}" else ""

        if (p1 != null) {
            holder.tvRank1.text = rankLabel(i); holder.tvName1.text = p1.name
            holder.tvPts1.text = statLine(p1);  holder.tvMom1.text = momLine(p1)
            holder.tvRank1.visibility = View.VISIBLE
            holder.tvName1.visibility = View.VISIBLE
            holder.tvPts1.visibility  = View.VISIBLE
        } else {
            holder.tvRank1.visibility = View.INVISIBLE
            holder.tvName1.visibility = View.INVISIBLE
            holder.tvPts1.visibility  = View.INVISIBLE
            holder.tvMom1.text = ""
        }

        if (p2 != null) {
            holder.tvRank2.text = rankLabel(i); holder.tvName2.text = p2.name
            holder.tvPts2.text = statLine(p2);  holder.tvMom2.text = momLine(p2)
            holder.tvRank2.visibility = View.VISIBLE
            holder.tvName2.visibility = View.VISIBLE
            holder.tvPts2.visibility  = View.VISIBLE
        } else {
            holder.tvRank2.visibility = View.INVISIBLE
            holder.tvName2.visibility = View.INVISIBLE
            holder.tvPts2.visibility  = View.INVISIBLE
            holder.tvMom2.text = ""
        }
    }
}
