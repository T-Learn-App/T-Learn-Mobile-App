package com.example.t_learnappmobile.presentation.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import com.example.t_learnappmobile.databinding.ActivitySettingsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettingsBottomSheet : BottomSheetDialogFragment() {
    private var _binding: ActivitySettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = ActivitySettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        setupListeners()

    }
    private fun setupListeners(){
        binding.btnClose.setOnClickListener {
            dismiss()
        }
        binding.btnResetDictionary.setOnClickListener {
           Toast.makeText(requireContext(), "Словарь сброшен", Toast.LENGTH_SHORT).show()
        }
        binding.btnResetAll.setOnClickListener {
            Toast.makeText(requireContext(), "Данные сброшены", Toast.LENGTH_SHORT).show()
        }
        binding.darkThemeButton.setOnClickListener {
            Toast.makeText(requireContext(), "Ночная тема выбрана", Toast.LENGTH_SHORT).show()
        }
        binding.lightThemeButton.setOnClickListener {
            Toast.makeText(requireContext(), "Дневная тема выбрана", Toast.LENGTH_SHORT).show()
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