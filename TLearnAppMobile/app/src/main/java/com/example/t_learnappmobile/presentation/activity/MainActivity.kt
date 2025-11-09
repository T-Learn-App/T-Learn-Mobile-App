package com.example.t_learnappmobile.presentation.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.model.Word
import com.example.t_learnappmobile.databinding.ActivityMainBinding
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.TranslationDirection
import com.example.t_learnappmobile.presentation.viewmodel.WordViewModel
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: WordViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this).get(WordViewModel::class.java)
        observeViewModel()
        setUpClickListener()
    }



    private fun observeViewModel(){
        viewModel.currentWord.observe(this) {word ->
            if (word == null){
                binding.categoryText.text = "Нет карточек"
                return@observe
            }
            binding.knownButton.isEnabled = true
            binding.unknownButton.isEnabled = true


            binding.categoryText.text = "Категория: ${word.category}"

            when (word.cardType){
                CardType.NEW -> {
                    binding.wordLabel.visibility = View.VISIBLE
                    binding.wordLabel.text = "● Новое слово"
                }
                CardType.ROTATION -> {
                    binding.wordLabel.visibility = View.VISIBLE
                    binding.wordLabel.text = "● Этап: ${word.repetitionStage + 1}"
                }
            }
            when (word.translationDirection){
                TranslationDirection.ENGLISH_TO_RUSSIAN -> {
                    binding.wordText.text = word.englishWord
                    binding.transcriptionText.text = word.transcription
                    binding.translationText.text = word.russianTranslation
                }
                TranslationDirection.RUSSIAN_TO_ENGLISH -> {
                    binding.wordText.text = word.russianTranslation
                    binding.transcriptionText.text = word.transcription
                    binding.translationText.text = word.englishWord
                }
            }
           // binding.partOfSpeechText.text = word.partOfSpeech
            binding.translationText.visibility = View.GONE
            binding.showTranslationButtonText.text = "Показать перевод"
            
            when(word.cardType){
                CardType.NEW -> {
                    binding.knownButton.text = "Я знаю это слово"
                    binding.unknownButton.text = "Я не знаю этого слова"
                }
                CardType.ROTATION -> {
                    binding.knownButton.text = "Я запомнил это слово"
                    binding.unknownButton.text = "Я не запомнил это слово"
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.knownButton.isEnabled = !isLoading
            binding.unknownButton.isEnabled = !isLoading
        }



        viewModel.isTranslationHidden.observe(this) { isHidden ->
            if (isHidden){
                binding.translationText.visibility = View.GONE
                binding.showTranslationButtonText.text = "Показать перевод"
                binding.eyeIcon.setImageResource(R.drawable.visibility_24px)
            } else {
                binding.translationText.visibility = View.VISIBLE
                binding.showTranslationButtonText.text = "Скрыть перевод"
                binding.eyeIcon.setImageResource(R.drawable.visibility_off_24px)
            }
        }

    }

    private fun setUpClickListener() {
        binding.eyeIcon.setOnClickListener {
            viewModel.toggleTranslation()
        }
        binding.showTranslationButtonText.setOnClickListener {
           viewModel.toggleTranslation()
        }
        binding.knownButton.setOnClickListener {
            binding.knownButton.isEnabled = false
            binding.unknownButton.isEnabled = false
            binding.knownButton.text = "Отлично!"
            viewModel.onKnowCard()
        }
        binding.unknownButton.setOnClickListener {
            binding.knownButton.isEnabled = false
            binding.unknownButton.isEnabled = false
            binding.unknownButton.text = "Подождите..."
            viewModel.onDontKnowCard()
        }
        binding.statsButton.setOnClickListener {

        }
        binding.settingsButton.setOnClickListener {

        }

    }



}