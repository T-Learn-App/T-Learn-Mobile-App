package com.example.t_learnappmobile.presentation.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.t_learnappmobile.data.auth.TokenManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

abstract class SecureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAuthentication()
    }

    private fun checkAuthentication() {
        lifecycleScope.launch {
            val tokenManager = TokenManager(this@SecureActivity)
            val accessToken = tokenManager.getAccessToken().firstOrNull()

            if (accessToken == null || tokenManager.isTokenExpired(accessToken)) {
                startActivity(Intent(this@SecureActivity, LoginActivity::class.java))
                finish()
            }
        }
    }
}
