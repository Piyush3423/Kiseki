package com.example.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object KisekiWidgetUpdater {

    fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TodayWidget().updateAll(appContext)
                QuickTaskWidget().updateAll(appContext)
            } catch (_: Exception) {
                // Ignore if widgets are not installed on home screen
            }
        }
    }
}
