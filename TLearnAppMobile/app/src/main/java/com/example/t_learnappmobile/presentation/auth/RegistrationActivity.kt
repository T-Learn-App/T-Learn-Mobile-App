package com.example.t_learnappmobile.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.databinding.ActivityRegistrationBinding
import com.example.t_learnappmobile.presentation.main.MainActivity
import kotlinx.coroutines.launch

class RegistrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var viewModel: RegistrationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(RegistrationViewModel::class.java)

        setupListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.resetState()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> {
                        binding.btnSendCode.isEnabled = false
                    }

                    is AuthState.Success -> {
                        binding.btnSendCode.isEnabled = true
                        val intent = Intent(this@RegistrationActivity, MainActivity::class.java)
                        intent.putExtra("SKIP_AUTH_CHECK", true)
                        startActivity(intent)
                        finish()
                    }

                    is AuthState.Error -> {
                        binding.btnSendCode.isEnabled = true
                        showErrorDialog(state.message)
                    }

                    else -> {
                        binding.btnSendCode.isEnabled = true
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.passwordEditTextRegistration.addTextChangedListener(
            PasswordValidationWatcher()
        )
        binding.loginEditTextRegistration.addTextChangedListener(
            InputValidationWatcher()
        )
        binding.emailEditTextRegistration.addTextChangedListener(
            InputValidationWatcher()
        )

        binding.btnSendCode.setOnClickListener {
            val login = binding.loginEditTextRegistration.text.toString().trim()
            val email = binding.emailEditTextRegistration.text.toString().trim()
            val password = binding.passwordEditTextRegistration.text.toString()

            if (isFormValid()) {
                viewModel.register(login, email, password)
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.error_fill_all_fields_correct),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnAlreadyHaveAccount.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun isFormValid(): Boolean {
        val login = binding.loginEditTextRegistration.text.toString().trim()
        val email = binding.emailEditTextRegistration.text.toString().trim()
        val password = binding.passwordEditTextRegistration.text.toString()

        return login.isNotEmpty() &&
                email.isNotEmpty() &&
                password.length >= 8 &&
                binding.passwordInputLayoutRegistration.error == null
    }

    private fun updateButtonState() {
        binding.btnSendCode.isEnabled = isFormValid()
    }

    private fun showErrorDialog(message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.error_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_ok)) { _, _ -> }
            .show()
    }

    private inner class PasswordValidationWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val password = s.toString()
            if (password.isEmpty()) {
                binding.passwordInputLayoutRegistration.error = null
                updateButtonState()
                return
            }

            val errors = mutableListOf<String>()
            if (password.length < 8) errors.add(getString(R.string.password_error_min_length))
            if (!password.any { it.isLowerCase() }) errors.add(getString(R.string.password_error_lowercase))
            if (!password.any { it.isUpperCase() }) errors.add(getString(R.string.password_error_uppercase))
            if (!password.any { it.isDigit() }) errors.add(getString(R.string.password_error_digits))
            if (!password.any { "!@#\$%^&*()_+=-[]{}|;:,.<>?".contains(it) }) errors.add(getString(R.string.password_error_special))

            binding.passwordInputLayoutRegistration.error =
                if (errors.isNotEmpty()) errors.joinToString("\n") else null

            updateButtonState()
        }
    }

    private inner class InputValidationWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            updateButtonState()
        }
    }
}
