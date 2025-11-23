package com.example.t_learnappmobile.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.databinding.ActivityCardBinding
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.TranslationDirection
import com.example.t_learnappmobile.presentation.viewmodel.WordViewModel
import kotlinx.coroutines.launch

class CardsFragment : Fragment() {


    private var _binding: ActivityCardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: WordViewModel
    private var onStatsClickListener: (() -> Unit)? = null
    private var onSettingsClickListener: (() -> Unit)? = null



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        _binding = ActivityCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(WordViewModel::class.java)
        observeViewModel()
        setUpClickListener()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentWord.collect { word ->
                    if (word == null) {
                        binding.categoryText.text = "Нет карточек"
                        binding.knownButton.isEnabled = false
                        binding.unknownButton.isEnabled = false
                        return@collect
                    }

                    binding.knownButton.isEnabled = true
                    binding.unknownButton.isEnabled = true

                    binding.categoryText.text = "Категория: ${word.category}"

                    when (word.cardType) {
                        CardType.NEW -> {
                            binding.wordLabel.visibility = View.VISIBLE
                            binding.wordLabel.text = "● Новое слово"
                            binding.knownButton.text = "Я знаю это слово"
                            binding.unknownButton.text = "Я не знаю этого слова"
                        }
                        CardType.ROTATION -> {
                            binding.wordLabel.visibility = View.VISIBLE
                            binding.wordLabel.text = "Этап повтора: ${word.repetitionStage}"
                            binding.knownButton.text = "Я запомнил это слово"
                            binding.unknownButton.text = "Я не запомнил это слово"
                        }
                    }

                    when (word.translationDirection) {
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

                    binding.partOfSpeechText.text = word.partOfSpeech.name

                    binding.translationText.visibility = View.GONE
                    binding.showTranslationButtonText.text = "Показать перевод"
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isTranslationHidden.collect { isHidden ->
                    if (isHidden) {
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
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cardStats.collect { stats ->
                    val statsText = "Новых: ${stats.newWordsCount} | В ротации: ${stats.rotationWordsCount} | Выучено: ${stats.learnedWordsCount}"
                    // binding.statsText.text = statsText
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    //binding.loadingProgressBar.visibility =
                    // if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { errorMessage ->
                    errorMessage?.let {
                        Toast.makeText(
                            requireContext(),
                            it,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
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
            lifecycleScope.launch {
                viewModel.onKnowCard()
            }
        }
        binding.unknownButton.setOnClickListener {
            binding.knownButton.isEnabled = false
            binding.unknownButton.isEnabled = false
            lifecycleScope.launch {
                viewModel.onDontKnowCard()
            }
        }
        binding.statsButton.setOnClickListener {
            val bottomSheet = StatistisBottomSheet()
            bottomSheet.show(parentFragmentManager, StatistisBottomSheet.TAG)
        }
        binding.settingsButton.setOnClickListener {
            val bottomSheet = SettingsBottomSheet()
            bottomSheet.show(parentFragmentManager, SettingsBottomSheet.TAG)
        }
        binding.exitButton.setOnClickListener {
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    fun setOnStatsClickListener(listener: () -> Unit){
        onStatsClickListener = listener
    }
    fun setOnSettingsClickListener(listener: () -> Unit){
        onSettingsClickListener = listener
    }
}