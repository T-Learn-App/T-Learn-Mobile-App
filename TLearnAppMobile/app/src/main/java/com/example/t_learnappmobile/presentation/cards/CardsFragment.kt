package com.example.t_learnappmobile.presentation.cards

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
import com.example.t_learnappmobile.data.dictionary.DictionaryManager
import com.example.t_learnappmobile.data.repository.ServiceLocator

import com.example.t_learnappmobile.databinding.FragmentCardBinding
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.TranslationDirection
import com.example.t_learnappmobile.presentation.auth.AuthState
import com.example.t_learnappmobile.presentation.auth.LoginActivity
import com.example.t_learnappmobile.presentation.auth.LogoutViewModel


import com.example.t_learnappmobile.presentation.settings.SettingsBottomSheet
import com.example.t_learnappmobile.presentation.statistics.StatisticsBottomSheet
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class CardsFragment : Fragment() {

    private var _binding: FragmentCardBinding? = null
    private val binding get() = _binding!!

    private lateinit var wordViewModel: WordViewModel
    private lateinit var logoutViewModel: LogoutViewModel
    private lateinit var dictionaryManager: DictionaryManager
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wordViewModel = ViewModelProvider(this).get(WordViewModel::class.java)
        logoutViewModel = ViewModelProvider(requireActivity()).get(LogoutViewModel::class.java)
        dictionaryManager = ServiceLocator.dictionaryManager  // ✅ ПРЯМО dictionaryManager
        observeViewModel()
        setUpClickListener()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                wordViewModel.currentWord.collect { word ->
                    if (word == null) {
                        binding.categoryText.setText(R.string.no_cards)
                        binding.knownButton.isEnabled = false
                        binding.unknownButton.isEnabled = false
                        return@collect
                    }

                    // Название текущего словаря
                    val currentDictName = runCatching {
                        val userId = ServiceLocator.tokenManager.getUserData().firstOrNull()?.id
                        userId?.let { dictionaryManager.getCurrentDictionary(it).name } ?: "Conversional"
                    }.getOrElse { "Conversional" }
                    binding.categoryText.text = currentDictName

                    binding.knownButton.isEnabled = true
                    binding.unknownButton.isEnabled = true

                    // ✅ ИСПРАВЛЕНИЕ: правильный текст кнопок по типу карточки
                    when (word.cardType) {
                        CardType.NEW -> {
                            binding.wordLabel.visibility = View.VISIBLE
                            binding.wordLabel.setText(R.string.new_word)
                            binding.knownButton.setText(R.string.i_know_that_word)           // "Я знаю это слово"
                            binding.unknownButton.setText(R.string.i_dont_know_that_word)    // "Я не знаю это слово"
                        }
                        CardType.ROTATION -> {
                            binding.wordLabel.visibility = View.VISIBLE
                            binding.wordLabel.text = getString(R.string.replay_stage, word.repetitionStage)
                            binding.knownButton.setText(R.string.i_remember_that_word)       // "Я запомнил это слово"
                            binding.unknownButton.setText(R.string.i_didnt_remember_that_word) // "Я не запомнил это слово"
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
                    binding.showTranslationButtonText.setText(R.string.show_translation)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                wordViewModel.isTranslationHidden.collect { isHidden ->
                    if (isHidden) {
                        binding.translationText.visibility = View.GONE
                        binding.showTranslationButtonText.setText(R.string.show_translation)
                        binding.eyeIcon.setImageResource(R.drawable.visibility_24px)
                    } else {
                        binding.translationText.visibility = View.VISIBLE
                        binding.showTranslationButtonText.setText(R.string.hide_translation)
                        binding.eyeIcon.setImageResource(R.drawable.visibility_off_24px)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                wordViewModel.isLoading.collect { isLoading ->
                    //binding.loadingProgressBar.visibility =
                    // if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                wordViewModel.error.collect { errorMessage ->
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                logoutViewModel.authState.collect { state ->
                    when (state) {
                        is AuthState.LoggedOut -> {
                            val intent = Intent(requireContext(), LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            requireActivity().finish()
                        }
                        is AuthState.Error -> {
                            Toast.makeText(
                                requireContext(),
                                state.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun setUpClickListener() {
        binding.eyeIcon.setOnClickListener {
            wordViewModel.toggleTranslation()
        }
        binding.showTranslationButtonText.setOnClickListener {
            wordViewModel.toggleTranslation()
        }
        binding.knownButton.setOnClickListener {
            binding.knownButton.isEnabled = false
            binding.unknownButton.isEnabled = false
            lifecycleScope.launch {
                wordViewModel.onKnowCard()
            }
        }
        binding.unknownButton.setOnClickListener {
            binding.knownButton.isEnabled = false
            binding.unknownButton.isEnabled = false
            lifecycleScope.launch {
                wordViewModel.onDontKnowCard()
            }
        }
        binding.statsButton.setOnClickListener {
            val bottomSheet = StatisticsBottomSheet()
            bottomSheet.show(parentFragmentManager, StatisticsBottomSheet.Companion.TAG)
        }
        binding.settingsButton.setOnClickListener {
            val settingsSheet = SettingsBottomSheet()
            // ✅ КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: перезагружаем слова при смене словаря
            settingsSheet.onDictionaryChanged = {
                wordViewModel.fetchWords() // Загружаем слова нового словаря
            }
            settingsSheet.show(parentFragmentManager, SettingsBottomSheet.TAG)
        }

        binding.exitButton.setOnClickListener {
            logoutViewModel.logout()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}