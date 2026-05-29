package com.shadowcell.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shadowcell.ShadowCellApp
import com.shadowcell.scoring.RiskSnapshot
import com.shadowcell.scoring.ThreatEvent
import com.shadowcell.widget.RiskWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ShadowCellApp
    private val repository = app.repository
    private val scorer = app.scorer

    private val _riskSnapshot = MutableStateFlow(scorer.currentRisk())
    val riskSnapshot: StateFlow<RiskSnapshot> = _riskSnapshot.asStateFlow()

    private val _recentEvents = MutableStateFlow<List<ThreatEvent>>(emptyList())
    val recentEvents: StateFlow<List<ThreatEvent>> = _recentEvents.asStateFlow()

    init {
        viewModelScope.launch {
            repository.recentEvents.collectLatest { events ->
                _recentEvents.value = events
                updateRiskAndWidget()
            }
        }

        // Periodic risk update even if no new events arrive
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000)
                updateRiskAndWidget()
            }
        }
    }

    private fun updateRiskAndWidget() {
        val newRisk = scorer.currentRisk()
        _riskSnapshot.value = newRisk
        
        // Notify widget to update
        val intent = Intent(app, RiskWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = android.appwidget.AppWidgetManager.getInstance(app)
            .getAppWidgetIds(android.content.ComponentName(app, RiskWidgetProvider::class.java))
        intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        app.sendBroadcast(intent)
    }

    fun markFalsePositive(event: ThreatEvent) {
        viewModelScope.launch {
            val updatedEvent = event.copy(score = 0, confirmed = false, context = event.context + " [User Marked False Positive]")
            repository.update(updatedEvent)
        }
    }

    suspend fun getExportData(): List<ThreatEvent> {
        return repository.getEventsForExport(7)
    }
}