package com.example.t_learnappmobile.presentation.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.t_learnappmobile.databinding.ActivityMainBinding
import com.example.t_learnappmobile.presentation.cards.CardsFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            loadCardsFragment()
        }
    }

    private fun loadCardsFragment() {
        val fragment = CardsFragment()
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

}