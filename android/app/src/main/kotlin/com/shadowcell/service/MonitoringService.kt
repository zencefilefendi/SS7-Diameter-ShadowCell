package com.shadowcell.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.shadowcell.ShadowCellApp
import com.shadowcell.api.ShadowCellApiClient
import com.shadowcell.detectors.CellTowerMonitor
import com.shadowcell.detectors.NetworkDowngradeDetector
import com.shadowcell.detectors.SilentSmsDetector
import com.shadowcell.scoring.ContextCorrelationFilter
import com.shadowcell.scoring.MlAnomalyScorer
import com.shadowcell.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    private lateinit var cellTowerMonitor: CellTowerMonitor
    private lateinit var networkDowngradeDetector: NetworkDowngradeDetector
    private lateinit var silentSmsDetector: SilentSmsDetector
    private lateinit var contextCorrelationFilter: ContextCorrelationFilter
    private lateinit var apiClient: ShadowCellApiClient
    private lateinit var mlScorer: MlAnomalyScorer

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
        
        val app = application as ShadowCellApp
        val repository = app.repository
        val scorer = app.scorer

        // Initialize detectors and components
        cellTowerMonitor = CellTowerMonitor(this)
        networkDowngradeDetector = NetworkDowngradeDetector(this)
        silentSmsDetector = SilentSmsDetector(this)
        contextCorrelationFilter = ContextCorrelationFilter(this)
        apiClient = ShadowCellApiClient(this)
        mlScorer = MlAnomalyScorer(this)

        // Start detectors
        cellTowerMonitor.start()
        networkDowngradeDetector.start()
        silentSmsDetector.start()
        
        // Wire the sensor fusion pipeline
        serviceScope.launch {
            merge(
                cellTowerMonitor.events,
                networkDowngradeDetector.events,
                silentSmsDetector.events
            ).collect { rawEvent ->
                // Step 1: Context evaluation (Moving, Time of day, etc.)
                var evaluatedEvent = contextCorrelationFilter.evaluate(rawEvent)
                
                // Step 2: ML evaluation (predict probability based on dummy features for now)
                val mlProb = mlScorer.predictRisk(floatArrayOf(evaluatedEvent.score.toFloat()))
                if (mlProb > 0.7f) {
                    evaluatedEvent = evaluatedEvent.copy(score = minOf(100, (evaluatedEvent.score * 1.2).toInt()))
                }

                // Step 3: Ingest to Temporal Correlator
                scorer.ingest(evaluatedEvent)

                // Step 4: Persist to Room DB
                repository.save(evaluatedEvent)

                // Step 5: Send to Crowd-sourced Backend if high risk
                if (evaluatedEvent.score >= 40) {
                    apiClient.reportAnomaly(evaluatedEvent)
                }
            }
        }
        
        // Schedule WorkManager as a fallback (minimum interval is 15 minutes per Android OS limits)
        val workRequest = PeriodicWorkRequestBuilder<CellInfoWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the scope when service is destroyed
        serviceScope.coroutineContext[Job]?.cancel()
        cellTowerMonitor.stop()
        networkDowngradeDetector.stop()
        silentSmsDetector.stop()
        contextCorrelationFilter.destroy()
        mlScorer.close()
    }

    private fun createNotification(): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return NotificationCompat.Builder(this, ShadowCellApp.CHANNEL_MONITORING)
            .setContentTitle("ShadowCell İzliyor")
            .setContentText("Ağ ve hücre kulesi anomalileri takip ediliyor.")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}