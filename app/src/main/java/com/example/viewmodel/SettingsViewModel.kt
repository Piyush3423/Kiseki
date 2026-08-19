package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.KisekiDatabase
import com.example.data.repository.BackupData
import com.example.data.repository.BackupRepository
import com.example.data.repository.ThemeMode
import com.example.data.repository.UserPreferences
import com.example.data.repository.UserPreferencesRepository
import com.example.util.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val context: Context
) : ViewModel() {

    private val backupRepository = BackupRepository(context.applicationContext)

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _pendingBackupData = MutableStateFlow<BackupData?>(null)
    val pendingBackupData: StateFlow<BackupData?> = _pendingBackupData.asStateFlow()

    private val _isOperationInProgress = MutableStateFlow(false)
    val isOperationInProgress: StateFlow<Boolean> = _isOperationInProgress.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setDefaultStartScreen(screen: String) {
        viewModelScope.launch {
            preferencesRepository.setDefaultStartScreen(screen)
        }
    }

    fun setShowCompletedOnToday(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowCompletedOnToday(show)
        }
    }

    fun setStartWeekOnMonday(startOnMonday: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setStartWeekOnMonday(startOnMonday)
        }
    }

    fun setEnableReminderNotifications(enable: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setEnableReminderNotifications(enable)
            val db = KisekiDatabase.getDatabase(context)
            val tasks = db.activityTaskDao().getAllTasksOneShot()
            tasks.forEach { task ->
                ReminderScheduler.scheduleOrCancelReminder(context, task)
            }
            if (enable) {
                ReminderScheduler.scheduleEndOfDayReview(context)
            } else {
                ReminderScheduler.cancelEndOfDayReview(context)
            }
        }
    }

    fun setEnableEndOfDayReview(enable: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setEnableEndOfDayReview(enable)
            if (enable) {
                ReminderScheduler.scheduleEndOfDayReview(context)
            } else {
                ReminderScheduler.cancelEndOfDayReview(context)
            }
        }
    }

    fun setEndOfDayReviewTime(time: String) {
        viewModelScope.launch {
            preferencesRepository.setEndOfDayReviewTime(time)
            ReminderScheduler.scheduleEndOfDayReview(context, time)
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _isOperationInProgress.value = true
            val result = backupRepository.exportBackup(uri)
            _isOperationInProgress.value = false
            result.fold(
                onSuccess = { count ->
                    _uiMessage.value = "Backup exported successfully ($count tasks saved)."
                },
                onFailure = { e ->
                    _uiMessage.value = "Export failed: ${e.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    fun onFileSelectedForImport(uri: Uri) {
        viewModelScope.launch {
            _isOperationInProgress.value = true
            val result = backupRepository.validateAndReadBackup(uri)
            _isOperationInProgress.value = false
            result.fold(
                onSuccess = { data ->
                    _pendingBackupData.value = data
                },
                onFailure = { e ->
                    _uiMessage.value = e.localizedMessage ?: "Invalid backup file"
                }
            )
        }
    }

    fun confirmImportReplace() {
        val data = _pendingBackupData.value ?: return
        _pendingBackupData.value = null
        viewModelScope.launch {
            _isOperationInProgress.value = true
            val result = backupRepository.restoreReplace(data)
            _isOperationInProgress.value = false
            result.fold(
                onSuccess = { count ->
                    _uiMessage.value = "Restore complete. $count tasks replaced."
                },
                onFailure = { e ->
                    _uiMessage.value = "Restore failed: ${e.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    fun confirmImportMerge() {
        val data = _pendingBackupData.value ?: return
        _pendingBackupData.value = null
        viewModelScope.launch {
            _isOperationInProgress.value = true
            val result = backupRepository.restoreMerge(data)
            _isOperationInProgress.value = false
            result.fold(
                onSuccess = { count ->
                    _uiMessage.value = "Merge complete. $count tasks added or updated."
                },
                onFailure = { e ->
                    _uiMessage.value = "Merge failed: ${e.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    fun cancelImport() {
        _pendingBackupData.value = null
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }
}

class SettingsViewModelFactory(
    private val preferencesRepository: UserPreferencesRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(preferencesRepository, context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
