package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK, SHADOW_MONARCH
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultStartScreen: String = "today",
    val showCompletedOnToday: Boolean = true,
    val startWeekOnMonday: Boolean = true,
    val enableReminderNotifications: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_START_SCREEN = stringPreferencesKey("default_start_screen")
        val SHOW_COMPLETED_ON_TODAY = booleanPreferencesKey("show_completed_on_today")
        val START_WEEK_ON_MONDAY = booleanPreferencesKey("start_week_on_monday")
        val ENABLE_REMINDER_NOTIFICATIONS = booleanPreferencesKey("enable_reminder_notifications")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val themeModeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeModeStr)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }

        UserPreferences(
            themeMode = themeMode,
            defaultStartScreen = preferences[PreferencesKeys.DEFAULT_START_SCREEN] ?: "today",
            showCompletedOnToday = preferences[PreferencesKeys.SHOW_COMPLETED_ON_TODAY] ?: true,
            startWeekOnMonday = preferences[PreferencesKeys.START_WEEK_ON_MONDAY] ?: true,
            enableReminderNotifications = preferences[PreferencesKeys.ENABLE_REMINDER_NOTIFICATIONS] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDefaultStartScreen(screen: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_START_SCREEN] = screen
        }
    }

    suspend fun setShowCompletedOnToday(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_COMPLETED_ON_TODAY] = show
        }
    }

    suspend fun setStartWeekOnMonday(startOnMonday: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.START_WEEK_ON_MONDAY] = startOnMonday
        }
    }

    suspend fun setEnableReminderNotifications(enable: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_REMINDER_NOTIFICATIONS] = enable
        }
    }

    fun isRemindersEnabledSync(): Boolean {
        return runBlocking {
            try {
                context.dataStore.data.map { preferences ->
                    preferences[PreferencesKeys.ENABLE_REMINDER_NOTIFICATIONS] ?: true
                }.first()
            } catch (e: Exception) {
                true
            }
        }
    }
}
