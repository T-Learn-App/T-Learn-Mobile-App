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

    private lateinit var settingsManager: SettingsManager
    private lateinit var dictionaryManager: DictionaryManager
    private var currentTheme: Int = SettingsManager.THEME_SYSTEM

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

        settingsManager = SettingsManager(requireContext())
        dictionaryManager = settingsManager.getDictionaryManager()
        dictionaries = dictionaryManager.getDictionaries()
        currentTheme = settingsManager.getTheme()

        setupDictionarySpinner()
        updateThemeSelection(currentTheme)
        setupListeners()
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
        if (position >= 0) {
            binding.dictionarySpinner.setSelection(position)
        }

        binding.dictionarySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedDictionary = dictionaries[position]
                    val userId = getUserId() ?: return
                    dictionaryManager.setCurrentDictionary(userId, selectedDictionary.id)
                    reloadWordsForNewDictionary(userId, selectedDictionary.vocabularyId)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun getUserId(): Int? {
        return runBlocking {
            ServiceLocator.tokenManager.getUserData().firstOrNull()?.id
        }
    }



    private fun reloadWordsForNewDictionary(userId: Int, vocabularyId: Int) {
        lifecycleScope.launch {
            ServiceLocator.wordRepository.fetchWordBatch(userId, vocabularyId, batchSize = 10)
            onDictionaryChanged?.invoke()
        }
    }


    private fun setupListeners() {
        binding.btnClose.setOnClickListener { dismiss() }

        binding.btnResetDictionary.setOnClickListener {
            val userId = getUserId() ?: return@setOnClickListener
            val currentDict = dictionaryManager.getCurrentDictionary(userId)
            showConfirmationDialog(
                title = getString(R.string.confirm_reset_dictionary_title),
                message = "Удалить статистику словаря \"${currentDict.name}\"?",
                onConfirm = {
                    settingsManager.clearDictionaryData(userId)
                    showSuccessDialog(getString(R.string.dictionary_has_been_reset))
                }
            )
        }

        binding.btnResetAll.setOnClickListener {
            showConfirmationDialog(
                title = getString(R.string.confirm_reset_all_title),
                message = getString(R.string.confirm_reset_all_message),
                onConfirm = {
                    settingsManager.clearAllData()
                    binding.dictionarySpinner.setSelection(0)
                    currentTheme = SettingsManager.THEME_SYSTEM
                    updateThemeSelection(currentTheme)
                    showSuccessDialog(getString(R.string.data_has_been_reset))
                }
            )
        }

        binding.darkThemeButton.setOnClickListener {
            currentTheme = SettingsManager.THEME_DARK
            settingsManager.setTheme(currentTheme)
            updateThemeSelection(currentTheme)
        }

        binding.lightThemeButton.setOnClickListener {
            currentTheme = SettingsManager.THEME_LIGHT
            settingsManager.setTheme(currentTheme)
            updateThemeSelection(currentTheme)
        }
    }

    private fun updateThemeSelection(theme: Int) {
        when (theme) {
            AppCompatDelegate.MODE_NIGHT_NO -> {
                binding.lightThemeButton.setBackgroundResource(R.drawable.selected_theme_bg)
                binding.darkThemeButton.setBackgroundResource(android.R.color.transparent)
            }
            AppCompatDelegate.MODE_NIGHT_YES -> {
                binding.lightThemeButton.setBackgroundResource(android.R.color.transparent)
                binding.darkThemeButton.setBackgroundResource(R.drawable.selected_theme_bg)
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
            .setTitle(getString(R.string.success))
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
