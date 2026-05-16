package com.gramakalyana.sports.ui.scorer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gramakalyana.sports.databinding.ActivityMatchResultBinding
import com.gramakalyana.sports.ui.fan.FanViewActivity

class MatchResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMatchResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val team1Name   = intent.getStringExtra("TEAM1_NAME") ?: "Team 1"
        val team2Name   = intent.getStringExtra("TEAM2_NAME") ?: "Team 2"
        val team1Score  = intent.getIntExtra("TEAM1_SCORE", 0)
        val team2Score  = intent.getIntExtra("TEAM2_SCORE", 0)
        val sport       = intent.getStringExtra("SPORT") ?: "Kabaddi"
        val team1Wickets = intent.getIntExtra("TEAM1_WICKETS", 0)
        val team2Wickets = intent.getIntExtra("TEAM2_WICKETS", 0)

        val diff = Math.abs(team1Score - team2Score)
        val winner = if (team1Score > team2Score) team1Name else team2Name
        val loser  = if (team1Score > team2Score) team2Name else team1Name
        val winnerScore = maxOf(team1Score, team2Score)
        val loserScore  = minOf(team1Score, team2Score)
        val winnerWickets = if (team1Score > team2Score) team1Wickets else team2Wickets

        // Build result message based on sport
        val resultMessage = when (sport) {
            "Cricket" -> {
                if (diff == 0) {
                    "🤝 It's a TIE!\n$team1Name $team1Score vs $team2Name $team2Score"
                } else {
                    val wicketsLeft = 10 - winnerWickets
                    if (winnerWickets < 10) {
                        "🏏 $winner won by $wicketsLeft wicket${if (wicketsLeft != 1) "s" else ""}!\n$winner: $winnerScore | $loser: $loserScore"
                    } else {
                        "🏏 $winner won by $diff run${if (diff != 1) "s" else ""}!\n$winner: $winnerScore | $loser: $loserScore"
                    }
                }
            }
            "Kabaddi" -> {
                if (diff == 0) {
                    "🤝 It's a TIE!\n$team1Name $team1Score — $team2Score $team2Name"
                } else {
                    "🏅 $winner won by $diff point${if (diff != 1) "s" else ""}!\n$winner: $winnerScore | $loser: $loserScore"
                }
            }
            "Volleyball" -> {
                if (diff == 0) {
                    "🤝 It's a TIE!\n$team1Name $team1Score — $team2Score $team2Name"
                } else {
                    "🏐 $winner won by $diff point${if (diff != 1) "s" else ""}!\n$winner: $winnerScore | $loser: $loserScore"
                }
            }
            else -> "$winner won $winnerScore - $loserScore!"
        }

        binding.tvWinner.text = winner
        binding.tvResultMessage.text = resultMessage
        binding.tvFinalScore.text = "$team1Score  —  $team2Score"
        binding.tvTeam1Final.text = team1Name
        binding.tvTeam2Final.text = team2Name
        binding.tvSportFinal.text = sport

        // Share result
        binding.btnShareResult.setOnClickListener {
            val shareText = buildString {
                appendLine("🏆 MATCH RESULT — GRAMA KALYANA SPORTS")
                appendLine("Sport: $sport")
                appendLine("")
                appendLine("$team1Name  $team1Score  —  $team2Score  $team2Name")
                appendLine("")
                appendLine(resultMessage)
                appendLine("")
                appendLine("Shared via Grama-Kalyana Sports App")
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                setPackage("com.whatsapp")
            }
            try {
                startActivity(shareIntent)
            } catch (e: Exception) {
                shareIntent.setPackage(null)
                startActivity(Intent.createChooser(shareIntent, "Share Result"))
            }
        }

        binding.btnBackHome.setOnClickListener {
            finish()
        }
    }
}
