package com.gramakalyana.sports.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.gramakalyana.sports.data.models.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference

    // ── AUTH ──────────────────────────────────────────────────────────────────

    suspend fun loginAdmin(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerAdmin(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserId() = auth.currentUser?.uid
    fun isLoggedIn() = auth.currentUser != null
    fun logout() = auth.signOut()

    // ── TOURNAMENTS ────────────────────────────────────────────────────────────

    suspend fun createTournament(tournament: Tournament): Result<String> {
        return try {
            val doc = firestore.collection("tournaments").document()
            val t = tournament.copy(id = doc.id, adminId = getCurrentUserId() ?: "")
            doc.set(t).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getTournaments(): Flow<List<Tournament>> = callbackFlow {
        val listener = firestore.collection("tournaments")
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { it.toObject(Tournament::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── TEAMS ─────────────────────────────────────────────────────────────────

    suspend fun addTeam(team: Team): Result<String> {
        return try {
            val doc = firestore.collection("teams").document()
            val t = team.copy(id = doc.id)
            doc.set(t).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getTeamsForTournament(tournamentId: String): Flow<List<Team>> = callbackFlow {
        val listener = firestore.collection("teams")
            .whereEqualTo("tournamentId", tournamentId)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { it.toObject(Team::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── MATCHES ───────────────────────────────────────────────────────────────

    suspend fun createMatch(match: Match): Result<String> {
        return try {
            val doc = firestore.collection("matches").document()
            val m = match.copy(id = doc.id)
            doc.set(m).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMatchesForTournament(tournamentId: String): Flow<List<Match>> = callbackFlow {
        val listener = firestore.collection("matches")
            .whereEqualTo("tournamentId", tournamentId)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { it.toObject(Match::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getAllMatches(): Flow<List<Match>> = callbackFlow {
        val listener = firestore.collection("matches")
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { it.toObject(Match::class.java) }
                    ?.sortedBy { it.scheduledAt } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // No orderBy — avoids composite Firestore index requirement. Sort in-memory.
    fun getAllPlayers(): Flow<List<Player>> = callbackFlow {
        val listener = firestore.collection("players")
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(Player::class.java) }
                    ?.sortedByDescending { it.totalPoints }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── LIVE SCORES (Realtime DB) ──────────────────────────────────────────────

    fun updateLiveScore(score: LiveScore) {
        realtimeDb.child("live_scores").child(score.matchId).setValue(score)
    }

    fun getLiveScore(matchId: String): Flow<LiveScore?> = callbackFlow {
        val ref = realtimeDb.child("live_scores").child(matchId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val score = snapshot.getValue(LiveScore::class.java)
                trySend(score)
            }
            override fun onCancelled(error: DatabaseError) { trySend(null) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getAllLiveScores(): Flow<List<LiveScore>> = callbackFlow {
        val ref = realtimeDb.child("live_scores")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(LiveScore::class.java) }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { trySend(emptyList()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun logScoreEvent(event: ScoreEvent) {
        realtimeDb.child("score_events").child(event.matchId).push().setValue(event)
    }

    // ── PLAYER STATS ──────────────────────────────────────────────────────────

    /**
     * Add points to a player. Also increments matchesPlayed when isManOfMatch = true
     * (called once at end of match per player who scored).
     * FIX: matchesPlayed now increments correctly — call markMatchPlayed() at end of match
     * for all players who participated.
     */
    suspend fun updatePlayerStats(
        playerId: String,
        pointsToAdd: Int,
        isManOfMatch: Boolean = false,
        wicketsToAdd: Int = 0,
        raidPointsToAdd: Int = 0,
        tacklePointsToAdd: Int = 0,
        acePointsToAdd: Int = 0,
        blockPointsToAdd: Int = 0
    ) {
        try {
            val ref = firestore.collection("players").document(playerId)
            firestore.runTransaction { tx ->
                val snap    = tx.get(ref)
                val current = snap.getLong("totalPoints")?.toInt() ?: 0
                val wickets = snap.getLong("wicketsTaken")?.toInt() ?: 0
                val raids   = snap.getLong("raidPoints")?.toInt() ?: 0
                val tackles = snap.getLong("tacklePoints")?.toInt() ?: 0
                val aces    = snap.getLong("acePoints")?.toInt() ?: 0
                val blocks  = snap.getLong("blockPoints")?.toInt() ?: 0
                val matches = snap.getLong("matchesPlayed")?.toInt() ?: 0
                val mom     = snap.getLong("manOfMatchCount")?.toInt() ?: 0
                // Current match stats (accumulate within this match)
                val mPts    = snap.getLong("matchPoints")?.toInt() ?: 0
                val mWkt    = snap.getLong("matchWickets")?.toInt() ?: 0
                val mRaid   = snap.getLong("matchRaidPoints")?.toInt() ?: 0
                val mTackle = snap.getLong("matchTacklePoints")?.toInt() ?: 0
                val mAce    = snap.getLong("matchAcePoints")?.toInt() ?: 0
                val mBlock  = snap.getLong("matchBlockPoints")?.toInt() ?: 0
                tx.update(ref, mapOf(
                    // Career stats
                    "totalPoints"      to current + pointsToAdd,
                    "wicketsTaken"     to wickets + wicketsToAdd,
                    "raidPoints"       to raids   + raidPointsToAdd,
                    "tacklePoints"     to tackles + tacklePointsToAdd,
                    "acePoints"        to aces    + acePointsToAdd,
                    "blockPoints"      to blocks  + blockPointsToAdd,
                    "matchesPlayed"    to if (isManOfMatch) matches + 1 else matches,
                    "manOfMatchCount"  to if (isManOfMatch) mom + 1 else mom,
                    // Match stats (same increments — reset at match start)
                    "matchPoints"      to mPts    + pointsToAdd,
                    "matchWickets"     to mWkt    + wicketsToAdd,
                    "matchRaidPoints"  to mRaid   + raidPointsToAdd,
                    "matchTacklePoints" to mTackle + tacklePointsToAdd,
                    "matchAcePoints"   to mAce    + acePointsToAdd,
                    "matchBlockPoints" to mBlock  + blockPointsToAdd
                ))
            }.await()
        } catch (e: Exception) {}
    }

    // Call this at the START of a new match to reset match stats for all players
    suspend fun resetMatchStats(playerIds: List<String>) {
        playerIds.forEach { id ->
            try {
                firestore.collection("players").document(id).update(mapOf(
                    "matchPoints"       to 0,
                    "matchWickets"      to 0,
                    "matchRaidPoints"   to 0,
                    "matchTacklePoints" to 0,
                    "matchAcePoints"    to 0,
                    "matchBlockPoints"  to 0
                )).await()
            } catch (e: Exception) {}
        }
    }

    /**
     * Call once per player at end of match to increment their matchesPlayed count.
     * Pass all player IDs who were active (scored or played) in this match.
     */
    suspend fun markMatchPlayed(playerIds: List<String>) {
        playerIds.forEach { playerId ->
            try {
                val ref = firestore.collection("players").document(playerId)
                firestore.runTransaction { tx ->
                    val snap = tx.get(ref)
                    val matches = snap.getLong("matchesPlayed")?.toInt() ?: 0
                    tx.update(ref, "matchesPlayed", matches + 1)
                }.await()
            } catch (e: Exception) {}
        }
    }

    // whereEqualTo alone is fine without a composite index.
    // Sort in-memory so no index needed at all.
    fun getPlayersForTournament(tournamentId: String): Flow<List<Player>> = callbackFlow {
        val listener = firestore.collection("players")
            .whereEqualTo("tournamentId", tournamentId)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(Player::class.java) }
                    ?.sortedByDescending { it.totalPoints }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // Alias used by ScorerActivity (name makes intent clear)
    fun getPlayersForTournamentNoIndex(tournamentId: String) =
        getPlayersForTournament(tournamentId)

    suspend fun addPlayer(player: Player): Result<String> {
        return try {
            val doc = firestore.collection("players").document()
            val p = player.copy(id = doc.id)
            doc.set(p).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
