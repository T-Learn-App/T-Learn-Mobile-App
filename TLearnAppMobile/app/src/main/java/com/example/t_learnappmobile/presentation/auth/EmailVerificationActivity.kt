package com.example.t_learnappmobile.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.data.auth.AuthViewModel
import com.example.t_learnappmobile.databinding.ActivityEmailVerificationBinding
import com.example.t_learnappmobile.presentation.main.MainActivity
import kotlinx.coroutines.launch

class EmailVerificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmailVerificationBinding
    private lateinit var verificationViewModel: EmailVerificationViewModel
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        verificationViewModel = ViewModelProvider(this).get(EmailVerificationViewModel::class.java)
        authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        val email = intent.getStringExtra("email") ?: ""
        binding.emailDisplay.text = getString(R.string.code_has_been_sent_to, email)

        setupListeners()
        observeViewModel()
        if (savedInstanceState == null) {
            verificationViewModel.startTimer()
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnVerify.setOnClickListener {
            val email = intent.getStringExtra("email") ?: ""
            val code = binding.codeEditText.text.toString()

            if (code.length == 6) {
                authViewModel.verifyEmail(email, code)
            } else {
                Toast.makeText(this, getString(R.string.error_enter_6_digit_code), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnResendCode.setOnClickListener {
            val email = intent.getStringExtra("email") ?: ""
            binding.codeError.visibility = View.GONE
            binding.codeEditText.text?.clear()
            authViewModel.sendVerificationCode(email)
            verificationViewModel.startTimer()
        }

        binding.codeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnVerify.isEnabled = s?.length == 6
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            verificationViewModel.timerText.collect { text ->
                binding.timerText.text = text
            }
        }

        lifecycleScope.launch {
            verificationViewModel.isResendEnabled.collect { isEnabled ->
                binding.btnResendCode.isEnabled = isEnabled
                binding.btnResendCode.alpha = if (isEnabled) 1f else 0.5f
            }
        }

        lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> {
                        binding.btnVerify.isEnabled = false
                    }
                    is AuthState.Success -> {
                        binding.btnVerify.isEnabled = true
                        startActivity(Intent(this@EmailVerificationActivity, MainActivity::class.java))
                        finishAffinity()
                    }
                    is AuthState.Error -> {
                        binding.btnVerify.isEnabled = true
                        binding.codeError.visibility = View.VISIBLE
                        binding.codeError.text = state.message
                    }
                    else -> {}
                }
            }
        }
    }
}
