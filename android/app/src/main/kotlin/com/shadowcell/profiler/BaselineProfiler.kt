package com.shadowcell.profiler

import android.content.Context
import android.content.SharedPreferences

class BaselineProfiler(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("baseline_prefs", Context.MODE_PRIVATE)

    companion object {
        const val CALIBRATION_PERIOD_MS = 48 * 60 * 60 * 1000L // 48 hours
        const val KEY_START_TIME = "calibration_start_time"
        const val KEY_NETWORK_TRANSITIONS = "network_transitions"
        const val KEY_TOWER_TRANSITIONS = "tower_transitions"
    }

    init {
        if (!prefs.contains(KEY_START_TIME)) {
            prefs.edit().putLong(KEY_START_TIME, System.currentTimeMillis()).apply()
        }
    }

    fun isCalibrating(): Boolean {
        val startTime = prefs.getLong(KEY_START_TIME, 0L)
        return (System.currentTimeMillis() - startTime) < CALIBRATION_PERIOD_MS
    }

    fun recordNetworkTransition() {
        if (isCalibrating()) {
            val current = prefs.getInt(KEY_NETWORK_TRANSITIONS, 0)
            prefs.edit().putInt(KEY_NETWORK_TRANSITIONS, current + 1).apply()
        }
    }

    fun recordTowerTransition() {
        if (isCalibrating()) {
            val current = prefs.getInt(KEY_TOWER_TRANSITIONS, 0)
            prefs.edit().putInt(KEY_TOWER_TRANSITIONS, current + 1).apply()
        }
    }

    fun getNetworkTransitionBaseline(): Int {
        // Return baseline per hour, min 1
        val transitions = prefs.getInt(KEY_NETWORK_TRANSITIONS, 0)
        return maxOf(1, transitions / 48)
    }

    fun getTowerTransitionBaseline(): Int {
        val transitions = prefs.getInt(KEY_TOWER_TRANSITIONS, 0)
        return maxOf(1, transitions / 48)
    }
}