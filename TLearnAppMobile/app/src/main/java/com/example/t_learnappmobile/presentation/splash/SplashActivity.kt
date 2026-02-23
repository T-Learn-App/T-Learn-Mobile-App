package com.example.t_learnappmobile.presentation.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.t_learnappmobile.presentation.auth.AuthState
import com.example.t_learnappmobile.presentation.auth.LoginActivity
import com.example.t_learnappmobile.presentation.main.MainActivity
import kotlinx.coroutines.launch


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var viewModel: SplashViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        viewModel = ViewModelProvider(this).get(SplashViewModel::class.java)
        observeAuthState()
        viewModel.checkAuthState()
    }

    private fun observeAuthState() {

      lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Success -> {
                        val intent = Intent(this@SplashActivity, MainActivity::class.java)
                        intent.putExtra("SKIP_AUTH_CHECK", true)
                        startActivity(intent)
                        finish()
                    }

                    is AuthState.LoggedOut, is AuthState.Error -> {
                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                        finish()
                    }

                    else -> {}
                }
            }
        }
    }


}