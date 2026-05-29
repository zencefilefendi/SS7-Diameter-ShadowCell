package com.shadowcell.storage

import com.shadowcell.scoring.ThreatEvent
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class EventRepository(private val db: EventDatabase) {

    private val dao = db.eventDao()

    val recentEvents: Flow<List<ThreatEvent>> = dao.getRecentEvents(200)

    suspend fun save(event: ThreatEvent): Long = dao.insert(event)

    suspend fun update(event: ThreatEvent) = dao.update(event)

    suspend fun getEventsForExport(days: Int = 7): List<ThreatEvent> {
        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        return dao.getEventsSince(since)
    }

    suspend fun getHighRiskEvents(hours: Int = 24): List<ThreatEvent> {
        val since = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours.toLong())
        return dao.getHighRiskEventsSince(since, minScore = 40)
    }

    suspend fun pruneOldEvents(keepDays: Int = 30) {
        val before = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(keepDays.toLong())
        dao.deleteOlderThan(before)
    }

    suspend fun eventCountLast24h(): Int {
        val since = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        return dao.countSince(since)
    }
}
