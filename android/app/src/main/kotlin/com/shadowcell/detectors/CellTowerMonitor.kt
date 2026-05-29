package com.shadowcell.detectors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.shadowcell.scoring.EventType
import com.shadowcell.scoring.ThreatEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * IMSI Catcher heuristik tespiti.
 *
 * Her 10 saniyede bir görünen cell tower'ları kaydeder.
 * Şüpheli pattern'lar:
 *  - Sadece kısa süre görülen (< 60s) bilinmeyen tower
 *  - Handoff sonrası sinyal düşüşü (sahte tower genellikle zayıf)
 *  - Modern bölgede 2G-only tower görülmesi
 *  - Aynı MNC/MCC ama olağandışı LAC/TAC
 */
class CellTowerMonitor(private val context: Context) {

    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)
    private val _events = MutableSharedFlow<ThreatEvent>(extraBufferCapacity = 32)
    val events: Flow<ThreatEvent> = _events

    private val seenTowers = mutableMapOf<String, TowerRecord>()
    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private data class TowerRecord(
        val cellId: String,
        val firstSeen: Long,
        var lastSeen: Long,
        var minSignalDbm: Int,
        var maxSignalDbm: Int,
        var snapshotCount: Int,
    )

    fun start() {
        pollingJob = scope.launch {
            while (isActive) {
                pollCellInfo()
                delay(10_000L)  // 10 saniyede bir
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
    }

    private fun pollCellInfo() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val cells = try {
            telephonyManager.allCellInfo ?: return
        } catch (e: Exception) {
            return
        }

        val now = System.currentTimeMillis()

        for (cell in cells) {
            val (cellId, signalDbm, generation) = extractCellInfo(cell) ?: continue
            val record = seenTowers.getOrPut(cellId) {
                TowerRecord(
                    cellId = cellId,
                    firstSeen = now,
                    lastSeen = now,
                    minSignalDbm = signalDbm,
                    maxSignalDbm = signalDbm,
                    snapshotCount = 0,
                )
            }
            record.lastSeen = now
            record.snapshotCount++
            if (signalDbm < record.minSignalDbm) record.minSignalDbm = signalDbm
            if (signalDbm > record.maxSignalDbm) record.maxSignalDbm = signalDbm

            // IMSI Catcher pattern 1: 2G tower modern ortamda
            if (generation == 2 && isModernArea()) {
                _events.tryEmit(
                    ThreatEvent(
                        type = EventType.CELL_TOWER_ANOMALY,
                        rawValue = "2G tower in modern area: $cellId",
                        score = 45,
                        context = "signal=${signalDbm}dBm",
                    )
                )
            }

            // IMSI Catcher pattern 2: Sinyal normalden çok daha zayıf
            if (signalDbm < -110 && cell.isRegistered) {
                _events.tryEmit(
                    ThreatEvent(
                        type = EventType.SIGNAL_DROP_HANDOFF,
                        rawValue = "Registered on very weak tower: $cellId @ ${signalDbm}dBm",
                        score = 35,
                        context = "gen=$generation",
                    )
                )
            }
        }

        // Fleeting tower: kısa süre görünüp kaybolan
        pruneAndCheckFleetingTowers(now)
    }

    private fun pruneAndCheckFleetingTowers(now: Long) {
        val fleetingThreshold = 90_000L  // 90 saniyeden az görünen
        val toRemove = mutableListOf<String>()

        for ((id, record) in seenTowers) {
            val wasVisible = now - record.lastSeen < 15_000L  // az önce görüldü
            val isGone = now - record.lastSeen > 60_000L

            if (isGone) {
                val visibleDuration = record.lastSeen - record.firstSeen
                if (visibleDuration < fleetingThreshold && record.snapshotCount < 5) {
                    // Tower sadece kısa süre göründü ve kayboldu
                    _events.tryEmit(
                        ThreatEvent(
                            type = EventType.CELL_TOWER_ANOMALY,
                            rawValue = "Fleeting tower: $id visible ${visibleDuration/1000}s",
                            score = 50,
                            context = "snapshots=${record.snapshotCount} signal_range=${record.minSignalDbm}..${record.maxSignalDbm}dBm",
                        )
                    )
                }
                toRemove.add(id)
            }
        }
        toRemove.forEach { seenTowers.remove(it) }
    }

    private data class CellExtract(val id: String, val signalDbm: Int, val generation: Int)

    private fun extractCellInfo(cell: android.telephony.CellInfo): CellExtract? {
        return when (cell) {
            is CellInfoLte -> {
                val id = (cell.cellIdentity as CellIdentityLte).let {
                    "LTE-${it.mcc}-${it.mnc}-${it.tac}-${it.ci}"
                }
                val dbm = cell.cellSignalStrength.dbm
                CellExtract(id, dbm, 4)
            }
            is CellInfoWcdma -> {
                val ci = cell.cellIdentity
                val id = "WCDMA-${ci.mcc}-${ci.mnc}-${ci.lac}-${ci.cid}"
                val dbm = cell.cellSignalStrength.dbm
                CellExtract(id, dbm, 3)
            }
            is CellInfoNr -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val id = (cell.cellIdentity as CellIdentityNr).let {
                        "NR-${it.mcc}-${it.mnc}-${it.tac}-${it.nci}"
                    }
                    val dbm = cell.cellSignalStrength.dbm
                    CellExtract(id, dbm, 5)
                } else null
            }
            else -> null
        }
    }

    // Placeholder: Gerçekte kullanıcının konumuna bakıp 2G'nin beklenmeyeceğini anlamak gerekir
    // TODO: Ülke/şehir bazlı 2G shutdown listesi ile entegre et
    private fun isModernArea(): Boolean {
        val networkOperator = telephonyManager.networkOperator ?: return false
        // Türkiye, AB, ABD gibi ülkelerde 2G shutdown yaygın
        return networkOperator.startsWith("28601") ||  // Turkcell
               networkOperator.startsWith("28602") ||  // Vodafone TR
               networkOperator.startsWith("28603")     // Türk Telekom
    }
}
