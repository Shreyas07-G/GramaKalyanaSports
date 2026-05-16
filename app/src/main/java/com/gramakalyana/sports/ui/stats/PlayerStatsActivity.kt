package com.gramakalyana.sports.ui.stats

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gramakalyana.sports.data.repository.FirebaseRepository
import com.gramakalyana.sports.databinding.ActivityPlayerStatsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlayerStatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerStatsBinding
    private val repo = FirebaseRepository()
    private lateinit var adapter: TwoColumnStatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tournamentId   = intent.getStringExtra("TOURNAMENT_ID") ?: ""
        val tournamentName = intent.getStringExtra("TOURNAMENT_NAME") ?: "Stats"
        val sport          = intent.getStringExtra("SPORT") ?: "Kabaddi"
        title = "Player Stats — $tournamentName"

        binding.rvPlayers.layoutManager = LinearLayoutManager(this)

        // ✅ Toggle buttons
        updateToggleUI(showCareer = true)

        binding.btnCareerStats.setOnClickListener {
            adapter.showCareer = true
            updateToggleUI(showCareer = true)
        }
        binding.btnMatchStats.setOnClickListener {
            adapter.showCareer = false
            updateToggleUI(showCareer = false)
        }

        lifecycleScope.launch {
            val teams = repo.getTeamsForTournament(tournamentId).first()
            val team1 = teams.getOrNull(0)
            val team2 = teams.getOrNull(1)

            adapter = TwoColumnStatsAdapter(
                team1Name = team1?.name ?: "Team 1",
                team2Name = team2?.name ?: "Team 2",
                team1Id   = team1?.id   ?: "",
                team2Id   = team2?.id   ?: "",
                sport     = sport
            )
            binding.rvPlayers.adapter = adapter

            repo.getPlayersForTournamentNoIndex(tournamentId).collectLatest { players ->
                val t1 = if (team1 != null) players.filter { it.teamId == team1.id } else emptyList()
                val t2 = if (team2 != null) players.filter { it.teamId == team2.id } else emptyList()
                adapter.setPlayers(t1, t2)
                binding.tvEmpty.visibility   = if (players.isEmpty()) View.VISIBLE else View.GONE
                binding.rvPlayers.visibility = if (players.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun updateToggleUI(showCareer: Boolean) {
        val activeColor   = 0xFFFF6B00.toInt()
        val inactiveColor = 0xFF1A1A2E.toInt()
        val activeText    = 0xFFFFFFFF.toInt()
        val inactiveText  = 0xFF999999.toInt()

        binding.btnCareerStats.backgroundTintList =
            android.content.res.ColorStateList.valueOf(if (showCareer) activeColor else inactiveColor)
        binding.btnCareerStats.setTextColor(if (showCareer) activeText else inactiveText)

        binding.btnMatchStats.backgroundTintList =
            android.content.res.ColorStateList.valueOf(if (!showCareer) activeColor else inactiveColor)
        binding.btnMatchStats.setTextColor(if (!showCareer) activeText else inactiveText)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
