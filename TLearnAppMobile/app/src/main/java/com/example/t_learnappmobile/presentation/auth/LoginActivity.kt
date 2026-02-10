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
import com.example.t_learnappmobile.databinding.ActivityLoginBinding
import com.example.t_learnappmobile.presentation.main.MainActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
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
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.putExtra("SKIP_AUTH_CHECK", true)
                        startActivity(intent)
                        finish()
                    }

                    is AuthState.Error -> {
                        binding.btnSendCode.isEnabled = true
                        val errorMessage = if (state.args.isEmpty()) {
                            getString(state.messageResId)
                        } else {
                            getString(state.messageResId, *state.args)
                        }
                        showErrorDialog(errorMessage)
                    }

                    else -> {
                        binding.btnSendCode.isEnabled = true
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }

        binding.loginEditTextRegistration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateButtonState()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.passwordEditTextRegistration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateButtonState()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSendCode.setOnClickListener {
            val login = binding.loginEditTextRegistration.text.toString().trim()
            val password = binding.passwordEditTextRegistration.text.toString()

            if (login.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(login, password)
            } else {
                Toast.makeText(this, getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun updateButtonState() {
        val login = binding.loginEditTextRegistration.text.toString().trim()
        val password = binding.passwordEditTextRegistration.text.toString()
        binding.btnSendCode.isEnabled = login.isNotEmpty() && password.isNotEmpty()
    }

    private fun showErrorDialog(message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.error_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_ok)) { _, _ -> }
            .show()
    }
}
