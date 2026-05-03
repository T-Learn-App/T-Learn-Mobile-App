package com.example.t_learnappmobile.presentation.cards

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.databinding.FragmentCardBinding
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.Dictionary
import com.example.t_learnappmobile.model.TranslationDirection
import com.example.t_learnappmobile.presentation.auth.AuthState
import com.example.t_learnappmobile.presentation.auth.LoginActivity
import com.example.t_learnappmobile.presentation.auth.LogoutViewModel
import com.example.t_learnappmobile.presentation.game.GameFragment
import com.example.t_learnappmobile.presentation.settings.SettingsBottomSheet
import com.example.t_learnappmobile.presentation.statistics.StatisticsBottomSheet
import kotlinx.coroutines.launch

class CardsFragment : Fragment() {
    private var _binding: FragmentCardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WordViewModel by viewModels {
        WordViewModelFactory(requireContext())
    }
    private val logoutViewModel: LogoutViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.knownButton.setOnClickListener {
            viewModel.onKnowCard()
        }

        binding.unknownButton.setOnClickListener {
            viewModel.onDontKnowCard()
        }

        binding.showTranslationButtonText.setOnClickListener {
            viewModel.toggleTranslation()
        }

        binding.eyeIcon.setOnClickListener {
            viewModel.toggleTranslation()
        }

        binding.statsButton.setOnClickListener {
            val bottomSheet = StatisticsBottomSheet()
            bottomSheet.show(parentFragmentManager, StatisticsBottomSheet.TAG)
        }

        binding.settingsButton.setOnClickListener {
            showDictionarySelector()
        }

        binding.gameButton.setOnClickListener {
            val gameFragment = GameFragment()
            gameFragment.show(parentFragmentManager, "GameFragment")
        }

        binding.exitButton.setOnClickListener {
            logoutViewModel.logout()
        }

        binding.retryButton.setOnClickListener {
            viewModel.retryLoad()
        }
    }

    private fun showDictionarySelector() {
        val dicts = viewModel.dictionaries.value
        if (dicts.isEmpty()) {
            Toast.makeText(requireContext(), "Загрузка словарей...", Toast.LENGTH_SHORT).show()
            return
        }

        val items = dicts.map { "${it.name}" }.toTypedArray()
        val currentId = viewModel.currentDictionary.value?.id

        AlertDialog.Builder(requireContext())
            .setTitle("Выберите словарь")
            .setItems(items) { _, which ->
                val selectedDict = dicts[which]
                viewModel.selectDictionary(selectedDict.id)
            }
            .show()
    }

    private fun observeViewModel() {
        // Наблюдение за текущим словом
        lifecycleScope.launch {
            viewModel.currentWord.collect { word ->
                if (word == null) {
                    showEmptyState(true)
                    return@collect
                }
                showEmptyState(false)
                updateCardContent(word)
            }
        }

        // Наблюдение за загрузкой
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
                if (isLoading) {
                    binding.loadingText.text = "Загрузка слов..."
                }
            }
        }

        // Наблюдение за ошибками
        lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error != null) {
                    binding.errorOverlay.visibility = View.VISIBLE
                    binding.errorOverlayDetails.text = error
                } else {
                    binding.errorOverlay.visibility = View.GONE
                }
            }
        }

        // Наблюдение за видимостью перевода
        lifecycleScope.launch {
            viewModel.isTranslationHidden.collect { isHidden ->
                binding.translationText.visibility = if (isHidden) View.GONE else View.VISIBLE
                binding.showTranslationButtonText.text = if (isHidden) "Показать перевод" else "Скрыть перевод"
            }
        }

        // Наблюдение за текущим словарем
        lifecycleScope.launch {
            viewModel.currentDictionary.collect { dict ->
                dict?.let {
                    binding.categoryText.text = "${it.name}"
                }
            }
        }

        // Наблюдение за выходом из системы
        lifecycleScope.launch {
            logoutViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.LoggedOut -> {
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    is AuthState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun showEmptyState(show: Boolean) {
        if (show) {
            binding.loadingOverlay.visibility = View.VISIBLE
            binding.loadingText.text = "Все слова выучены!\nОтличная работа! 🎉"
        }
    }

    private fun updateCardContent(word: com.example.t_learnappmobile.model.Word) {
        val (positiveBtn, negativeBtn) = viewModel.getButtonTexts()
        binding.knownButton.text = positiveBtn
        binding.unknownButton.text = negativeBtn

        val cardType = viewModel.getCardType()
        binding.wordLabel.visibility = if (cardType == CardType.NEW) View.VISIBLE else View.GONE
        binding.wordLabel.text = if (cardType == CardType.NEW) {
            "Новое слово"
        } else {
            "Повторение (${word.stage}/7)"
        }

        // Показываем слово в зависимости от направления перевода
        when (word.translationDirection) {
            TranslationDirection.EN_TO_RU -> {
                binding.wordText.text = word.englishWord
                binding.transcriptionText.text = word.transcription
                binding.transcriptionText.visibility = View.VISIBLE
                binding.translationText.text = word.translation
                binding.partOfSpeechText.text = word.partOfSpeech.russian
            }
            TranslationDirection.RU_TO_EN -> {
                binding.wordText.text = word.translation
                binding.transcriptionText.visibility = View.GONE
                binding.translationText.text = word.englishWord
                binding.partOfSpeechText.text = word.partOfSpeech.russian
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}