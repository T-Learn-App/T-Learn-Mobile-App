package com.example.t_learnappmobile.presentation.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditTextRegistration)
        val passwordInputLayout = findViewById<TextInputLayout>(R.id.passwordInputLayoutRegistration)

        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                if (password.isEmpty()){
                    passwordInputLayout.error = null
                    return
                }
                val requirements = mutableListOf<String>()
                if (password.length < 8){
                    requirements.add("• Минимум 8 символов")
                }
                if (!password.any {it.isLowerCase()}){
                    requirements.add("• Строчные буквы (a-z)")
                }
                if (!password.any {it.isUpperCase()}){
                    requirements.add("• Прописные буквы (A-Z)")
                }
                if (!password.any {it.isDigit()}){
                    requirements.add("• Цифры (0-9)")
                }
                if (!password.any {"!@#\$%^&*()_+=-[]{}|;:,.<>?".contains(it)}){
                    requirements.add("• Спецсимволы (!@#$%)")
                }

                if (requirements.isNotEmpty()){
                    passwordInputLayout.error = requirements.joinToString("\n")
                } else {
                    passwordInputLayout.error = null
                }
            }
        })
        binding.btnSendCode.setOnClickListener {
//            val intent = Intent(this, RegistrationActivity::class.java)
//            intent.putExtra("email", binding.emailEditText.text.toString())
//            startActivity(intent)
        }

        binding.passwordEditTextRegistration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnSendCode.isEnabled = !s.isNullOrEmpty()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }




}