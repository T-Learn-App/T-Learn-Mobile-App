package com.example.t_learnappmobile.presentation.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
        viewModel.startGame(GameMode.TIME)
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
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateUI(state)
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
            binding.wordCounterText.text = "${state.currentWordIndex}/10"
            binding.timerText.text = String.format("%02d:%02d", state.timer / 60, state.timer % 60)
            binding.scoreText.text = state.score.toString()

            if (state.options.isNotEmpty()) {
                binding.option1Text.text = state.options[0]
                binding.option2Text.text = state.options[1]
            }

        } else if (state.showResults) {

            binding.gameWordText.text = "🎉 ${state.score} очков!"
            binding.wordCounterText.text = "10 слов завершено"
            binding.timerText.text = ""
            binding.scoreText.text = ""


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
