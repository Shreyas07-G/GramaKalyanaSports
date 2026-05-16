package com.gramakalyana.sports.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gramakalyana.sports.data.repository.FirebaseRepository
import com.gramakalyana.sports.databinding.ActivityLoginBinding
import com.gramakalyana.sports.ui.admin.AdminDashboardActivity
import com.gramakalyana.sports.ui.fan.FanViewActivity
import com.gramakalyana.sports.ui.fan.PublicFanActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val repo = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setLoading(true)
            lifecycleScope.launch {
                val result = repo.loginAdmin(email, password)
                setLoading(false)
                result.onSuccess {
                    startActivity(Intent(this@LoginActivity, AdminDashboardActivity::class.java))
                    finish()
                }.onFailure {
                    Toast.makeText(this@LoginActivity, "Login failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setLoading(true)
            lifecycleScope.launch {
                val result = repo.registerAdmin(email, password)
                setLoading(false)
                result.onSuccess {
                    startActivity(Intent(this@LoginActivity, AdminDashboardActivity::class.java))
                    finish()
                }.onFailure {
                    Toast.makeText(this@LoginActivity, "Registration failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Fan button - no login required
        binding.btnFanView.setOnClickListener {
            startActivity(Intent(this, PublicFanActivity::class.java))
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnRegister.isEnabled = !loading
    }
}
