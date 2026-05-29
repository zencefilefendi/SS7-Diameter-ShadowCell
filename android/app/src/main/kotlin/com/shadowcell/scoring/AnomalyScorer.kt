package com.shadowcell.scoring

import java.util.ArrayDeque

/**
 * Temporal korelasyon motoru.
 *
 * Birden fazla sensörden gelen event'leri zaman pencereleri içinde
 * birleştirip toplam risk skoru hesaplar.
 *
 * Mantık:
 *  - Her event kendi skorunu taşır (0-100)
 *  - Aynı zaman penceresinde birden fazla event varsa çarpan uygulanır
 *  - Bazı kombinasyonlar özel yüksek-güven kuralları tetikler
 *  - Son 300 saniyedeki window skoru = mevcut risk seviyesi
 */
class AnomalyScorer {

    private val shortWindow = 60_000L    // 60 saniye: ani saldırı tespiti
    private val longWindow = 300_000L   // 5 dakika: genel risk seviyesi

    private val eventBuffer = ArrayDeque<ThreatEvent>()

    fun ingest(event: ThreatEvent): RiskSnapshot {
        eventBuffer.addLast(event)
        pruneOlderThan(longWindow)
        return computeSnapshot()
    }

    private fun computeSnapshot(): RiskSnapshot {
        val now = System.currentTimeMillis()
        val windowEvents = eventBuffer.filter { now - it.timestamp <= longWindow }

        if (windowEvents.isEmpty()) {
            return RiskSnapshot(
                totalScore = 0,
                level = RiskLevel.SAFE,
                contributingEvents = emptyList(),
                windowSeconds = (longWindow / 1000).toInt(),
            )
        }

        // Base: en yüksek tek event skoru
        var score = windowEvents.maxOf { it.score }

        // Short window korelasyon: 60s içinde birden fazla event = çarpan
        val shortEvents = windowEvents.filter { now - it.timestamp <= shortWindow }
        if (shortEvents.size >= 2) {
            val shortBonus = (shortEvents.sumOf { it.score } * 0.4).toInt()
            score += shortBonus
        }

        // Özel kombinasyon kuralları
        score += applyComboRules(windowEvents, shortEvents)

        // 100 ile kısıtla
        score = score.coerceIn(0, 100)

        return RiskSnapshot(
            totalScore = score,
            level = score.toRiskLevel(),
            contributingEvents = windowEvents,
            windowSeconds = (longWindow / 1000).toInt(),
        )
    }

    private fun applyComboRules(longWindow: List<ThreatEvent>, shortWindow: List<ThreatEvent>): Int {
        var bonus = 0
        val types = shortWindow.map { it.type }.toSet()

        // Kural 1: Downgrade + Silent SMS beraber = çok yüksek güven saldırı imzası
        if (types.contains(EventType.NETWORK_DOWNGRADE) &&
            types.contains(EventType.SILENT_SMS_CANDIDATE)
        ) {
            bonus += 25
        }

        // Kural 2: Downgrade + Location Update Burst = SS7 konum izleme imzası
        if (types.contains(EventType.NETWORK_DOWNGRADE) &&
            types.contains(EventType.LOCATION_UPDATE_BURST)
        ) {
            bonus += 20
        }

        // Kural 3: Fleeting tower + Signal Drop = aktif IMSI catcher
        if (types.contains(EventType.CELL_TOWER_ANOMALY) &&
            types.contains(EventType.SIGNAL_DROP_HANDOFF)
        ) {
            bonus += 20
        }

        // Kural 4: Tüm üç ana kategori birden → kapsamlı saldırı
        val hasDowngrade = types.contains(EventType.NETWORK_DOWNGRADE)
        val hasSms = types.contains(EventType.SILENT_SMS_CANDIDATE)
        val hasTower = types.contains(EventType.CELL_TOWER_ANOMALY)
        if (hasDowngrade && hasSms && hasTower) {
            bonus += 15  // Üstüne ek bonus
        }

        return bonus
    }

    private fun pruneOlderThan(windowMs: Long) {
        val cutoff = System.currentTimeMillis() - windowMs
        while (eventBuffer.isNotEmpty() && eventBuffer.peekFirst().timestamp < cutoff) {
            eventBuffer.removeFirst()
        }
    }

    fun currentRisk(): RiskSnapshot {
        pruneOlderThan(longWindow)
        return computeSnapshot()
    }

    fun clearHistory() {
        eventBuffer.clear()
    }
}
