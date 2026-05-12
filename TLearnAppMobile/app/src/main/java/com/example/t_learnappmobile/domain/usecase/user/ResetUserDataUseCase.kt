// domain/usecase/user/ResetUserDataUseCase.kt
package com.example.t_learnappmobile.domain.usecase.user

import com.example.t_learnappmobile.data.sync.SyncManager
import com.example.t_learnappmobile.domain.repository.UserRepository
import com.example.t_learnappmobile.domain.repository.WordRepository

class ResetUserDataUseCase(
    private val userRepository: UserRepository,
    private val wordRepository: WordRepository,
    private val syncManager: SyncManager
) {
    suspend operator fun invoke(userId: String): Result<Unit> {
        return try {
            // Сбрасываем прогресс всех слов (НЕ удаляем игровые результаты и очки)
            wordRepository.resetAllProgress(userId)

            // Запускаем синхронизацию для сохранения изменений
            syncManager.syncPendingChanges()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}