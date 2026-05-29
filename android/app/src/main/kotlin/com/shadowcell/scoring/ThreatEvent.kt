package com.shadowcell.scoring

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EventType {
    NETWORK_DOWNGRADE,      // 4G → 3G veya 3G → 2G
    NETWORK_UPGRADE_FAST,   // Çok hızlı upgrade (downgrade sonrası)
    SILENT_SMS_CANDIDATE,   // Delivery receipt ama inbox boş
    CELL_TOWER_ANOMALY,     // Bilinmeyen/zayıf tower handoff
    LOCATION_UPDATE_BURST,  // Çok sık location update
    SIGNAL_DROP_HANDOFF,    // Handoff sonrası sinyal düşüşü
    PAGING_ANOMALY,         // Beklenmedik device paging
}

enum class RiskLevel { SAFE, MEDIUM, HIGH, CRITICAL }

@Entity(tableName = "threat_events")
data class ThreatEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: EventType,
    val rawValue: String,       // Ham veri: "4G→3G", "CID:12345", vb.
    val score: Int,             // Bu event'in katkısı (0-100)
    val context: String = "",   // Ek bağlam: lokasyon, sinyal gücü, vb.
    val confirmed: Boolean = false,  // Kullanıcı "bu gerçekti" dedi mi
)

data class RiskSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val totalScore: Int,
    val level: RiskLevel,
    val contributingEvents: List<ThreatEvent>,
    val windowSeconds: Int,
)

fun Int.toRiskLevel(): RiskLevel = when {
    this <= 30 -> RiskLevel.SAFE
    this <= 65 -> RiskLevel.MEDIUM
    this <= 85 -> RiskLevel.HIGH
    else -> RiskLevel.CRITICAL
}
