package com.example.t_learnappmobile.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.databinding.ActivityRegistrationBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegistrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()
    }

    private fun setupListeners() {
        updateButtonState()
        binding.passwordEditTextRegistration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                if (password.isEmpty()) {
                    binding.passwordEditTextRegistration.error = null
                    return
                }
                val requirements = mutableListOf<String>()
                if (password.length < 8) {
                    requirements.add("• Минимум 8 символов")
                }
                if (!password.any { it.isLowerCase() }) {
                    requirements.add("• Строчные буквы (a-z)")
                }
                if (!password.any { it.isUpperCase() }) {
                    requirements.add("• Прописные буквы (A-Z)")
                }
                if (!password.any { it.isDigit() }) {
                    requirements.add("• Цифры (0-9)")
                }
                if (!password.any { "!@#\$%^&*()_+=-[]{}|;:,.<>?".contains(it) }) {
                    requirements.add("• Спецсимволы (!@#$%)")
                }

                if (requirements.isNotEmpty()) {
                    binding.passwordInputLayoutRegistration.error = requirements.joinToString("\n")
                } else {
                    binding.passwordInputLayoutRegistration.error = null
                }
                updateButtonState()
            }
        })
        binding.loginEditTextRegistration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateButtonState()
            }})
        binding.emailEditTextRegistration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateButtonState()
            }})

        binding.btnSendCode.setOnClickListener {
            val intent = Intent(this, EmailVerificationActivity::class.java)
            intent.putExtra("email", binding.emailEditTextRegistration.text.toString())
            intent.putExtra("login", binding.loginEditTextRegistration.text.toString())
            intent.putExtra("password", binding.passwordEditTextRegistration.text.toString())

            startActivity(intent)
        }

        binding.btnAlreadyHaveAccount.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
    fun updateButtonState(){
        val loginValid = binding.loginEditTextRegistration.text.toString().trim().isNotEmpty()
        val emailValid = binding.emailEditTextRegistration.text.toString().trim().isNotEmpty()
        val passwordValid = binding.passwordEditTextRegistration.error == null && binding.passwordEditTextRegistration.text.toString().isNotEmpty()
        binding.btnSendCode.isEnabled = loginValid && emailValid && passwordValid
    }


}