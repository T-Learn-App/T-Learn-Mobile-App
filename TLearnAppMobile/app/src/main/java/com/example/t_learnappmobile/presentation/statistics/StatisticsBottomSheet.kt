package com.example.t_learnappmobile.presentation.statistics

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.databinding.FragmentStatisticsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.t_learnappmobile.data.repository.ServiceLocator

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

    override fun onResume() {
        super.onResume()
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
                    viewModel.currentDictionaryName.collect { name ->
                        binding.dictionaryNameText.text = name
                    }
                }
                launch {
                    viewModel.totalStats.collect { totals ->
                        binding.newWordsCount.text = totals.new.toString()
                        binding.inProgressCount.text = totals.inProgress.toString()
                        binding.learnedWordsCount.text = totals.learned.toString()
                    }
                }
                launch {
                    viewModel.weekStats.collect { stats ->
                        binding.statsChart.setStats(stats)
                        binding.statsChart.post {
                            binding.statsChart.invalidate()
                        }
                    }
                }
                launch {
                    viewModel.leaderboardPlayers.collect { players ->
                        leaderboardAdapter.updatePlayers(players)
                    }
                }
                launch {
                    viewModel.yourPosition.collect { yourPos ->
                        if (yourPos != null && yourPos.position > 0) {
                            binding.yourPositionText.text = "#${yourPos.position}"
                            binding.yourNameText.text = yourPos.name
                            binding.yourScoreText.text = yourPos.score.toString()
                        } else {
                            binding.yourPositionText.text = "#-"
                            val email =
                                com.example.t_learnappmobile.data.repository.ServiceLocator.tokenManager.getUserEmail()
                                    ?: "user@email.com"
                            val displayName =
                                email.split("@").first().replaceFirstChar { it.uppercase() }
                            binding.yourNameText.text = displayName
                            val score = viewModel.yourGameScore.value
                            binding.yourScoreText.text = score.toString()
                        }
                    }
                }
                launch {
                    viewModel.yourGameScore.collect { score ->
                        binding.yourGameScoreText.text = score.toString()
                    }
                }
                launch {
                    viewModel.isLeaderboardLoading.collect { isLoading ->
                        if (isLoading) {

                            binding.leaderboardRecyclerView.visibility = View.GONE
                        } else {

                            binding.leaderboardRecyclerView.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    viewModel.yourPosition.collect { yourPos ->
                        if (yourPos != null && yourPos.position > 0) {
                            binding.yourPositionText.text = "#${yourPos.position}"
                            binding.yourNameText.text = yourPos.name
                            binding.yourScoreText.text = yourPos.score.toString()
                        } else {
                            binding.yourPositionText.text = "#-"
                            val email =
                                ServiceLocator.tokenManager.getUserEmail() ?: "user@email.com"
                            val displayName =
                                email.split("@").first().replaceFirstChar { it.uppercase() }
                            binding.yourNameText.text = displayName
                            val score = viewModel.yourGameScore.value
                            binding.yourScoreText.text = score.toString()
                        }
                    }

                }
            }
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