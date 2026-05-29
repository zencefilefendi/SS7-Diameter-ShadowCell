package com.shadowcell.detectors

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import com.shadowcell.scoring.EventType
import com.shadowcell.scoring.ThreatEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.ArrayDeque

class NetworkDowngradeDetector(context: Context) {

    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)
    private val _events = Channel<ThreatEvent>(Channel.BUFFERED)
    val events: Flow<ThreatEvent> = _events.receiveAsFlow()

    // Son 10 geçişi tut; pattern analizi için
    private val transitionHistory = ArrayDeque<Pair<Long, Int>>(10)
    private var lastNetworkType: Int = TelephonyManager.NETWORK_TYPE_UNKNOWN

    private val listener = object : PhoneStateListener() {
        @Deprecated("Deprecated in API 31 but required for older targets")
        override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
            handleNetworkTypeChange(networkType)
        }

        @Deprecated("Deprecated in API 31")
        override fun onServiceStateChanged(serviceState: ServiceState) {
            // ServiceState değişimi = ağ yeniden kaydolma; sık olursa şüpheli
            val now = System.currentTimeMillis()
            recentServiceStateChanges.add(now)
            pruneOlderThan(recentServiceStateChanges, 60_000L)
            if (recentServiceStateChanges.size > 4) {
                _events.trySend(
                    ThreatEvent(
                        type = EventType.LOCATION_UPDATE_BURST,
                        rawValue = "ServiceState changes: ${recentServiceStateChanges.size}/min",
                        score = 30,
                        context = "state=${serviceState.state}",
                    )
                )
            }
        }
    }

    private val recentServiceStateChanges = ArrayDeque<Long>()

    fun start() {
        @Suppress("DEPRECATION")
        telephonyManager.listen(
            listener,
            PhoneStateListener.LISTEN_DATA_CONNECTION_STATE or
            PhoneStateListener.LISTEN_SERVICE_STATE
        )
    }

    fun stop() {
        @Suppress("DEPRECATION")
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
    }

    private fun handleNetworkTypeChange(newType: Int) {
        val now = System.currentTimeMillis()
        val prev = lastNetworkType

        if (prev == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            lastNetworkType = newType
            return
        }

        val prevGen = networkGeneration(prev)
        val newGen = networkGeneration(newType)

        if (newGen < prevGen) {
            // Downgrade
            transitionHistory.addLast(Pair(now, newGen))
            if (transitionHistory.size > 10) transitionHistory.removeFirst()

            val score = when {
                prevGen == 4 && newGen == 3 -> 40
                prevGen == 4 && newGen == 2 -> 55  // 4G→2G doğrudan = çok şüpheli
                prevGen == 3 && newGen == 2 -> 30
                else -> 20
            }

            _events.trySend(
                ThreatEvent(
                    type = EventType.NETWORK_DOWNGRADE,
                    rawValue = "${prevGen}G→${newGen}G",
                    score = score,
                    context = "prevType=$prev newType=$newType",
                )
            )
        } else if (newGen > prevGen && wasRecentDowngrade(now)) {
            // Hızlı upgrade: downgrade'den kısa süre sonra geri dönüş = sahte tower pattern
            _events.trySend(
                ThreatEvent(
                    type = EventType.NETWORK_UPGRADE_FAST,
                    rawValue = "${prevGen}G→${newGen}G (fast return)",
                    score = 25,
                    context = "downgrade+upgrade within 120s",
                )
            )
        }

        lastNetworkType = newType
    }

    // Son 120 saniyede downgrade var mıydı?
    private fun wasRecentDowngrade(now: Long): Boolean {
        val cutoff = now - 120_000L
        return transitionHistory.any { (ts, gen) -> ts > cutoff && gen <= 3 }
    }

    private fun pruneOlderThan(queue: ArrayDeque<Long>, windowMs: Long) {
        val cutoff = System.currentTimeMillis() - windowMs
        while (queue.isNotEmpty() && queue.peekFirst()!! < cutoff) queue.removeFirst()
    }

    private fun networkGeneration(networkType: Int): Int = when (networkType) {
        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_CDMA,
        TelephonyManager.NETWORK_TYPE_1xRTT,
        TelephonyManager.NETWORK_TYPE_IDEN -> 2

        TelephonyManager.NETWORK_TYPE_UMTS,
        TelephonyManager.NETWORK_TYPE_EVDO_0,
        TelephonyManager.NETWORK_TYPE_EVDO_A,
        TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_EVDO_B,
        TelephonyManager.NETWORK_TYPE_EHRPD,
        TelephonyManager.NETWORK_TYPE_HSPAP,
        TelephonyManager.NETWORK_TYPE_TD_SCDMA -> 3

        TelephonyManager.NETWORK_TYPE_LTE,
        TelephonyManager.NETWORK_TYPE_IWLAN -> 4

        TelephonyManager.NETWORK_TYPE_NR -> 5

        else -> 0
    }
}
