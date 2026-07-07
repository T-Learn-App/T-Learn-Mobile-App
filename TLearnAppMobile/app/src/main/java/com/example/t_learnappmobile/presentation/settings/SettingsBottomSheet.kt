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
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.settings.SettingsManager
import com.example.t_learnappmobile.databinding.FragmentSettingsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.firstOrNull
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        lifecycleScope.launch {
            val accessToken = ServiceLocator.tokenManager.getAccessToken().firstOrNull()
            val userId = ServiceLocator.tokenManager.getUserId()

            if (accessToken != null && userId != null) {
                try {
                    val profileResponse = ServiceLocator.userApiService.getCurrentUser("Bearer $accessToken")
                    if (profileResponse.isSuccessful && profileResponse.body() != null) {
                        val profile = profileResponse.body()!!
                        binding.nameEditText.setText(profile.firstName ?: "")
                        binding.surnameEditText.setText(profile.lastName ?: "")
                        return@launch
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SettingsBS", "Profile load error: ${e.message}")
                }
            }

            binding.nameEditText.setText(settingsManager.getFirstName())
            binding.surnameEditText.setText(settingsManager.getLastName())
        }
    }

    private fun setupCategorySpinner() {
        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoryNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.dictionarySpinner.adapter = adapter

        val currentCategoryId = settingsManager.getCurrentCategoryId()
        val position = categories.indexOfFirst { it.id == currentCategoryId }

        if (position != -1) {
            binding.dictionarySpinner.setSelection(position, false)
        }

        binding.dictionarySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                settingsManager.setCurrentCategoryId(selectedCategory.id)
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
                    if (success) {
                        showSuccessDialog("Профиль обновлен!")
                    } else {
                        showErrorDialog("Ошибка сохранения профиля")
                    }
                }
            } else {
                showErrorDialog("Заполните имя и фамилию")
            }
        }

        binding.btnResetDictionary.setOnClickListener {
            showConfirmationDialog(
                title = getString(R.string.confirm_reset_dictionary_title),
                message = "Статистика категории будет очищена",
                onConfirm = {
                    showSuccessDialog("Статистика категории очищена")
                }
            )
        }

        binding.btnResetAll.setOnClickListener {
            showConfirmationDialog(
                title = getString(R.string.confirm_reset_all_title),
                message = getString(R.string.confirm_reset_all_message),
                onConfirm = {
                    lifecycleScope.launch {
                        settingsManager.clearAllData()
                        binding.dictionarySpinner.setSelection(0)
                        updateThemeSelection(SettingsManager.THEME_SYSTEM)
                        showSuccessDialog("Все данные очищены")
                    }
                }
            )
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
        when (theme) {
            SettingsManager.THEME_LIGHT -> {
                binding.lightThemeButton.setBackgroundResource(R.drawable.selected_theme_bg)
                binding.darkThemeButton.setBackgroundResource(android.R.color.transparent)
            }
            SettingsManager.THEME_DARK -> {
                binding.lightThemeButton.setBackgroundResource(android.R.color.transparent)
                binding.darkThemeButton.setBackgroundResource(R.drawable.selected_theme_bg)
            }
            else -> {
                binding.lightThemeButton.setBackgroundResource(android.R.color.transparent)
                binding.darkThemeButton.setBackgroundResource(android.R.color.transparent)
            }
        }
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.confirm)) { _, _ -> onConfirm() }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showSuccessDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Успех")
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_ok), null)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_ok), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SettingsBottomSheet"
    }
}