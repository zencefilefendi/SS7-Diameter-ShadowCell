package com.shadowcell

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.shadowcell.scoring.AnomalyScorer
import com.shadowcell.storage.EventDatabase
import com.shadowcell.storage.EventRepository

class ShadowCellApp : Application() {

    lateinit var db: EventDatabase
        private set
    lateinit var repository: EventRepository
        private set
    lateinit var scorer: AnomalyScorer
        private set

    override fun onCreate() {
        super.onCreate()
        db = EventDatabase.get(this)
        repository = EventRepository(db)
        scorer = AnomalyScorer()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITORING,
                "ShadowCell İzleme",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Arka planda çalışan sensör bildirimi"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERT,
                "ShadowCell Uyarıları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Yüksek risk tespiti uyarıları"
            }
        )
    }

    companion object {
        const val CHANNEL_MONITORING = "shadowcell_monitoring"
        const val CHANNEL_ALERT = "shadowcell_alert"
    }
}
