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
import com.example.t_learnappmobile.data.dictionary.Dictionary
import com.example.t_learnappmobile.data.dictionary.DictionaryManager
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.settings.SettingsManager
import com.example.t_learnappmobile.databinding.FragmentSettingsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SettingsBottomSheet : BottomSheetDialogFragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var dictionaryManager: DictionaryManager
    private lateinit var settingsManager: SettingsManager
    private var dictionaries: List<Dictionary> = emptyList()
    var onDictionaryChanged: (() -> Unit)? = null

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

        initManagers()
        setupUI()
        setupListeners()
        loadProfileData()
    }

    private fun initManagers() {
        settingsManager = SettingsManager(requireContext())
        dictionaryManager = settingsManager.getDictionaryManager()
        dictionaries = dictionaryManager.getDictionaries()
    }

    private fun setupUI() {

        setupDictionarySpinner()


        updateThemeSelection(settingsManager.getTheme())
    }

    private fun loadProfileData() {
        lifecycleScope.launch {
            val userId = ServiceLocator.tokenManager.getUserId()?.toInt() ?: 0
            val email = ServiceLocator.tokenManager.getUserEmail() ?: "user@example.com"

            // ✅ Используем email как имя (до 12 символов)
            val displayName = email.split("@").first()
                .replaceFirstChar { it.uppercase() }
                .take(12)

            binding.nameEditText.setText(displayName)
            binding.surnameEditText.setText("Игрок")  // Фиксированное
        }
    }


    private fun setupDictionarySpinner() {
        val dictionaryNames = dictionaries.map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            dictionaryNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.dictionarySpinner.adapter = adapter

        val userId = getUserId() ?: return
        val currentDictionary = dictionaryManager.getCurrentDictionary(userId)
        val position = dictionaries.indexOfFirst { it.id == currentDictionary.id }
        if (position != -1) binding.dictionarySpinner.setSelection(position)

        binding.dictionarySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedDictionary = dictionaries[position]
                val userId = getUserId() ?: return
                dictionaryManager.setCurrentDictionary(userId, selectedDictionary.id)
                onDictionaryChanged?.invoke()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun getUserId(): Int? {
        return ServiceLocator.tokenManager.getUserId()?.toInt()  // ✅ JWT!
    }

    private fun setupListeners() {

        binding.btnClose.setOnClickListener { dismiss() }


        binding.btnSaveProfile.setOnClickListener {
            val firstName = binding.nameEditText.text.toString().trim()
            val lastName = binding.surnameEditText.text.toString().trim()

            if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                lifecycleScope.launch {
                    settingsManager.updateUserProfile(firstName, lastName)
                    showSuccessDialog("Профиль обновлен!")
                }
            } else {
                showErrorDialog("Заполните имя и фамилию")
            }
        }


        binding.btnResetDictionary.setOnClickListener {
            val userId = getUserId() ?: return@setOnClickListener
            val currentDict = dictionaryManager.getCurrentDictionary(userId)
            showConfirmationDialog(
                title = getString(R.string.confirm_reset_dictionary_title),
                message = "${currentDict.name} будет очищен",
                onConfirm = {
                    lifecycleScope.launch {
                        settingsManager.clearDictionaryData(userId)
                        showSuccessDialog("Статистика словаря очищена")
                    }
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
