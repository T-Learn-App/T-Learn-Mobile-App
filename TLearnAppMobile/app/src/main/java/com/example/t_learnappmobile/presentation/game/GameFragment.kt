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
        setupUI()
        observeViewModel()
        viewModel.startGame(GameMode.WORDS)
    }

    private fun setupUI() {
        binding.option1Card.setOnClickListener {
            viewModel.selectAnswer(0)
        }
        binding.option2Card.setOnClickListener {
            viewModel.selectAnswer(1)
        }

        binding.closeGameButton.setOnClickListener {
            viewModel.closeResults()
            dismiss()
        }

        binding.gameWordText.visibility = View.VISIBLE
        binding.wordCounterText.visibility = View.VISIBLE
        binding.scoreText.visibility = View.VISIBLE
        binding.noWordsMessage.visibility = View.GONE
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateUI(state)
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.loadingOverlay.visibility = if (isLoading) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                }
                launch {
                    viewModel.isNetworkAvailable.collect { isAvailable ->
                        if (!isAvailable) {
                            binding.noNetworkOverlay.visibility = View.VISIBLE
                            binding.loadingOverlay.visibility = View.GONE
                            binding.option1Card.isEnabled = false
                            binding.option2Card.isEnabled = false
                        } else {
                            binding.noNetworkOverlay.visibility = View.GONE
                        }
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
            binding.noWordsMessage.visibility = View.GONE

            binding.gameWordText.text = state.currentWord.english
            binding.wordCounterText.text = "${state.currentWordIndex + 1}/10"
            binding.scoreText.text = state.score.toString()

            if (state.options.isNotEmpty()) {
                binding.option1Text.text = state.options[0]
                binding.option2Text.text = state.options[1]
            }

        } else if (state.showResults) {

            binding.gameWordText.text = "🎉 ${state.score} очков!"
            binding.wordCounterText.text = "10 слов завершено"
            binding.scoreText.text = ""

            binding.option1Card.visibility = View.GONE
            binding.option2Card.visibility = View.GONE
            binding.noWordsMessage.visibility = View.GONE
            binding.closeGameButton.visibility = View.VISIBLE

        } else if (state.totalWords == 0 && !state.isGameActive && !state.showResults) {
            binding.noWordsMessage.visibility = View.VISIBLE
            binding.option1Card.visibility = View.GONE
            binding.option2Card.visibility = View.GONE
            binding.closeGameButton.visibility = View.VISIBLE
            binding.gameWordText.visibility = View.GONE
            binding.wordCounterText.visibility = View.GONE
            binding.scoreText.visibility = View.GONE
        } else {

            binding.noWordsMessage.visibility = View.GONE
            binding.gameWordText.visibility = View.VISIBLE
            binding.wordCounterText.visibility = View.VISIBLE
            binding.scoreText.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
