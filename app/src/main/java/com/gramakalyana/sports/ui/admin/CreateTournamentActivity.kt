package com.gramakalyana.sports.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gramakalyana.sports.data.models.Tournament
import com.gramakalyana.sports.data.repository.FirebaseRepository
import com.gramakalyana.sports.databinding.ActivityCreateTournamentBinding
import kotlinx.coroutines.launch

class CreateTournamentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateTournamentBinding
    private val repo = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateTournamentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Create Tournament"

        val sports = arrayOf("Kabaddi", "Volleyball", "Cricket")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sports)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSport.adapter = adapter

        binding.btnCreate.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()
            val sport = binding.spinnerSport.selectedItem.toString()

            if (name.isEmpty()) {
                binding.etName.error = "Enter tournament name"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = repo.createTournament(
                    Tournament(name = name, sport = sport, location = location)
                )
                result.onSuccess { id ->
                    Toast.makeText(this@CreateTournamentActivity, "Tournament created!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@CreateTournamentActivity, AddTeamActivity::class.java)
                    intent.putExtra("TOURNAMENT_ID", id)
                    intent.putExtra("SPORT", sport)
                    startActivity(intent)
                    finish()
                }.onFailure {
                    Toast.makeText(this@CreateTournamentActivity, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
