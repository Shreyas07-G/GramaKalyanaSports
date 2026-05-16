package com.gramakalyana.sports.data.models

data class Tournament(
    val id: String = "",
    val name: String = "",
    val sport: String = "Kabaddi",
    val location: String = "",
    val adminId: String = "",
    val status: String = "upcoming",
    val createdAt: Long = System.currentTimeMillis()
)

data class Team(
    val id: String = "",
    val tournamentId: String = "",
    val name: String = "",
    val players: List<Player> = emptyList()
)

data class Player(
    val id: String = "",
    val name: String = "",
    val teamId: String = "",
    val tournamentId: String = "",
    // ── Career stats (accumulate across all matches) ──────────────────────
    val totalPoints: Int = 0,
    val wicketsTaken: Int = 0,
    val raidPoints: Int = 0,
    val tacklePoints: Int = 0,
    val acePoints: Int = 0,
    val blockPoints: Int = 0,
    val matchesPlayed: Int = 0,
    val manOfMatchCount: Int = 0,
    // ── Current match stats (reset each match) ────────────────────────────
    val matchPoints: Int = 0,
    val matchWickets: Int = 0,
    val matchRaidPoints: Int = 0,
    val matchTacklePoints: Int = 0,
    val matchAcePoints: Int = 0,
    val matchBlockPoints: Int = 0
)

data class Match(
    val id: String = "",
    val tournamentId: String = "",
    val team1Id: String = "",
    val team1Name: String = "",
    val team2Id: String = "",
    val team2Name: String = "",
    val scheduledAt: Long = 0L,
    val status: String = "scheduled",
    val scorerId: String = ""
)

data class LiveScore(
    val matchId: String = "",
    val tournamentId: String = "",
    val team1Score: Int = 0,
    val team2Score: Int = 0,
    val team1Name: String = "",
    val team2Name: String = "",
    val sport: String = "",
    val period: String = "1st Half",
    val isLive: Boolean = false,
    val manOfMatch: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    // ── Toss ──────────────────────────────────────────
    val tossWinner: String = "",
    val tossDecision: String = "",
    // ── Cricket live batting ───────────────────────────
    val striker: String = "",
    val nonStriker: String = "",
    val team1Wickets: Int = 0,
    val team2Wickets: Int = 0,
    val team1Overs: String = "",
    val team2Overs: String = "",
    // ── Kabaddi raider ────────────────────────────────
    val currentRaider: String = "",
    // ── Volleyball server ─────────────────────────────
    val currentServer: String = ""
)

data class ScoreEvent(
    val matchId: String = "",
    val teamId: String = "",
    val playerId: String = "",
    val playerName: String = "",
    val eventType: String = "",
    val points: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

object SportConfig {
    fun getScoringButtons(sport: String): List<ScoreButton> = when (sport) {
        "Kabaddi" -> listOf(
            ScoreButton("Raid\nPoint", 1, "raid"),
            ScoreButton("Tackle\nPoint", 1, "tackle"),
            ScoreButton("All Out\n+2", 2, "allout"),
            ScoreButton("Super\nRaid +3", 3, "superraid"),
            ScoreButton("Bonus\nPoint", 1, "bonus")
        )
        "Volleyball" -> listOf(
            ScoreButton("Point", 1, "point"),
            ScoreButton("Ace\nServe", 1, "ace"),
            ScoreButton("Block\nPoint", 1, "block")
        )
        "Cricket" -> listOf(
            ScoreButton("1 Run", 1, "run1"),
            ScoreButton("2 Runs", 2, "run2"),
            ScoreButton("4 Runs", 4, "boundary4"),
            ScoreButton("6 Runs", 6, "six"),
            ScoreButton("Wide", 1, "wide"),
            ScoreButton("No Ball", 1, "noball"),
            ScoreButton("WICKET\n🏏 OUT", 0, "wicket")
        )
        else -> listOf(ScoreButton("Point", 1, "point"))
    }
}

data class ScoreButton(
    val label: String,
    val points: Int,
    val type: String
)

data class ScoreSnapshot(
    val team1Score: Int,
    val team2Score: Int,
    val team1Wickets: Int,
    val team2Wickets: Int,
    val team1Balls: Int,
    val team1Overs: Int,
    val team2Balls: Int,
    val team2Overs: Int,
    val inning: Int,
    val label: String
)
