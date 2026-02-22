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

class StatisticsBottomSheet : BottomSheetDialogFragment() {

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
        observeViewModel()
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
