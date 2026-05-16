package com.gramakalyana.sports.ui.scorer

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.gramakalyana.sports.R
import com.gramakalyana.sports.data.models.LiveScore
import com.gramakalyana.sports.data.models.Player
import com.gramakalyana.sports.data.models.ScoreButton
import com.gramakalyana.sports.data.models.ScoreSnapshot
import com.gramakalyana.sports.data.models.SportConfig
import com.gramakalyana.sports.data.repository.FirebaseRepository
import com.gramakalyana.sports.databinding.ActivityScorerBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScorerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScorerBinding
    private val repo = FirebaseRepository()

    private var tournamentId = ""
    private var matchId = ""
    private var sport = ""
    private var team1Name = "Team 1"
    private var team2Name = "Team 2"
    private var team1Id = ""
    private var team2Id = ""
    private var team1Score = 0
    private var team2Score = 0
    private var team1Wickets = 0
    private var team2Wickets = 0
    private var period = "1st Half"

    // ── Cricket ──────────────────────────────────────────────────────────────
    private var totalOvers = 20
    private var team1Balls = 0; private var team1Overs = 0
    private var team2Balls = 0; private var team2Overs = 0
    private var currentInning = 1
    private var inning1Score = 0
    private var isSingleInnings = false
    private var isTestMatch = false          // ✅ NEW: Test match flag
    private var testInnings = 1             // ✅ 1–4 innings for test
    private var testTeam1Innings = mutableListOf<Pair<Int,Int>>() // score, wickets per innings
    private var testTeam2Innings = mutableListOf<Pair<Int,Int>>()

    // ── Active batters (Cricket) ─────────────────────────────────────────────
    private var striker: Player? = null         // ✅ current facing batter
    private var nonStriker: Player? = null      // ✅ other end batter
    private val battingTeamIsTeam1 get() = (currentInning % 2 == 1)  // odd innings = team1 bats

    // ── Toss ─────────────────────────────────────────────────────────────────
    private var tossWinnerName = ""
    private var tossDecision  = ""   // "Bat First" / "Bowl First" / "Raid First" / "Serve First"

    // ── Kabaddi ──────────────────────────────────────────────────────────────
    private var halfDurationMinutes = 20
    private var breakDurationMinutes = 5
    private var countDownTimer: CountDownTimer? = null
    private var timerRunning = false
    private var timeLeftMillis = 0L

    // ── Volleyball ───────────────────────────────────────────────────────────
    private var team1SetScore = 0
    private var team2SetScore = 0
    private var isExtraPoint = false

    private var team1Players: List<Player> = emptyList()
    private var team2Players: List<Player> = emptyList()
    private val scoreHistory = ArrayDeque<ScoreSnapshot>()
    private val outPlayerIds    = mutableSetOf<String>()
    private val scoredPlayerIds = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScorerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tournamentId = intent.getStringExtra("TOURNAMENT_ID") ?: ""
        sport        = intent.getStringExtra("SPORT") ?: "Kabaddi"
        title        = "Scorer — ${intent.getStringExtra("TOURNAMENT_NAME") ?: ""}"

        // Always show toss dialog first, then format/match dialog
        lifecycleScope.launch {
            val matches = repo.getMatchesForTournament(tournamentId).first()
            val match   = matches.firstOrNull { it.status == "scheduled" || it.status == "live" }
            if (match != null) {
                team1Name = match.team1Name; team2Name = match.team2Name
                team1Id   = match.team1Id;   team2Id   = match.team2Id
            }
            showTossDialog()
        }

        binding.btnPeriod.setOnClickListener { togglePeriod() }
        binding.btnShareScorecard.setOnClickListener { shareScorecard() }
        binding.btnUndo.setOnClickListener { undoLastScore() }
        binding.btnEndMatch.setOnClickListener { showEndMatchDialog() }
    }

    // ── TOSS DIALOG ──────────────────────────────────────────────────────────
    private fun showTossDialog() {
        // Step 1: Who won the toss?
        val teams = arrayOf(team1Name, team2Name)
        var tossWinnerIndex = 0
        AlertDialog.Builder(this)
            .setTitle("🪙 Toss — Who Won?")
            .setSingleChoiceItems(teams, 0) { _, i -> tossWinnerIndex = i }
            .setPositiveButton("Next ▶") { _, _ ->
                tossWinnerName = teams[tossWinnerIndex]
                showTossDecisionDialog(tossWinnerIndex)
            }
            .setCancelable(false)
            .show()
    }

    private fun showTossDecisionDialog(winnerIndex: Int) {
        // Step 2: What did they choose?
        val decisions = when (sport) {
            "Cricket"    -> arrayOf("🏏 Bat First", "🎯 Bowl First")
            "Kabaddi"    -> arrayOf("⚡ Raid First", "🛡️ Defend First")
            "Volleyball" -> arrayOf("🏐 Serve First", "🛡️ Receive First")
            else         -> arrayOf("Go First", "Go Second")
        }
        var chosenDecision = 0
        AlertDialog.Builder(this)
            .setTitle("$tossWinnerName won the toss! Choose:")
            .setSingleChoiceItems(decisions, 0) { _, i -> chosenDecision = i }
            .setPositiveButton("Confirm ✅") { _, _ ->
                tossDecision = decisions[chosenDecision]

                val announcement = buildString {
                    append("🪙 Toss: $tossWinnerName won\n")
                    append("Decision: $tossDecision\n\n")
                    when (sport) {
                        "Cricket" -> {
                            val batting = if (chosenDecision == 0) tossWinnerName else (if (winnerIndex == 0) team2Name else team1Name)
                            val bowling = if (batting == team1Name) team2Name else team1Name
                            append("🏏 $batting bats first\n🎯 $bowling bowls first")
                        }
                        "Kabaddi" -> {
                            val raider = if (chosenDecision == 0) tossWinnerName else (if (winnerIndex == 0) team2Name else team1Name)
                            append("⚡ $raider raids first!")
                        }
                        "Volleyball" -> {
                            val server = if (chosenDecision == 0) tossWinnerName else (if (winnerIndex == 0) team2Name else team1Name)
                            append("🏐 $server serves first!")
                        }
                    }
                }
                AlertDialog.Builder(this)
                    .setTitle("✅ Toss Result")
                    .setMessage(announcement)
                    .setPositiveButton("Let's Play! 🏆") { _, _ ->
                        // Now proceed to sport format dialog
                        when (sport) {
                            "Cricket"    -> askMatchFormatDialog()
                            "Kabaddi"    -> askKabaddiFormatDialog()
                            "Volleyball" -> askVolleyballFormatDialog()
                            else         -> loadMatchAndPlayers()
                        }
                    }
                    .setCancelable(false)
                    .show()
            }
            .setCancelable(false)
            .show()
    }

    // ── Match format dialog ──────────────────────────────────────────────────
    private fun askMatchFormatDialog() {
        val formatOptions = arrayOf(
            "T20 / IPL  (20 Overs, Single Innings)",
            "ODI  (50 Overs, Single Innings)",
            "Practice  (6 Overs, Single Innings)",
            "Custom Overs  (Single Innings)",
            "Test Match  (2 Innings each, Unlimited Overs)"  // ✅ NEW
        )
        var chosenFormat = 0

        AlertDialog.Builder(this)
            .setTitle("🏏 Select Match Format")
            .setSingleChoiceItems(formatOptions, 0) { _, i -> chosenFormat = i }
            .setPositiveButton("Next") { _, _ ->
                when (chosenFormat) {
                    0 -> { totalOvers = 20; isSingleInnings = true; isTestMatch = false; loadMatchAndPlayers() }
                    1 -> { totalOvers = 50; isSingleInnings = true; isTestMatch = false; loadMatchAndPlayers() }
                    2 -> { totalOvers = 6;  isSingleInnings = true; isTestMatch = false; loadMatchAndPlayers() }
                    3 -> askCustomOversDialog()
                    4 -> { isTestMatch = true; isSingleInnings = false; totalOvers = 999; loadMatchAndPlayers() } // ✅ Test
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun askCustomOversDialog() {
        val overOptions = arrayOf("5 Overs","8 Overs","10 Overs","15 Overs","20 Overs","50 Overs")
        val overValues  = intArrayOf(5, 8, 10, 15, 20, 50)
        var chosen = 2
        AlertDialog.Builder(this)
            .setTitle("🏏 How many overs?")
            .setSingleChoiceItems(overOptions, chosen) { _, i -> chosen = i }
            .setPositiveButton("Start") { _, _ ->
                totalOvers = overValues[chosen]; isSingleInnings = true; isTestMatch = false
                loadMatchAndPlayers()
            }
            .setCancelable(false).show()
    }

    private fun askKabaddiFormatDialog() {
        val options     = arrayOf("10 min halves","15 min halves","20 min halves (Official)","25 min halves")
        val halfValues  = intArrayOf(10, 15, 20, 25)
        val breakOpts   = arrayOf("3 min","5 min","10 min")
        val breakValues = intArrayOf(3, 5, 10)
        var chosenHalf = 2; var chosenBreak = 1

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL; setPadding(48, 16, 48, 16)
        }
        container.addView(android.widget.TextView(this).apply {
            text = "Half Duration"; textSize = 13f; setTextColor(ContextCompat.getColor(this@ScorerActivity, R.color.primary)); setPadding(0,0,0,8)
        })
        val halfList = android.widget.ListView(this).apply {
            adapter = android.widget.ArrayAdapter(this@ScorerActivity, android.R.layout.simple_list_item_single_choice, options)
            choiceMode = android.widget.ListView.CHOICE_MODE_SINGLE
            setItemChecked(chosenHalf, true)
            layoutParams = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 480)
            setOnItemClickListener { _, _, i, _ -> chosenHalf = i }
        }
        container.addView(halfList)
        container.addView(android.widget.TextView(this).apply {
            text = "Half-Time Break"; textSize = 13f; setTextColor(ContextCompat.getColor(this@ScorerActivity, R.color.primary)); setPadding(0,16,0,8)
        })
        val breakGroup = android.widget.RadioGroup(this).apply { orientation = android.widget.RadioGroup.HORIZONTAL }
        breakOpts.forEachIndexed { i, label ->
            breakGroup.addView(android.widget.RadioButton(this).apply { text = label; id = i; isChecked = (i == chosenBreak) })
        }
        breakGroup.setOnCheckedChangeListener { _, id -> chosenBreak = id }
        container.addView(breakGroup)

        AlertDialog.Builder(this)
            .setTitle("🤸 Kabaddi Format")
            .setView(android.widget.ScrollView(this).apply { addView(container) })
            .setPositiveButton("Start Match") { _, _ ->
                halfDurationMinutes = halfValues[chosenHalf]
                breakDurationMinutes = breakValues[chosenBreak]
                loadMatchAndPlayers()
            }
            .setCancelable(false).show()
    }

    private fun askVolleyballFormatDialog() {
        AlertDialog.Builder(this)
            .setTitle("🏐 Volleyball — Match Rules")
            .setMessage("Single Match:\n\n• First to 25 points wins\n• Must lead by at least 2 points\n• Deuce at 24–24: keep playing until 2-pt lead\n\nScore updates automatically after each point!")
            .setPositiveButton("Start Match") { _, _ -> loadMatchAndPlayers() }
            .setCancelable(false).show()
    }

    // ── Load match & players ─────────────────────────────────────────────────
    private fun loadMatchAndPlayers() {
        lifecycleScope.launch {
            val matches = repo.getMatchesForTournament(tournamentId).first()
            val match   = matches.firstOrNull { it.status == "scheduled" || it.status == "live" }
            if (match == null) { binding.tvNoMatch.visibility = View.VISIBLE; return@launch }

            matchId   = match.id
            team1Name = match.team1Name; team2Name = match.team2Name
            team1Id   = match.team1Id;   team2Id   = match.team2Id

            period = when {
                isTestMatch  -> "Test · Innings 1"
                sport == "Cricket" -> "Over 0/$totalOvers"
                sport == "Kabaddi" -> "1st Half · ${halfDurationMinutes}min"
                sport == "Volleyball" -> "Set 1  ·  0–0"
                else -> "1st Half"
            }

            val allPlayers = repo.getPlayersForTournamentNoIndex(tournamentId).first()
            team1Players   = allPlayers.filter { it.teamId == team1Id }.sortedByDescending { it.totalPoints }
            team2Players   = allPlayers.filter { it.teamId == team2Id }.sortedByDescending { it.totalPoints }

            // ✅ Auto-select first two batters if cricket and players exist
            if (sport == "Cricket") {
                val battingTeam = if (battingTeamIsTeam1) team1Players else team2Players
                striker    = battingTeam.getOrNull(0)
                nonStriker = battingTeam.getOrNull(1)
            }

            // Reset match stats for all players at start of match
            val allIds = (team1Players + team2Players).map { it.id }
            if (allIds.isNotEmpty()) launch { repo.resetMatchStats(allIds) }

            updateScoreDisplay()
            setupScoringButtons()
            updateBatterHighlights()  // ✅ highlight initial batters

            binding.btnPeriod.text = when {
                isTestMatch          -> "Innings 1"
                sport == "Kabaddi"   -> "⏸ End Half"
                sport == "Volleyball"-> "0–0"
                sport == "Cricket"   -> "Over 0/$totalOvers"
                else -> period
            }
            if (sport == "Kabaddi") startHalfTimer(isFirstHalf = true)
        }
    }

    // ── Scoring buttons ──────────────────────────────────────────────────────
    private fun setupScoringButtons() {
        binding.tvTeam1Name.text = team1Name
        binding.tvTeam2Name.text = team2Name
        val buttons = SportConfig.getScoringButtons(sport)
        binding.layoutTeam1Buttons.orientation = LinearLayout.VERTICAL
        binding.layoutTeam2Buttons.orientation = LinearLayout.VERTICAL
        binding.layoutTeam1Buttons.removeAllViews()
        binding.layoutTeam2Buttons.removeAllViews()
        if (sport == "Cricket") {
            val runsButtons  = buttons.filter { it.type in listOf("run1","run2","boundary4","six") }
            val extraButtons = buttons.filter { it.type in listOf("wide","noball","wicket") }
            addButtonRow(binding.layoutTeam1Buttons, runsButtons, true)
            addButtonRow(binding.layoutTeam1Buttons, extraButtons, true)
            addButtonRow(binding.layoutTeam2Buttons, runsButtons, false)
            addButtonRow(binding.layoutTeam2Buttons, extraButtons, false)
        } else {
            addButtonRow(binding.layoutTeam1Buttons, buttons, true)
            addButtonRow(binding.layoutTeam2Buttons, buttons, false)
        }
    }

    private fun addButtonRow(parent: LinearLayout, buttons: List<ScoreButton>, isTeam1: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = 8 }
        }
        buttons.forEach { btn -> row.addView(makeButton(btn, isTeam1)) }
        parent.addView(row)
    }

    private fun makeButton(btn: ScoreButton, isTeam1: Boolean): MaterialButton {
        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 4 }
        return MaterialButton(this).apply {
            layoutParams = params; text = btn.label; textSize = 11f; setPadding(4, 10, 4, 10)
            when (btn.type) {
                "wicket"              -> backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this@ScorerActivity, R.color.live_red))
                "wide","noball"       -> backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this@ScorerActivity, R.color.primary_dark))
                "six"                 -> backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF1565C0.toInt())  // bold blue for 6
                "boundary4"           -> backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D32.toInt())  // bold green for 4
                "allout","superraid"  -> backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF6A1B9A.toInt())  // purple
                else                  -> backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this@ScorerActivity, R.color.primary))
            }
            setOnClickListener {
                val teamName = if (isTeam1) team1Name else team2Name
                when (btn.type) {
                    "wide","noball" -> applyScore(btn, isTeam1, teamName, player = null)
                    "wicket" -> {
                        val batters = (if (isTeam1) team1Players else team2Players).filter { it.id !in outPlayerIds }
                        if (batters.isNotEmpty()) showWicketFlow(btn, isTeam1, teamName, batters)
                        else applyScore(btn, isTeam1, teamName, null)
                    }
                    else -> {
                        if (sport == "Cricket") {
                            // ✅ For cricket, only show striker + non-striker as options
                            val activeBatters = listOfNotNull(striker, nonStriker)
                            if (activeBatters.isNotEmpty()) showPlayerPicker(btn, isTeam1, teamName, activeBatters)
                            else applyScore(btn, isTeam1, teamName, null)
                        } else {
                            val players = if (isTeam1) team1Players else team2Players
                            if (players.isNotEmpty()) showPlayerPicker(btn, isTeam1, teamName, players)
                            else applyScore(btn, isTeam1, teamName, null)
                        }
                    }
                }
            }
        }
    }

    // ── Wicket flow ──────────────────────────────────────────────────────────
    private fun showWicketFlow(btn: ScoreButton, isTeam1: Boolean, teamName: String, batters: List<Player>) {
        // ✅ For cricket, only show striker & nonStriker as dismissal options
        val dismissible = if (sport == "Cricket") listOfNotNull(striker, nonStriker) else batters

        AlertDialog.Builder(this)
            .setTitle("🏏 WICKET! Who got out? ($teamName)")
            .setItems(dismissible.map { it.name }.toTypedArray()) { _, i ->
                val outBatter = dismissible[i]
                val fieldingTeam = if (isTeam1) team2Players else team1Players
                if (fieldingTeam.isNotEmpty()) {
                    val names = (listOf("Unknown / Run Out") + fieldingTeam.map { it.name }).toTypedArray()
                    AlertDialog.Builder(this)
                        .setTitle("Who took the wicket?")
                        .setItems(names) { _, j ->
                            val fielder = if (j == 0) null else fieldingTeam[j - 1]
                            applyWicket(btn, isTeam1, teamName, outBatter, fielder)
                        }.show()
                } else {
                    applyWicket(btn, isTeam1, teamName, outBatter, null)
                }
            }.show()
    }

    private fun applyWicket(btn: ScoreButton, isTeam1: Boolean, teamName: String, outBatter: Player, fielder: Player?) {
        outPlayerIds.add(outBatter.id)

        // ✅ After wicket: ask who comes in next (new batter)
        if (sport == "Cricket") {
            val allBatters = if (isTeam1) team1Players else team2Players
            val remaining = allBatters.filter { it.id !in outPlayerIds && it.id != striker?.id && it.id != nonStriker?.id }

            // Remove out batter from active pair
            if (outBatter.id == striker?.id) striker = null
            else if (outBatter.id == nonStriker?.id) nonStriker = null

            val fielderMsg = if (fielder != null) " · Caught by ${fielder.name}" else ""
            Toast.makeText(this, "🏏 OUT! ${outBatter.name}$fielderMsg", Toast.LENGTH_SHORT).show()

            if (fielder != null) {
                lifecycleScope.launch { repo.updatePlayerStats(fielder.id, pointsToAdd = 0, wicketsToAdd = 1) }
            }

            applyScore(btn, isTeam1, teamName, player = null)

            // ✅ Ask who comes in next
            if (remaining.isNotEmpty()) {
                val names = remaining.map { it.name }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle("🏏 New Batter — Who comes in?")
                    .setItems(names) { _, idx ->
                        val newBatter = remaining[idx]
                        // Fill the empty slot
                        if (striker == null) striker = newBatter else nonStriker = newBatter
                        updateBatterHighlights()
                        Toast.makeText(this, "✅ ${newBatter.name} is now batting", Toast.LENGTH_SHORT).show()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                updateBatterHighlights()
            }
        } else {
            val fielderMsg = if (fielder != null) " · Caught by ${fielder.name}" else ""
            Toast.makeText(this, "🏏 OUT! ${outBatter.name}$fielderMsg", Toast.LENGTH_SHORT).show()
            if (fielder != null) {
                lifecycleScope.launch { repo.updatePlayerStats(fielder.id, pointsToAdd = 0, wicketsToAdd = 1) }
            }
            applyScore(btn, isTeam1, teamName, player = null)
        }
    }

    // ✅ Highlight active batters in player picker + show dimmed for rest
    private fun showPlayerPicker(btn: ScoreButton, isTeam1: Boolean, teamName: String, players: List<Player>) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("${btn.label} · Who scored for $teamName?")
            .setNegativeButton("Skip") { _, _ -> applyScore(btn, isTeam1, teamName, null) }
            .create()
        dialog.setView(buildPlayerListView(players) { selected ->
            dialog.dismiss()
            // ✅ After odd runs (1,3), swap striker/nonStriker (they cross)
            if (sport == "Cricket" && btn.points % 2 == 1) {
                val temp = striker; striker = nonStriker; nonStriker = temp
                updateBatterHighlights()
            }
            applyScore(btn, isTeam1, teamName, selected)
        })
        dialog.show()
    }

    private fun buildPlayerListView(players: List<Player>, onPick: (Player) -> Unit): android.widget.ScrollView {
        val scroll = android.widget.ScrollView(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 8, 0, 8) }

        // ── Dark scoreboard palette — same dark navy background as the rest of the UI ──
        val bgDefault  = ContextCompat.getColor(this, R.color.secondary)        // #1A1A2E navy
        val bgStriker  = ContextCompat.getColor(this, R.color.accent)           // #FFD700 gold  → striker row
        val bgNonStr   = ContextCompat.getColor(this, R.color.primary_dark)     // #CC5500 dark-orange → non-striker row
        val bgOut      = ContextCompat.getColor(this, R.color.score_bg)         // #1A1A2E dimmed
        val bgYetToBat = ContextCompat.getColor(this, R.color.secondary)        // same navy

        val txtStriker  = ContextCompat.getColor(this, R.color.black)           // black on gold — high contrast
        val txtNonStr   = ContextCompat.getColor(this, R.color.white)           // white on dark-orange
        val txtOut      = ContextCompat.getColor(this, R.color.text_secondary)  // #666 dimmed
        val txtDefault  = ContextCompat.getColor(this, R.color.white)           // white on navy

        val dividerColor = ContextCompat.getColor(this, R.color.primary)        // orange divider line

        players.forEach { player ->
            val isOut     = player.id in outPlayerIds
            val isStriker = sport == "Cricket" && player.id == striker?.id
            val isNonStr  = sport == "Cricket" && player.id == nonStriker?.id
            val isActive  = isStriker || isNonStr

            val label = when {
                isOut      -> "❌  ${player.name}  (OUT)"
                isStriker  -> "🏏  ${player.name}  ← STRIKER"
                isNonStr   -> "🏃  ${player.name}  (non-striker)"
                sport == "Cricket" -> "   ${player.name}  (yet to bat)"
                else       -> "   ${player.name}"
            }
            val bgColor   = when { isOut -> bgOut; isStriker -> bgStriker; isNonStr -> bgNonStr; else -> bgDefault }
            val textColor = when { isOut -> txtOut; isStriker -> txtStriker; isNonStr -> txtNonStr; else -> txtDefault }

            col.addView(android.widget.TextView(this).apply {
                text = label; textSize = 16f; typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(48, 30, 48, 30)
                setTextColor(textColor)
                setBackgroundColor(bgColor)
                alpha = if (isOut) 0.35f else if (!isActive && sport == "Cricket") 0.55f else 1f
                isEnabled = !isOut && (sport != "Cricket" || isActive)
                isClickable = isEnabled; isFocusable = isEnabled
                if (isEnabled) setOnClickListener { onPick(player) }
            })
            // Divider
            col.addView(android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
                setBackgroundColor(dividerColor)
            })
        }
        scroll.setBackgroundColor(bgDefault)
        scroll.addView(col); return scroll
    }

    // ✅ Update the period label to show current batters
    private fun updateBatterHighlights() {
        if (sport != "Cricket") return
        val s = striker?.name ?: "—"
        val n = nonStriker?.name ?: "—"
        val batterLine = "🏏 $s  |  $n"
        binding.tvPeriod.text = "$period\n$batterLine"
    }

    // ── Apply score ──────────────────────────────────────────────────────────
    private fun applyScore(btn: ScoreButton, isTeam1: Boolean, teamName: String, player: Player?) {
        scoreHistory.addLast(ScoreSnapshot(
            team1Score, team2Score, team1Wickets, team2Wickets,
            team1Balls, team1Overs, team2Balls, team2Overs,
            currentInning, "${btn.label} for $teamName"
        ))
        val isCricket = sport == "Cricket"

        when {
            btn.type == "wicket" -> {
                val currentWickets = if (isTeam1) team1Wickets else team2Wickets
                if (currentWickets >= 10) { Toast.makeText(this,"❌ ALL OUT!",Toast.LENGTH_LONG).show(); updateLiveScore(); return }
                if (isTeam1) team1Wickets++ else team2Wickets++
                val newW = if (isTeam1) team1Wickets else team2Wickets
                if (player != null) lifecycleScope.launch { repo.updatePlayerStats(player.id, 0, wicketsToAdd = 1) }
                if (isCricket) {
                    advanceBall(isTeam1)
                    if (newW >= 10) {
                        updateLiveScore(); disableTeamButtons(isTeam1)
                        AlertDialog.Builder(this).setTitle("🏏 ALL OUT! $teamName")
                            .setMessage("$teamName ALL OUT for ${if (isTeam1) team1Score else team2Score} runs!")
                            .setPositiveButton("OK", null).setCancelable(false).show()
                        checkTestInningsEnd(isTeam1); return
                    }
                }
            }
            btn.type == "wide" || btn.type == "noball" -> {
                if (isTeam1) team1Score += btn.points else team2Score += btn.points
                Toast.makeText(this, "${if (btn.type=="wide") "Wide ⚡" else "No Ball ⚡"} +1 for $teamName", Toast.LENGTH_SHORT).show()
            }
            else -> {
                if (isTeam1) { team1Score += btn.points; if (sport=="Volleyball") team1SetScore += btn.points }
                else          { team2Score += btn.points; if (sport=="Volleyball") team2SetScore += btn.points }
                if (isCricket) advanceBall(isTeam1)
                if (sport == "Volleyball") {
                    period = "$team1SetScore–$team2SetScore"
                    binding.tvPeriod.text = period; binding.btnPeriod.text = period
                    checkVolleyballMatchWin(isTeam1)
                }
                val msg = when(btn.type) {
                    "six" -> "🎉 SIX! ${player?.name ?: teamName} +6"
                    "boundary4" -> "🔥 FOUR! ${player?.name ?: teamName} +4"
                    "allout" -> "💥 ALL OUT! +2 for $teamName"
                    "superraid" -> "⚡ SUPER RAID! +3 for $teamName"
                    else -> "${player?.name ?: teamName}: +${btn.points}"
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                if (player != null && btn.points > 0) {
                    scoredPlayerIds.add(player.id)
                    lifecycleScope.launch {
                        when {
                            sport=="Kabaddi" && btn.type in listOf("raid","superraid","bonus") ->
                                repo.updatePlayerStats(player.id, btn.points, raidPointsToAdd = btn.points)
                            sport=="Kabaddi" && btn.type in listOf("tackle","allout") ->
                                repo.updatePlayerStats(player.id, btn.points, tacklePointsToAdd = btn.points)
                            sport=="Volleyball" && btn.type == "ace" ->
                                repo.updatePlayerStats(player.id, btn.points, acePointsToAdd = btn.points)
                            sport=="Volleyball" && btn.type == "block" ->
                                repo.updatePlayerStats(player.id, btn.points, blockPointsToAdd = btn.points)
                            else -> repo.updatePlayerStats(player.id, btn.points)
                        }
                    }
                }
            }
        }
        updateLiveScore()
    }

    // ── Over advance ─────────────────────────────────────────────────────────
    private fun advanceBall(isTeam1: Boolean) {
        if (isTestMatch) {
            // Test: no over limit — just track balls/overs for display
            if (isTeam1) { team1Balls++; if (team1Balls==6) { team1Balls=0; team1Overs++ } }
            else          { team2Balls++; if (team2Balls==6) { team2Balls=0; team2Overs++ } }
            val t1 = if (team1Balls==0) "$team1Overs" else "$team1Overs.$team1Balls"
            val t2 = if (team2Balls==0) "$team2Overs" else "$team2Overs.$team2Balls"
            period = "Test · Inn $testInnings  |  $team1Name: $t1 ov  $team2Name: $t2 ov"
            binding.tvPeriod.text = period; binding.btnPeriod.text = "Inn $testInnings"
            return
        }

        if (isTeam1) {
            team1Balls++
            if (team1Balls == 6) {
                team1Balls = 0; team1Overs++
                if (team1Overs >= totalOvers) {
                    Toast.makeText(this, "✅ ${team1Name} innings complete! ${team1Score}/${team1Wickets}w", Toast.LENGTH_LONG).show()
                    disableTeamButtons(true)
                    if (team2Overs >= totalOvers || team2Wickets >= 10) matchOver()
                } else {
                    // End of over: non-striker becomes striker (they crossed on last ball)
                    swapStrikerForNewOver(team1Name, team1Overs)
                }
            }
        } else {
            team2Balls++
            if (team2Balls == 6) {
                team2Balls = 0; team2Overs++
                if (team2Overs >= totalOvers) {
                    Toast.makeText(this, "✅ ${team2Name} innings complete! ${team2Score}/${team2Wickets}w", Toast.LENGTH_LONG).show()
                    disableTeamButtons(false)
                    if (team1Overs >= totalOvers || team1Wickets >= 10) matchOver()
                } else {
                    // End of over: non-striker becomes striker (they crossed on last ball)
                    swapStrikerForNewOver(team2Name, team2Overs)
                }
            }
        }
        val t1 = if (team1Balls==0) "$team1Overs" else "$team1Overs.$team1Balls"
        val t2 = if (team2Balls==0) "$team2Overs" else "$team2Overs.$team2Balls"
        period = "${team1Name}: $t1  |  ${team2Name}: $t2  / $totalOvers ov"
        binding.tvPeriod.text = period; binding.btnPeriod.text = "$t1 | $t2"
        updateBatterHighlights()
    }

    // ── End-of-over striker swap (like real cricket) ─────────────────────────
    // At the end of every over the non-striker becomes the striker for the next over.
    // This mirrors real cricket — both batters crossed to complete the last delivery.
    private fun swapStrikerForNewOver(teamName: String, overNumber: Int) {
        val temp   = striker
        striker    = nonStriker
        nonStriker = temp
        updateBatterHighlights()
        Toast.makeText(this, "🔄 Over $overNumber done — ${striker?.name ?: "?"} faces now", Toast.LENGTH_SHORT).show()
    }

    // ✅ Test match innings end — ask if next innings should start
    private fun checkTestInningsEnd(isTeam1: Boolean) {
        if (!isTestMatch) return
        val score  = if (isTeam1) team1Score else team2Score
        val wickets = if (isTeam1) team1Wickets else team2Wickets
        val teamName = if (isTeam1) team1Name else team2Name

        if (testInnings < 4) {
            AlertDialog.Builder(this)
                .setTitle("🏏 Innings $testInnings Over")
                .setMessage("$teamName: $score/$wickets\n\nDo you want to start Innings ${testInnings + 1}?")
                .setPositiveButton("Start Next Innings") { _, _ ->
                    startNextTestInnings()
                }
                .setNegativeButton("End Match") { _, _ -> showEndMatchDialog() }
                .setCancelable(false).show()
        } else {
            showEndMatchDialog()
        }
    }

    private fun startNextTestInnings() {
        // Save current innings scores
        if (testInnings % 2 == 1) {
            testTeam1Innings.add(Pair(team1Score, team1Wickets))
            team1Score = 0; team1Wickets = 0; team1Balls = 0; team1Overs = 0
        } else {
            testTeam2Innings.add(Pair(team2Score, team2Wickets))
            team2Score = 0; team2Wickets = 0; team2Balls = 0; team2Overs = 0
        }
        testInnings++
        outPlayerIds.clear()

        // Swap batting team — reset active batters
        val newBattingTeam = if (battingTeamIsTeam1) team1Players else team2Players
        striker    = newBattingTeam.getOrNull(0)
        nonStriker = newBattingTeam.getOrNull(1)

        // Re-enable buttons for the new batting team
        val newIsTeam1 = battingTeamIsTeam1
        val layout = if (newIsTeam1) binding.layoutTeam1Buttons else binding.layoutTeam2Buttons
        for (i in 0 until layout.childCount) {
            val row = layout.getChildAt(i)
            if (row is LinearLayout) for (j in 0 until row.childCount) { row.getChildAt(j).isEnabled = true; row.getChildAt(j).alpha = 1f }
        }

        period = "Test · Inn $testInnings"
        binding.tvPeriod.text = period; binding.btnPeriod.text = "Inn $testInnings"
        updateScoreDisplay(); updateBatterHighlights()
        Toast.makeText(this, "🏏 Innings $testInnings — ${if (newIsTeam1) team1Name else team2Name} batting", Toast.LENGTH_LONG).show()
    }

    private fun disableTeamButtons(isTeam1: Boolean) {
        val layout = if (isTeam1) binding.layoutTeam1Buttons else binding.layoutTeam2Buttons
        for (i in 0 until layout.childCount) {
            val row = layout.getChildAt(i)
            if (row is LinearLayout) for (j in 0 until row.childCount) { row.getChildAt(j).isEnabled = false; row.getChildAt(j).alpha = 0.35f }
        }
    }

    private fun matchOver() {
        period = "Match Over"; binding.tvPeriod.text = period; binding.btnPeriod.text = period
        Toast.makeText(this, "🏁 All overs done! Tap END MATCH", Toast.LENGTH_LONG).show()
    }

    private fun undoLastScore() {
        if (scoreHistory.isEmpty()) { Toast.makeText(this,"Nothing to undo!",Toast.LENGTH_SHORT).show(); return }
        val last = scoreHistory.removeLast()
        team1Score=last.team1Score; team2Score=last.team2Score
        team1Wickets=last.team1Wickets; team2Wickets=last.team2Wickets
        team1Balls=last.team1Balls; team1Overs=last.team1Overs
        team2Balls=last.team2Balls; team2Overs=last.team2Overs
        currentInning=last.inning
        if (sport=="Cricket") {
            val t1 = if(team1Balls==0)"$team1Overs" else "$team1Overs.$team1Balls"
            val t2 = if(team2Balls==0)"$team2Overs" else "$team2Overs.$team2Balls"
            period = if (isTestMatch) "Test · Inn $testInnings  |  $team1Name: $t1  $team2Name: $t2"
                     else "${team1Name}: $t1  |  ${team2Name}: $t2  / $totalOvers ov"
        }
        updateLiveScore(); updateBatterHighlights()
        Toast.makeText(this,"↩ Undid: ${last.label}",Toast.LENGTH_SHORT).show()
    }

    private fun showEndMatchDialog() {
        val winner = when {
            team1Score > team2Score -> team1Name
            team2Score > team1Score -> team2Name
            else -> "Draw"
        }
        val ctx = this
        val container = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(24,16,24,8) }

        // Test match summary
        val summary = if (isTestMatch) {
            buildString {
                appendLine("🏏 TEST MATCH SUMMARY")
                testTeam1Innings.forEachIndexed { i, (s,w) -> appendLine("$team1Name Inn ${i*2+1}: $s/$w") }
                testTeam2Innings.forEachIndexed { i, (s,w) -> appendLine("$team2Name Inn ${i*2+2}: $s/$w") }
                appendLine("Current: $team1Name $team1Score | $team2Name $team2Score")
                appendLine("\nWinner: $winner")
            }
        } else {
            "🏆 $winner wins!\n\n$team1Name: $team1Score\n$team2Name: $team2Score"
        }

        container.addView(android.widget.TextView(ctx).apply {
            text = summary; textSize = 14f
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            setPadding(0,0,0,16)
        })
        container.addView(android.widget.TextView(ctx).apply {
            text = "Man of the Match:"; textSize=13f
            setTextColor(ContextCompat.getColor(ctx, R.color.accent))
        })
        val allPlayers = team1Players + team2Players
        val playerNames = (listOf("None") + allPlayers.map { it.name }).toTypedArray()
        var selectedMom = winner; var momIndex = 0
        container.addView(android.widget.Spinner(ctx).apply {
            adapter = android.widget.ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, playerNames)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.topMargin=4 }
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: android.view.View?, i: Int, id: Long) { momIndex=i; selectedMom=if(i==0) winner else allPlayers[i-1].name }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            }
        })

        AlertDialog.Builder(this).setTitle("🏁 Match Summary").setView(container)
            .setPositiveButton("End Match") { _, _ ->
                val momPlayer = if (momIndex > 0) allPlayers[momIndex-1] else null
                if (momPlayer != null) lifecycleScope.launch { repo.updatePlayerStats(momPlayer.id, 0, isManOfMatch=true) }
                lifecycleScope.launch { repo.markMatchPlayed(scoredPlayerIds.toList()) }
                repo.updateLiveScore(LiveScore(matchId=matchId, tournamentId=tournamentId,
                    team1Score=team1Score, team2Score=team2Score, team1Name=team1Name, team2Name=team2Name,
                    sport=sport, period="Full Time", isLive=false, manOfMatch=selectedMom,
                    tossWinner=tossWinnerName, tossDecision=tossDecision,
                    lastUpdated=System.currentTimeMillis()))
                startActivity(Intent(this, MatchResultActivity::class.java).apply {
                    putExtra("TEAM1_NAME",team1Name); putExtra("TEAM2_NAME",team2Name)
                    putExtra("TEAM1_SCORE",team1Score); putExtra("TEAM2_SCORE",team2Score)
                    putExtra("SPORT",sport); putExtra("TEAM1_WICKETS",team1Wickets); putExtra("TEAM2_WICKETS",team2Wickets)
                    putExtra("MAN_OF_MATCH",selectedMom)
                })
                finish()
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun updateLiveScore() {
        updateScoreDisplay()
        val t1Ov = if (team1Balls == 0) "$team1Overs.0" else "$team1Overs.$team1Balls"
        val t2Ov = if (team2Balls == 0) "$team2Overs.0" else "$team2Overs.$team2Balls"
        repo.updateLiveScore(LiveScore(
            matchId       = matchId,
            tournamentId  = tournamentId,
            team1Score    = team1Score,
            team2Score    = team2Score,
            team1Name     = team1Name,
            team2Name     = team2Name,
            sport         = sport,
            period        = period,
            isLive        = true,
            lastUpdated   = System.currentTimeMillis(),
            // Toss
            tossWinner    = tossWinnerName,
            tossDecision  = tossDecision,
            // Cricket live batting
            striker       = striker?.name ?: "",
            nonStriker    = nonStriker?.name ?: "",
            team1Wickets  = team1Wickets,
            team2Wickets  = team2Wickets,
            team1Overs    = t1Ov,
            team2Overs    = t2Ov
        ))
    }

    private fun updateScoreDisplay() {
        when (sport) {
            "Cricket" -> {
                val t1Ov = if(team1Balls==0)"$team1Overs.0" else "$team1Overs.$team1Balls"
                val t2Ov = if(team2Balls==0)"$team2Overs.0" else "$team2Overs.$team2Balls"
                val ovSuffix = if (isTestMatch) " ov" else "/$totalOvers ov"
                binding.tvTeam1Score.text = "$team1Score/${team1Wickets}w\n$t1Ov$ovSuffix"
                binding.tvTeam2Score.text = "$team2Score/${team2Wickets}w\n$t2Ov$ovSuffix"
            }
            "Volleyball" -> { binding.tvTeam1Score.text = team1SetScore.toString(); binding.tvTeam2Score.text = team2SetScore.toString() }
            else -> { binding.tvTeam1Score.text = team1Score.toString(); binding.tvTeam2Score.text = team2Score.toString() }
        }
        binding.tvPeriod.text = period; binding.tvTeam1Name.text = team1Name
        binding.tvTeam2Name.text = team2Name; binding.tvSport.text = sport
    }

    private fun togglePeriod() {
        when (sport) {
            "Cricket"    -> { /* auto managed */ }
            "Volleyball" -> Toast.makeText(this,"Points tracked automatically!",Toast.LENGTH_SHORT).show()
            "Kabaddi"    -> {
                when {
                    period.startsWith("1st Half") -> { stopKabaddiTimer(); period="Half Time"; binding.tvPeriod.text=period; binding.btnPeriod.text="▶ Start 2nd Half"; updateLiveScore(); startBreakTimer() }
                    period=="Half Time"||period.startsWith("Break") -> { stopKabaddiTimer(); period="2nd Half · ${halfDurationMinutes}:00"; binding.tvPeriod.text=period; binding.btnPeriod.text="⏸ End Half"; updateLiveScore(); startHalfTimer(false) }
                    period.startsWith("2nd Half") -> { stopKabaddiTimer(); period="Full Time"; binding.tvPeriod.text=period; binding.btnPeriod.text="Full Time"; updateLiveScore() }
                    else -> { period="1st Half · ${halfDurationMinutes}:00"; binding.tvPeriod.text=period; startHalfTimer(true) }
                }
            }
        }
    }

    private fun checkVolleyballMatchWin(isTeam1: Boolean) {
        val s1=team1SetScore; val s2=team2SetScore; val lead=kotlin.math.abs(s1-s2)
        if ((s1>=25||s2>=25)&&lead>=2) { volleyballMatchWon(if(s1>s2)team1Name else team2Name,s1,s2); return }
        if (s1>=24&&s2>=24&&s1==s2) Toast.makeText(this,"⚡ Deuce! Need 2-pt lead!",Toast.LENGTH_SHORT).show()
        if (s1>=24&&s2>=24&&lead>=2) volleyballMatchWon(if(s1>s2)team1Name else team2Name,s1,s2)
    }

    private fun volleyballMatchWon(winner: String, s1: Int, s2: Int) {
        period="Match Over · $winner wins! ($s1–$s2)"; binding.tvPeriod.text=period; binding.btnPeriod.text="Match Over"
        Toast.makeText(this,"🏆 $winner wins $s1–$s2!",Toast.LENGTH_LONG).show(); updateLiveScore()
        AlertDialog.Builder(this).setTitle("🏐 Match Over!").setMessage("$winner wins!\n\nFinal Score: $s1–$s2\n\nTap END MATCH to save.").setPositiveButton("OK",null).show()
    }

    private fun startHalfTimer(isFirstHalf: Boolean) {
        stopKabaddiTimer()
        val totalMs = halfDurationMinutes*60*1000L; timeLeftMillis=totalMs; timerRunning=true
        val halfLabel = if(isFirstHalf)"1st Half" else "2nd Half"
        countDownTimer = object : CountDownTimer(totalMs,1000) {
            override fun onTick(ms: Long) {
                timeLeftMillis=ms; val mins=(ms/1000)/60; val secs=(ms/1000)%60
                period="$halfLabel · %02d:%02d".format(mins,secs); binding.tvPeriod.text=period
                if(ms<=60_000) binding.tvPeriod.setTextColor(ContextCompat.getColor(this@ScorerActivity, R.color.live_red)) else binding.tvPeriod.setTextColor(ContextCompat.getColor(this@ScorerActivity, R.color.white))
            }
            override fun onFinish() {
                timerRunning=false; binding.tvPeriod.setTextColor(ContextCompat.getColor(this@ScorerActivity, R.color.white))
                if(isFirstHalf) {
                    period="Half Time"; binding.tvPeriod.text="⏸ HALF TIME!"; binding.btnPeriod.text="▶ Start 2nd Half"; updateLiveScore()
                    AlertDialog.Builder(this@ScorerActivity).setTitle("⏸ Half Time!").setMessage("1st Half is over!\n\nBreak: $breakDurationMinutes minutes\n\nTap '▶ Start 2nd Half' when ready.")
                        .setPositiveButton("OK"){_,_->startBreakTimer()}.setCancelable(false).show()
                } else {
                    period="Full Time"; binding.tvPeriod.text="🏁 FULL TIME!"; binding.btnPeriod.text="Full Time"; updateLiveScore()
                    AlertDialog.Builder(this@ScorerActivity).setTitle("🏁 Full Time!").setMessage("Match over!\n\nTap END MATCH to save.")
                        .setPositiveButton("OK",null).setCancelable(false).show()
                }
            }
        }.start(); binding.btnPeriod.text="⏸ End Half"
    }

    private fun startBreakTimer() {
        stopKabaddiTimer()
        countDownTimer = object : CountDownTimer(breakDurationMinutes*60*1000L,1000) {
            override fun onTick(ms: Long) { val m=(ms/1000)/60; val s=(ms/1000)%60; binding.tvPeriod.text="Break · %02d:%02d".format(m,s); binding.btnPeriod.text="▶ Start 2nd Half" }
            override fun onFinish() { binding.tvPeriod.text="Break Over! Tap Start 2nd Half"; binding.btnPeriod.text="▶ Start 2nd Half"; Toast.makeText(this@ScorerActivity,"⏰ Break over! Tap to start 2nd Half",Toast.LENGTH_LONG).show() }
        }.start()
    }

    private fun stopKabaddiTimer() { countDownTimer?.cancel(); countDownTimer=null; timerRunning=false; binding.tvPeriod.setTextColor(ContextCompat.getColor(this@ScorerActivity, R.color.white)) }
    override fun onDestroy() { super.onDestroy(); stopKabaddiTimer() }

    private fun shareScorecard() {
        val text = buildString {
            appendLine("🏆 GRAMA KALYANA SPORTS"); appendLine("Sport: $sport | $period")
            if (sport=="Cricket") {
                val t1Ov=if(team1Balls==0)"$team1Overs.0" else "$team1Overs.$team1Balls"
                val t2Ov=if(team2Balls==0)"$team2Overs.0" else "$team2Overs.$team2Balls"
                appendLine("$team1Name: $team1Score/${team1Wickets}w ($t1Ov ov)")
                appendLine("$team2Name: $team2Score/${team2Wickets}w ($t2Ov ov)")
                if (striker!=null) appendLine("Batting: ${striker?.name} & ${nonStriker?.name}")
            } else appendLine("$team1Name  $team1Score  —  $team2Score  $team2Name")
            appendLine("Shared via Grama-Kalyana Sports App")
        }
        val i = Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT,text); setPackage("com.whatsapp") }
        try { startActivity(i) } catch (e: Exception) { i.setPackage(null); startActivity(Intent.createChooser(i,"Share")) }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
