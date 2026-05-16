package com.gramakalyana.sports.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gramakalyana.sports.R
import com.gramakalyana.sports.data.models.Tournament
import com.gramakalyana.sports.data.repository.FirebaseRepository
import com.gramakalyana.sports.databinding.ActivityAdminDashboardBinding
import com.gramakalyana.sports.ui.auth.LoginActivity
import com.gramakalyana.sports.ui.fan.FanViewActivity
import com.gramakalyana.sports.ui.scorer.ScorerActivity
import com.gramakalyana.sports.ui.stats.PlayerStatsActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val repo = FirebaseRepository()
    private val adapter = TournamentAdapter(
        onScore = { t -> openScorer(t) },
        onStats = { t -> openStats(t) },
        onFan   = { t -> openFanView(t) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.rvTournaments.layoutManager = LinearLayoutManager(this)
        binding.rvTournaments.adapter = adapter

        binding.fabCreate.setOnClickListener {
            startActivity(Intent(this, CreateTournamentActivity::class.java))
        }

        lifecycleScope.launch {
            repo.getTournaments().collectLatest { list ->
                adapter.submitList(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                repo.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openScorer(t: Tournament) {
        val intent = Intent(this, ScorerActivity::class.java)
        intent.putExtra("TOURNAMENT_ID", t.id)
        intent.putExtra("TOURNAMENT_NAME", t.name)
        intent.putExtra("SPORT", t.sport)
        startActivity(intent)
    }

    private fun openStats(t: Tournament) {
        val intent = Intent(this, PlayerStatsActivity::class.java)
        intent.putExtra("TOURNAMENT_ID", t.id)
        intent.putExtra("TOURNAMENT_NAME", t.name)
        intent.putExtra("SPORT", t.sport)
        startActivity(intent)
    }

    private fun openFanView(t: Tournament) {
        val intent = Intent(this, FanViewActivity::class.java)
        intent.putExtra("TOURNAMENT_ID", t.id)
        intent.putExtra("TOURNAMENT_NAME", t.name)
        intent.putExtra("SPORT", t.sport)
        startActivity(intent)
    }
}
