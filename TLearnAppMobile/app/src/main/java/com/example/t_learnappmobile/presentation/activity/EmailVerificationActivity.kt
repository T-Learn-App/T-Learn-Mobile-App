package com.example.t_learnappmobile.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import com.example.t_learnappmobile.databinding.ActivityEmailVerificationBinding

class EmailVerificationActivity: AppCompatActivity() {
    private lateinit var binding: ActivityEmailVerificationBinding
    private var countDownTimer: CountDownTimer? = null
    private val TIMER_DURATION = 60000L
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = intent.getStringExtra("email") ?: "user@example.com"
        binding.emailDisplay.text = "Код отправлен на: $email"

        setupListeners()
        startTimer()
    }
    private fun setupListeners(){
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
            binding.btnResendCode.isEnabled = false
            startTimer()
        }
        binding.codeEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnVerify.isEnabled = s?.length == 6
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }
    private fun startTimer(){
        countDownTimer?.cancel()
        binding.btnResendCode.isEnabled = false
        binding.btnResendCode.alpha = 0.5f

        countDownTimer = object : CountDownTimer(TIMER_DURATION, 1000){
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                binding.timerText.text = "Повторить через: ${secondsRemaining}s"
            }

            override fun onFinish() {
                binding.timerText.text = "Код истёк"
                binding.btnResendCode.isEnabled = true
                binding.btnResendCode.alpha = 1f
            }
        }
        countDownTimer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}