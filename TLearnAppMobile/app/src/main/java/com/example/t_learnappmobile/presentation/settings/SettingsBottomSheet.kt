package com.example.t_learnappmobile.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.databinding.FragmentSettingsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettingsBottomSheet : BottomSheetDialogFragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()

    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
        binding.btnResetDictionary.setOnClickListener {
            Toast.makeText(
                requireContext(), getString(R.string.dictionary_has_been_reset), Toast.LENGTH_SHORT
            ).show()
        }
        binding.btnResetAll.setOnClickListener {
            Toast.makeText(
                requireContext(), getString(R.string.data_has_been_reset), Toast.LENGTH_SHORT
            ).show()
        }
        binding.darkThemeButton.setOnClickListener {
            Toast.makeText(
                requireContext(), getString(R.string.dark_theme_is_selected), Toast.LENGTH_SHORT
            ).show()
        }
        binding.lightThemeButton.setOnClickListener {
            Toast.makeText(
                requireContext(), getString(R.string.light_theme_is_selected), Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SettingsBottomSheet"
    }
}