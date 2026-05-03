package com.example.t_learnappmobile.presentation.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.t_learnappmobile.databinding.FragmentStatisticsBinding
import com.example.t_learnappmobile.data.leaderboard.LeaderboardPlayer
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class StatisticsBottomSheet : BottomSheetDialogFragment() {
    private lateinit var leaderboardAdapter: LeaderboardAdapter
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupLeaderboard()
        observeViewModel()
        viewModel.refreshStats()
    }

    private fun setupLeaderboard() {
        binding.leaderboardRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        leaderboardAdapter = LeaderboardAdapter(emptyList())
        binding.leaderboardRecyclerView.adapter = leaderboardAdapter
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener { dismiss() }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.leaderboardPlayers.collect { players ->
                        leaderboardAdapter.updatePlayers(players)
                    }
                }
                launch {
                    viewModel.yourPosition.collect { yourPos ->
                        updateYourPosition(yourPos)
                    }
                }
                launch {
                    viewModel.yourGameScore.collect { score ->
                        binding.yourGameScoreText.text = score.toString()
                    }
                }
                launch {
                    viewModel.learnedWordsCount.collect { count ->
                        binding.learnedWordsCount.text = count.toString()
                    }
                }
                launch {
                    viewModel.inProgressWordsCount.collect { count ->
                        binding.inProgressCount.text = count.toString()
                    }
                }
                launch {
                    viewModel.newWordsCount.collect { count ->
                        binding.newWordsCount.text = count.toString()
                    }
                }
                launch {
                    viewModel.currentDictionaryName.collect { name ->
                        binding.dictionaryNameText.text = name
                    }
                }
                launch {
                    viewModel.weekStats.collect { stats ->
                        binding.statsChart.setStats(stats)
                    }
                }
            }
        }
    }

    private fun updateYourPosition(yourPos: LeaderboardPlayer?) {
        if (yourPos != null && yourPos.position > 0) {
            binding.yourPositionText.text = "#${yourPos.position}"
            binding.yourNameText.text = yourPos.name
            binding.yourScoreText.text = yourPos.score.toString()
        } else {
            binding.yourPositionText.text = "#-"
            binding.yourNameText.text = "Вы"
            binding.yourScoreText.text = viewModel.yourGameScore.value.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "StatisticsBottomSheet"
    }
}