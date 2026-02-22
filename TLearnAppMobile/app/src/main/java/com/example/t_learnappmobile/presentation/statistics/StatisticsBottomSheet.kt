package com.example.t_learnappmobile.presentation.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.t_learnappmobile.databinding.FragmentStatisctisBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.data.leaderboard.LeaderboardPlayer
import com.example.t_learnappmobile.presentation.statistics.LeaderboardAdapter
class StatisticsBottomSheet : BottomSheetDialogFragment() {
    private lateinit var leaderboardAdapter: LeaderboardAdapter

    private var _binding: FragmentStatisctisBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisctisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupLeaderboard() // ✅ ДОБАВЬ
        observeViewModel()
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
                        binding.dictionaryNameText.text = name  // ✅ Название словаря
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
                    }
                }
                launch {
                    viewModel.leaderboardPlayers.collect { players ->
                        leaderboardAdapter = LeaderboardAdapter(players)
                        binding.leaderboardRecyclerView.adapter = leaderboardAdapter
                    }
                }

                launch {
                    viewModel.yourPosition.collect { yourPos ->
                        yourPos?.let {
                            binding.yourPositionText.text = "#${it.position}"
                            binding.yourNameText.text = it.name
                            binding.yourScoreText.text = it.score.toString()
                            binding.yourAvatarImage.setImageResource(R.drawable.gray_button_rounded)
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
