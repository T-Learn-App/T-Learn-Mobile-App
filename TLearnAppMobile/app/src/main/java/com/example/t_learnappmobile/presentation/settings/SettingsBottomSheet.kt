package com.example.t_learnappmobile.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.settings.SettingsManager
import com.example.t_learnappmobile.databinding.FragmentSettingsBinding
import com.example.t_learnappmobile.model.Dictionary
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsBottomSheet : BottomSheetDialogFragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsManager: SettingsManager

    var onDictionaryChanged: ((String) -> Unit)? = null
    var onStatisticsReset: (() -> Unit)? = null

    private var dictionaries = listOf<Dictionary>()

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

        dialog?.setCanceledOnTouchOutside(true)

        loadDictionaries()
        setupUI()
        setupListeners()
    }

    private fun loadDictionaries() {
        lifecycleScope.launch {
            showLoading(true)
            dictionaries = ServiceLocator.wordRepository.getDictionaries()
            if (dictionaries.isNotEmpty()) {
                setupDictionarySpinner()
            }
            showLoading(false)
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

        val currentDictId = settingsManager.getCurrentCategoryId()
        val position = dictionaries.indexOfFirst { it.id == currentDictId }
        if (position != -1) {
            binding.dictionarySpinner.setSelection(position)
        }
    }

    private fun setupUI() {
        binding.nameEditText.setText(settingsManager.getFirstName())
        binding.surnameEditText.setText(settingsManager.getLastName())
        updateThemeSelection(settingsManager.getTheme())
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener { dismiss() }

        binding.dictionarySpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedDict = dictionaries[position]
                    settingsManager.setCurrentCategoryId(selectedDict.id)
                    settingsManager.setCurrentDictionaryName(selectedDict.name)
                    onDictionaryChanged?.invoke(selectedDict.id)
                    Toast.makeText(
                        requireContext(),
                        "Выбран словарь: ${selectedDict.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

        binding.btnSaveProfile.setOnClickListener {
            val firstName = binding.nameEditText.text.toString().trim()
            val lastName = binding.surnameEditText.text.toString().trim()

            if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                lifecycleScope.launch {
                    showLoading(true)
                    val success = settingsManager.updateUserProfile(firstName, lastName)
                    showLoading(false)

                    if (success) {
                        Toast.makeText(requireContext(), "Профиль обновлен!", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Ошибка сохранения профиля",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Заполните имя и фамилию", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.btnResetDictionary.setOnClickListener {
            showConfirmationDialog(
                "Сброс статистики словаря",
                "Весь прогресс изучения слов в текущем словаре будет безвозвратно удален."
            ) {
                lifecycleScope.launch {
                    resetDictionaryStatistics()
                }
            }
        }

        binding.btnResetAll.setOnClickListener {
            showConfirmationDialog(
                "Сброс всех данных",
                "Будут удалены:\n• Прогресс изучения всех слов\n• Результаты игр\n• Все настройки приложения\n\nЭто действие нельзя отменить."
            ) {
                lifecycleScope.launch {
                    resetAllData()
                }
            }
        }

        binding.lightThemeButton.setOnClickListener {
            settingsManager.setTheme(SettingsManager.THEME_LIGHT)
            updateThemeSelection(SettingsManager.THEME_LIGHT)
            AppCompatDelegate.setDefaultNightMode(SettingsManager.THEME_LIGHT)
        }

        binding.darkThemeButton.setOnClickListener {
            settingsManager.setTheme(SettingsManager.THEME_DARK)
            updateThemeSelection(SettingsManager.THEME_DARK)
            AppCompatDelegate.setDefaultNightMode(SettingsManager.THEME_DARK)
        }
    }

    private suspend fun resetDictionaryStatistics() {
        val userId = ServiceLocator.firebaseAuthManager.getUserId()
        if (userId == null) {
            Toast.makeText(
                requireContext(),
                "Ошибка: пользователь не авторизован",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val currentDictId = settingsManager.getCurrentCategoryId()

        showLoading(true)
        try {
            val wordsSnapshot = ServiceLocator.firestore.collection("words")
                .whereEqualTo("dictionaryId", currentDictId)
                .get()
                .await()

            var resetCount = 0
            for (doc in wordsSnapshot.documents) {
                val userWordDocId = "${userId}_${doc.id}"
                val now = System.currentTimeMillis()

                ServiceLocator.firestore.collection("user_words")
                    .document(userWordDocId)
                    .set(
                        mapOf(
                            "userId" to userId,
                            "wordId" to doc.id,
                            "dictionaryId" to currentDictId,
                            "stage" to 0,
                            "nextReviewDate" to now,
                            "lastReviewDate" to null,
                            "totalViews" to 0,
                            "correctCount" to 0,
                            "incorrectCount" to 0
                        )
                    )
                    .await()
                resetCount++
            }

            showLoading(false)
            Toast.makeText(
                requireContext(),
                "Статистика словаря очищена! ($resetCount слов)",
                Toast.LENGTH_LONG
            ).show()

            onStatisticsReset?.invoke()
            onDictionaryChanged?.invoke(currentDictId)

        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun resetAllData() {
        val userId = ServiceLocator.firebaseAuthManager.getUserId()
        if (userId == null) {
            Toast.makeText(
                requireContext(),
                "Ошибка: пользователь не авторизован",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        showLoading(true)
        try {
            // Удаляем все user_words для пользователя
            val userWordsSnapshot = ServiceLocator.firestore.collection("user_words")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            for (doc in userWordsSnapshot.documents) {
                doc.reference.delete().await()
            }

            // Удаляем результаты игр
            val gamesSnapshot = ServiceLocator.firestore.collection("game_results")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            for (doc in gamesSnapshot.documents) {
                doc.reference.delete().await()
            }

            // Обнуляем лидерборд
            val leaderboardRef =
                ServiceLocator.firestore.collection("leaderboard").document(userId)
            if (leaderboardRef.get().await().exists()) {
                leaderboardRef.delete().await()
            }

            // Обнуляем счет пользователя
            ServiceLocator.firestore.collection("users")
                .document(userId)
                .update("totalScore", 0)
                .await()

            settingsManager.clearAllData()

            if (dictionaries.isNotEmpty()) {
                settingsManager.setCurrentCategoryId(dictionaries.first().id)
                settingsManager.setCurrentDictionaryName(dictionaries.first().name)
                onDictionaryChanged?.invoke(dictionaries.first().id)
            }

            binding.nameEditText.setText("")
            binding.surnameEditText.setText("")

            showLoading(false)
            Toast.makeText(
                requireContext(),
                "Все данные очищены!",
                Toast.LENGTH_LONG
            ).show()

            onStatisticsReset?.invoke()

        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateThemeSelection(theme: Int) {
        binding.lightThemeButton.setBackgroundResource(
            if (theme == SettingsManager.THEME_LIGHT) R.drawable.selected_theme_bg
            else android.R.color.transparent
        )
        binding.darkThemeButton.setBackgroundResource(
            if (theme == SettingsManager.THEME_DARK) R.drawable.selected_theme_bg
            else android.R.color.transparent
        )
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showConfirmationDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Да, удалить") { _, _ -> onConfirm() }
            .setNegativeButton("Отмена", null)
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