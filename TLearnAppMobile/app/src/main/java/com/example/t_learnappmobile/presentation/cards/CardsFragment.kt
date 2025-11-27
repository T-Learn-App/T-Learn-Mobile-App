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
import com.example.t_learnappmobile.databinding.FragmentCardBinding
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.TranslationDirection
import com.example.t_learnappmobile.presentation.auth.AuthActivity
import com.example.t_learnappmobile.presentation.settings.SettingsBottomSheet
import com.example.t_learnappmobile.presentation.statistics.StatistisBottomSheet
import kotlinx.coroutines.launch

class CardsFragment : Fragment() {


    private var _binding: FragmentCardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: WordViewModel
    private var onStatsClickListener: (() -> Unit)? = null
    private var onSettingsClickListener: (() -> Unit)? = null


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
        viewModel = ViewModelProvider(this).get(WordViewModel::class.java)
        observeViewModel()
        setUpClickListener()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentWord.collect { word ->
                    if (word == null) {
                        binding.categoryText.setText(R.string.no_cards)
                        binding.knownButton.isEnabled = false
                        binding.unknownButton.isEnabled = false
                        return@collect
                    }

                    binding.knownButton.isEnabled = true
                    binding.unknownButton.isEnabled = true

                    binding.categoryText.text = getString(R.string.type, word.category)


                    when (word.cardType) {
                        CardType.NEW -> {
                            binding.wordLabel.visibility = View.VISIBLE
                            binding.wordLabel.setText(R.string.new_word)
                            binding.knownButton.setText(R.string.i_know_that_word)
                            binding.unknownButton.setText(R.string.i_dont_know_that_word)
                        }

                        CardType.ROTATION -> {
                            binding.wordLabel.visibility = View.VISIBLE
                            binding.wordLabel.text =
                                getString(R.string.replay_stage, word.repetitionStage)
                            binding.knownButton.setText(R.string.i_remember_that_word)
                            binding.unknownButton.setText(R.string.i_didnt_remember_that_word)
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
                viewModel.isTranslationHidden.collect { isHidden ->
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
            bottomSheet.show(parentFragmentManager, StatistisBottomSheet.Companion.TAG)
        }
        binding.settingsButton.setOnClickListener {
            val bottomSheet = SettingsBottomSheet()
            bottomSheet.show(parentFragmentManager, SettingsBottomSheet.Companion.TAG)
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

    fun setOnStatsClickListener(listener: () -> Unit) {
        onStatsClickListener = listener
    }

    fun setOnSettingsClickListener(listener: () -> Unit) {
        onSettingsClickListener = listener
    }
}