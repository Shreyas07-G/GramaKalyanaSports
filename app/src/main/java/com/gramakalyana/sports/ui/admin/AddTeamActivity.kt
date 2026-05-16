package com.gramakalyana.sports.ui.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gramakalyana.sports.data.models.Match
import com.gramakalyana.sports.data.models.Player
import com.gramakalyana.sports.data.models.Team
import com.gramakalyana.sports.data.repository.FirebaseRepository
import com.gramakalyana.sports.databinding.ActivityAddTeamBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddTeamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTeamBinding
    private val repo = FirebaseRepository()
    private var tournamentId = ""
    private var sport = ""
    private val teams = mutableListOf<Team>()

    // Stores the chosen match date+time as a Calendar
    private val matchCalendar = Calendar.getInstance().apply {
        // Default: tomorrow at 4 PM
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 16)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    private val displayFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTeamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Add Teams & Players"

        tournamentId = intent.getStringExtra("TOURNAMENT_ID") ?: ""
        sport        = intent.getStringExtra("SPORT") ?: "Kabaddi"

        // Show default date/time on button
        updateDateTimeDisplay()

        // ── Date/Time picker ─────────────────────────────────────────────────
        binding.btnPickDateTime.setOnClickListener {
            // First pick date
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    matchCalendar.set(year, month, day)
                    // Then pick time
                    TimePickerDialog(
                        this,
                        { _, hour, minute ->
                            matchCalendar.set(Calendar.HOUR_OF_DAY, hour)
                            matchCalendar.set(Calendar.MINUTE, minute)
                            matchCalendar.set(Calendar.SECOND, 0)
                            updateDateTimeDisplay()
                        },
                        matchCalendar.get(Calendar.HOUR_OF_DAY),
                        matchCalendar.get(Calendar.MINUTE),
                        false
                    ).show()
                },
                matchCalendar.get(Calendar.YEAR),
                matchCalendar.get(Calendar.MONTH),
                matchCalendar.get(Calendar.DAY_OF_MONTH)
            ).also { dlg ->
                // Don't allow selecting past dates
                dlg.datePicker.minDate = System.currentTimeMillis() - 1000
            }.show()
        }

        // ── Add Team ─────────────────────────────────────────────────────────
        binding.btnAddTeam.setOnClickListener {
            val teamName    = binding.etTeamName.text.toString().trim()
            val playersText = binding.etPlayers.text.toString().trim()

            if (teamName.isEmpty()) {
                binding.etTeamName.error = "Enter team name"
                return@setOnClickListener
            }

            val playerNames = playersText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

            lifecycleScope.launch {
                val teamResult = repo.addTeam(Team(tournamentId = tournamentId, name = teamName))
                teamResult.onSuccess { teamId ->
                    playerNames.forEach { playerName ->
                        repo.addPlayer(Player(
                            name         = playerName,
                            teamId       = teamId,
                            tournamentId = tournamentId
                        ))
                    }
                    teams.add(Team(id = teamId, name = teamName, tournamentId = tournamentId))
                    updateTeamsList()
                    binding.etTeamName.text?.clear()
                    binding.etPlayers.text?.clear()
                    Toast.makeText(
                        this@AddTeamActivity,
                        "✅ Team '$teamName' added with ${playerNames.size} player(s)!",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (teams.size >= 2) binding.btnCreateMatch.isEnabled = true
                }
                teamResult.onFailure {
                    Toast.makeText(this@AddTeamActivity, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // ── Schedule Match ────────────────────────────────────────────────────
        binding.btnCreateMatch.setOnClickListener {
            if (teams.size < 2) {
                Toast.makeText(this, "Add at least 2 teams first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val match = Match(
                    tournamentId = tournamentId,
                    team1Id      = teams[0].id,
                    team1Name    = teams[0].name,
                    team2Id      = teams[1].id,
                    team2Name    = teams[1].name,
                    scheduledAt  = matchCalendar.timeInMillis   // ✅ Real scheduled time
                )
                val result = repo.createMatch(match)
                result.onSuccess {
                    Toast.makeText(
                        this@AddTeamActivity,
                        "🏆 Match scheduled for ${displayFormat.format(matchCalendar.time)}!",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                result.onFailure {
                    Toast.makeText(this@AddTeamActivity, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.btnCreateMatch.isEnabled = false
    }

    private fun updateDateTimeDisplay() {
        binding.btnPickDateTime.text = "📅  ${displayFormat.format(matchCalendar.time)}"
    }

    private fun updateTeamsList() {
        binding.tvTeamsList.text = teams.mapIndexed { i, t ->
            "${i + 1}. ${t.name}"
        }.joinToString("\n")
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
