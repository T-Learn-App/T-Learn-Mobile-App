package com.example.t_learnappmobile.presentation.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.t_learnappmobile.databinding.FragmentGameBinding
import com.example.t_learnappmobile.domain.model.GameMode
import com.example.t_learnappmobile.data.settings.SettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class GameFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GameViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем текущий словарь и запускаем игру
        val settingsManager = SettingsManager(requireContext())
        val currentDictId = settingsManager.getCurrentCategoryId()
        viewModel.setDictionary(currentDictId)

        setupUI()
        observeViewModel()
        viewModel.startGame(GameMode.WORDS)
    }

    private fun setupUI() {
        binding.option1Card.setOnClickListener { viewModel.selectAnswer(0) }
        binding.option2Card.setOnClickListener { viewModel.selectAnswer(1) }
        binding.closeGameButton.setOnClickListener {
            viewModel.closeResults()
            dismiss()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { updateUI(it) } }
                launch {
                    viewModel.isLoading.collect {
                        binding.loadingOverlay.visibility =
                            if (it) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun updateUI(state: GameState) {
        binding.option1Card.isEnabled = state.isGameActive
        binding.option2Card.isEnabled = state.isGameActive

        if (state.isGameActive && state.currentWord != null) {
            binding.closeGameButton.visibility = View.GONE
            binding.option1Card.visibility = View.VISIBLE
            binding.option2Card.visibility = View.VISIBLE
            binding.gameWordText.text = state.currentWord.english
            binding.wordCounterText.text = "${state.currentWordIndex + 1}/${state.totalWords}"
            binding.scoreText.text = state.score.toString()

            if (state.options.size >= 2) {
                binding.option1Text.text = state.options[0]
                binding.option2Text.text = state.options[1]
            }

            binding.noWordsMessage.visibility = View.GONE
        } else if (state.showResults) {
            binding.gameWordText.text = "\uD83C\uDF89 ${state.score} очков!"
            binding.wordCounterText.text = "${state.totalWords} слов завершено"
            binding.option1Card.visibility = View.GONE
            binding.option2Card.visibility = View.GONE
            binding.closeGameButton.visibility = View.VISIBLE
            binding.scoreText.text = state.score.toString()
            binding.noWordsMessage.visibility = View.GONE
        } else if (state.totalWords == 0 && !state.isGameActive) {
            binding.noWordsMessage.visibility = View.VISIBLE
            binding.option1Card.visibility = View.GONE
            binding.option2Card.visibility = View.GONE
            binding.closeGameButton.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}