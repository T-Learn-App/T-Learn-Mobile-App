package com.example.t_learnappmobile.presentation.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.t_learnappmobile.databinding.ActivityAuthBinding


class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSendCode.setOnClickListener {
            val intent = Intent(this, EmailVerificationActivity::class.java)
            intent.putExtra("email", binding.emailEditText.text.toString())
            startActivity(intent)
        }

        binding.emailEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnSendCode.isEnabled = !s.isNullOrEmpty()
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

}