package com.gramakalyana.sports.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gramakalyana.sports.data.models.Tournament
import com.gramakalyana.sports.databinding.ItemTournamentBinding

class TournamentAdapter(
    private val onScore: (Tournament) -> Unit,
    private val onStats: (Tournament) -> Unit,
    private val onFan:   (Tournament) -> Unit
) : ListAdapter<Tournament, TournamentAdapter.VH>(DIFF) {

    inner class VH(val b: ItemTournamentBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemTournamentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = getItem(position)
        holder.b.tvName.text = t.name
        holder.b.tvSport.text = t.sport
        holder.b.tvLocation.text = t.location
        holder.b.tvStatus.text = t.status.uppercase()
        holder.b.btnScore.setOnClickListener  { onScore(t) }
        holder.b.btnStats.setOnClickListener  { onStats(t) }
        holder.b.btnFanView.setOnClickListener { onFan(t) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Tournament>() {
            override fun areItemsTheSame(a: Tournament, b: Tournament) = a.id == b.id
            override fun areContentsTheSame(a: Tournament, b: Tournament) = a == b
        }
    }
}
