package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.UserPreferencesRepository

class MainActivity : ComponentActivity() {
    private var pendingTaskId by mutableStateOf<String?>(null)
    private var pendingNavigateAction by mutableStateOf<String?>(null)
    private var pendingReviewDate by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingTaskId = intent?.getStringExtra("NAVIGATE_TASK_ID")
        pendingNavigateAction = intent?.getStringExtra("NAVIGATE_ACTION")
        pendingReviewDate = intent?.getStringExtra("REVIEW_DATE")
        val prefsRepository = UserPreferencesRepository(applicationContext)

        setContent {
            val userPrefs by prefsRepository.userPreferencesFlow.collectAsStateWithLifecycle(
                initialValue = com.example.data.repository.UserPreferences()
            )

            MyApplicationTheme(themeMode = userPrefs.themeMode) {
                AppNavigation(
                    userPreferences = userPrefs,
                    preferencesRepository = prefsRepository,
                    initialTaskId = pendingTaskId,
                    onHandledInitialTask = { pendingTaskId = null },
                    initialNavigateAction = pendingNavigateAction,
                    initialReviewDate = pendingReviewDate,
                    onHandledInitialAction = {
                        pendingNavigateAction = null
                        pendingReviewDate = null
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        com.example.widget.KisekiWidgetUpdater.updateAllWidgets(applicationContext)
    }

    override fun onStop() {
        super.onStop()
        com.example.widget.KisekiWidgetUpdater.updateAllWidgets(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("NAVIGATE_TASK_ID")?.let { id ->
            pendingTaskId = id
        }
        intent.getStringExtra("NAVIGATE_ACTION")?.let { action ->
            pendingNavigateAction = action
        }
        intent.getStringExtra("REVIEW_DATE")?.let { date ->
            pendingReviewDate = date
        }
    }
}
