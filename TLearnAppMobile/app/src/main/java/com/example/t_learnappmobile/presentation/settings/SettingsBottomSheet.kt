package com.example.t_learnappmobile.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.data.settings.SettingsManager
import com.example.t_learnappmobile.databinding.FragmentSettingsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class SettingsBottomSheet : BottomSheetDialogFragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsManager: SettingsManager
    var onCategoryChanged: (() -> Unit)? = null

    private val categories = listOf(
        Category(1L, "Conversional"),
        Category(2L, "Technologies"),
        Category(3L, "Slang"),
        Category(4L, "Finance")
    )

    data class Category(val id: Long, val name: String)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsManager = SettingsManager(requireContext())
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        setupCategorySpinner()
        updateThemeSelection(settingsManager.getTheme())
        loadProfileData()
    }

    private fun loadProfileData() {
        binding.nameEditText.setText(settingsManager.getFirstName())
        binding.surnameEditText.setText(settingsManager.getLastName())
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories.map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.dictionarySpinner.adapter = adapter

        val position = categories.indexOfFirst { it.id == settingsManager.getCurrentCategoryId() }
        if (position != -1) binding.dictionarySpinner.setSelection(position, false)

        binding.dictionarySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                settingsManager.setCurrentCategoryId(categories[position].id)
                onCategoryChanged?.invoke()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener { dismiss() }

        binding.btnSaveProfile.setOnClickListener {
            val firstName = binding.nameEditText.text.toString().trim()
            val lastName = binding.surnameEditText.text.toString().trim()
            if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                lifecycleScope.launch {
                    val success = settingsManager.updateUserProfile(firstName, lastName)
                    if (success) showSuccessDialog("Профиль обновлен!") else showErrorDialog("Ошибка сохранения")
                }
            } else {
                showErrorDialog("Заполните имя и фамилию")
            }
        }

        binding.btnResetDictionary.setOnClickListener {
            showConfirmationDialog("Сброс статистики", "Статистика категории будет очищена") {
                showSuccessDialog("Статистика категории очищена")
            }
        }

        binding.btnResetAll.setOnClickListener {
            showConfirmationDialog("Сброс всех настроек", getString(R.string.confirm_reset_all_message)) {
                lifecycleScope.launch {
                    settingsManager.clearAllData()
                    binding.dictionarySpinner.setSelection(0)
                    updateThemeSelection(SettingsManager.THEME_SYSTEM)
                    showSuccessDialog("Все данные очищены")
                }
            }
        }

        binding.lightThemeButton.setOnClickListener {
            settingsManager.setTheme(SettingsManager.THEME_LIGHT)
            updateThemeSelection(SettingsManager.THEME_LIGHT)
        }

        binding.darkThemeButton.setOnClickListener {
            settingsManager.setTheme(SettingsManager.THEME_DARK)
            updateThemeSelection(SettingsManager.THEME_DARK)
        }
    }

    private fun updateThemeSelection(theme: Int) {
        val lightBg = if (theme == SettingsManager.THEME_LIGHT) R.drawable.selected_theme_bg else android.R.color.transparent
        val darkBg = if (theme == SettingsManager.THEME_DARK) R.drawable.selected_theme_bg else android.R.color.transparent
        binding.lightThemeButton.setBackgroundResource(lightBg)
        binding.darkThemeButton.setBackgroundResource(darkBg)
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Да") { _, _ -> onConfirm() }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun showSuccessDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Успех")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object { const val TAG = "SettingsBottomSheet" }
}