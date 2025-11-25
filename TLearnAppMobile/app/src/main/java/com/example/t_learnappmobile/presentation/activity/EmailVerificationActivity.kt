package com.example.t_learnappmobile.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.databinding.ActivityEmailVerificationBinding
import com.example.t_learnappmobile.presentation.viewmodel.EmailVerificationViewModel
import kotlinx.coroutines.launch

class EmailVerificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmailVerificationBinding
    private lateinit var viewModel: EmailVerificationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(EmailVerificationViewModel::class.java)

        val email = intent.getStringExtra("email") ?: ""
        binding.emailDisplay.text = getString(R.string.code_has_been_sent_to, email)

        setupListeners()
        observeViewModel()
        if (savedInstanceState == null) {
            viewModel.startTimer()
        }

    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.btnVerify.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        binding.btnResendCode.setOnClickListener {
            binding.codeError.visibility = android.view.View.GONE
            binding.codeEditText.text?.clear()
            viewModel.resetCode()
        }
        binding.codeEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnVerify.isEnabled = s?.length == 6
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.timerText.collect { text ->
                binding.timerText.text = text
            }
        }

        lifecycleScope.launch {
            viewModel.isResendEnabled.collect { isEnabled ->
                binding.btnResendCode.isEnabled = isEnabled
                binding.btnResendCode.alpha = if (isEnabled) 1f else 0.5f
            }
        }
    }
}