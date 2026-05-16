package com.gramakalyana.sports.ui.fan

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.gramakalyana.sports.data.repository.FirebaseRepository
import com.gramakalyana.sports.databinding.ActivityPublicFanBinding
import com.gramakalyana.sports.ui.stats.TwoColumnStatsAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PublicFanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPublicFanBinding
    private val repo = FirebaseRepository()

    private val liveScoreAdapter = LiveScoreAdapter()
    private val scheduleAdapter  = ScheduleAdapter()

    // Stats adapter — initialised after teams load
    private var statsAdapter: TwoColumnStatsAdapter? = null

    private var liveJob:     Job? = null
    private var scheduleJob: Job? = null
    private var statsJob:    Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublicFanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "🏆 Grama Kalyana Sports"

        binding.rvLive.layoutManager     = LinearLayoutManager(this)
        binding.rvLive.adapter           = liveScoreAdapter

        binding.rvSchedule.layoutManager = LinearLayoutManager(this)
        binding.rvSchedule.adapter       = scheduleAdapter

        binding.rvStats.layoutManager    = LinearLayoutManager(this)
        // adapter set after teams load below

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { showTab(tab?.position ?: 0) }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        loadLiveScores()
        loadSchedule()
        initStats()
        showTab(0)
    }

    private fun showTab(index: Int) {
        binding.rvLive.visibility       = if (index == 0) View.VISIBLE else View.GONE
        binding.tvNoLive.visibility     = View.GONE
        binding.rvSchedule.visibility   = if (index == 1) View.VISIBLE else View.GONE
        binding.tvNoSchedule.visibility = View.GONE
        binding.rvStats.visibility      = if (index == 2) View.VISIBLE else View.GONE
        binding.tvNoStats.visibility    = View.GONE
        binding.liveBanner.visibility   = View.GONE
    }

    private fun loadLiveScores() {
        liveJob?.cancel()
        liveJob = lifecycleScope.launch {
            repo.getAllLiveScores().collectLatest { scores ->
                liveScoreAdapter.submitList(scores)
                if (binding.tabLayout.selectedTabPosition == 0) {
                    binding.tvNoLive.visibility   = if (scores.isEmpty()) View.VISIBLE else View.GONE
                    binding.liveBanner.visibility = if (scores.any { it.isLive }) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun loadSchedule() {
        scheduleJob?.cancel()
        scheduleJob = lifecycleScope.launch {
            // Load tournament info first to show sport + name on schedule cards
            val tournaments = repo.getTournaments().first()
            val tInfo = tournaments.associate { it.id to Pair(it.name, it.sport) }
            scheduleAdapter.setTournamentInfo(tInfo)

            repo.getAllMatches().collectLatest { matches ->
                scheduleAdapter.submitList(matches)
                if (binding.tabLayout.selectedTabPosition == 1)
                    binding.tvNoSchedule.visibility = if (matches.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    /**
     * Fan view shows all players globally (across all tournaments).
     * We load all matches to infer team groupings, then group players by teamId.
     * If there are exactly 2 teams across all live/recent matches, show them in columns;
     * otherwise fall back to two columns split by natural ordering.
     */
    /**
     * Stats tab shows ALL players grouped by team across ALL tournaments.
     * Each unique teamId gets one column. If more than 2 teams exist, we show
     * the two teams with the most players (most active match).
     */
    private fun initStats() {
        statsJob?.cancel()
        statsJob = lifecycleScope.launch {
            // Load all matches + teams to build teamId -> teamName map
            val allMatches = repo.getAllMatches().first()
            val teamNames  = mutableMapOf<String, String>() // teamId -> teamName
            val teamSport  = mutableMapOf<String, String>() // teamId -> sport (from tournament)
            allMatches.forEach { m ->
                if (m.team1Id.isNotEmpty()) teamNames[m.team1Id] = m.team1Name
                if (m.team2Id.isNotEmpty()) teamNames[m.team2Id] = m.team2Name
            }

            // Get all tournaments to find sport per match
            val tournaments = repo.getTournaments().first()
            val tournamentSportMap = tournaments.associate { it.id to it.sport }
            val latestSport = allMatches.lastOrNull()?.let { m ->
                tournamentSportMap[m.tournamentId] ?: "Kabaddi"
            } ?: "Kabaddi"

            repo.getAllPlayers().collectLatest { players ->
                if (players.isEmpty()) {
                    if (binding.tabLayout.selectedTabPosition == 2)
                        binding.tvNoStats.visibility = View.VISIBLE
                    return@collectLatest
                }

                // Group players by teamId
                val grouped = players.groupBy { it.teamId }

                // Pick the 2 biggest groups
                val sortedGroups = grouped.entries
                    .sortedByDescending { it.value.size }
                    .take(2)

                val team1Id   = sortedGroups.getOrNull(0)?.key ?: ""
                val team2Id   = sortedGroups.getOrNull(1)?.key ?: ""
                val team1Name = teamNames[team1Id] ?: "Team 1"
                val team2Name = teamNames[team2Id] ?: "Team 2"
                val t1Players = grouped[team1Id] ?: emptyList()
                val t2Players = grouped[team2Id] ?: emptyList()

                val adapter = TwoColumnStatsAdapter(team1Name, team2Name, team1Id, team2Id, latestSport)
                statsAdapter = adapter
                binding.rvStats.adapter = adapter
                adapter.setPlayers(t1Players, t2Players)

                if (binding.tabLayout.selectedTabPosition == 2)
                    binding.tvNoStats.visibility = View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
