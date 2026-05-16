package com.gramakalyana.sports.ui.fan

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.gramakalyana.sports.data.repository.FirebaseRepository
import com.gramakalyana.sports.databinding.ActivityFanViewBinding
import com.gramakalyana.sports.ui.stats.TwoColumnStatsAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FanViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFanViewBinding
    private val repo = FirebaseRepository()

    private val liveAdapter     = LiveScoreAdapter()
    private val scheduleAdapter = ScheduleAdapter()
    private var statsAdapter: TwoColumnStatsAdapter? = null

    private var tournamentId = ""
    private var sport        = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFanViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tournamentId = intent.getStringExtra("TOURNAMENT_ID") ?: ""
        sport        = intent.getStringExtra("SPORT") ?: "Kabaddi"
        val name     = intent.getStringExtra("TOURNAMENT_NAME") ?: "Fan View"
        title = "📺 $name"

        // Setup RecyclerViews
        binding.rvLiveScores.layoutManager = LinearLayoutManager(this)
        binding.rvLiveScores.adapter = liveAdapter

        binding.rvSchedule.layoutManager = LinearLayoutManager(this)
        binding.rvSchedule.adapter = scheduleAdapter

        binding.rvStats.layoutManager = LinearLayoutManager(this)

        // Tab listener
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { showTab(tab?.position ?: 0) }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        loadLive()
        loadSchedule()
        loadStats()
        showTab(0)
    }

    private fun showTab(index: Int) {
        binding.rvLiveScores.visibility  = if (index == 0) View.VISIBLE else View.GONE
        binding.tvNoLive.visibility      = View.GONE
        binding.lottieAnimation.visibility = View.GONE
        binding.rvSchedule.visibility    = if (index == 1) View.VISIBLE else View.GONE
        binding.tvNoSchedule.visibility  = View.GONE
        binding.rvStats.visibility       = if (index == 2) View.VISIBLE else View.GONE
        binding.tvNoStats.visibility     = View.GONE
    }

    private fun loadLive() {
        lifecycleScope.launch {
            repo.getAllLiveScores().collectLatest { all ->
                val filtered = if (tournamentId.isNotEmpty())
                    all.filter { it.tournamentId == tournamentId } else all
                liveAdapter.submitList(filtered)
                binding.tvNoLive.visibility =
                    if (filtered.isEmpty()) View.VISIBLE else View.GONE
                binding.lottieAnimation.visibility =
                    if (filtered.any { it.isLive }) View.VISIBLE else View.GONE
            }
        }
    }

    private fun loadSchedule() {
        lifecycleScope.launch {
            // Pass tournament info so schedule shows sport name
            val tournaments = repo.getTournaments().first()
            val tInfo = tournaments.associate { it.id to Pair(it.name, it.sport) }
            scheduleAdapter.setTournamentInfo(tInfo)

            repo.getMatchesForTournament(tournamentId).collectLatest { matches ->
                scheduleAdapter.submitList(matches)
                binding.tvNoSchedule.visibility =
                    if (matches.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val teams = repo.getTeamsForTournament(tournamentId).first()
            val team1 = teams.getOrNull(0)
            val team2 = teams.getOrNull(1)

            val adapter = TwoColumnStatsAdapter(
                team1Name = team1?.name ?: "Team 1",
                team2Name = team2?.name ?: "Team 2",
                team1Id   = team1?.id   ?: "",
                team2Id   = team2?.id   ?: "",
                sport     = sport
            )
            statsAdapter = adapter
            binding.rvStats.adapter = adapter

            repo.getPlayersForTournament(tournamentId).collectLatest { players ->
                val t1 = if (team1 != null) players.filter { it.teamId == team1.id } else emptyList()
                val t2 = if (team2 != null) players.filter { it.teamId == team2.id } else emptyList()
                adapter.setPlayers(t1, t2)
                binding.tvNoStats.visibility =
                    if (players.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
