package com.shadowcell.detectors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Telephony
import com.shadowcell.scoring.EventType
import com.shadowcell.scoring.ThreatEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Type-0 (Silent) SMS tespiti.
 *
 * Android, Type-0 SMS'i kullanıcıya göstermez. Ancak bazı durumlarda:
 * - DATA_SMS_RECEIVED_ACTION (port 0) ateşlenir
 * - Delivery report sayısı ile inbox sayısı arasında delta oluşur
 * - WAP_PUSH_RECEIVED bazı carrier'larda tetiklenir
 *
 * Bu detector, teslimat raporları ve gelen SMS arasındaki tutarsızlığı izler.
 */
class SilentSmsDetector(private val context: Context) {

    private val _events = Channel<ThreatEvent>(Channel.BUFFERED)
    val events: Flow<ThreatEvent> = _events.receiveAsFlow()

    private var inboxCountSnapshot: Int = 0
    private var deliveryReceiptCount: Int = 0

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                    // Normal SMS geldi; baseline güncelle
                    inboxCountSnapshot = readInboxCount()
                }
                Telephony.Sms.Intents.SMS_DELIVER_ACTION -> {
                    // Delivery notification; inbox'ta karşılığı var mı?
                    deliveryReceiptCount++
                    checkForSilentSms()
                }
                "android.provider.Telephony.SMS_RECEIVED" -> {
                    // Bazı cihazlarda Type-0 bu broadcast'i tetikler ama
                    // PDU tipini kontrol etmemiz lazım
                    checkPduType(intent)
                }
            }
        }
    }

    fun start() {
        inboxCountSnapshot = readInboxCount()
        val filter = IntentFilter().apply {
            addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
            addAction(Telephony.Sms.Intents.SMS_DELIVER_ACTION)
            addAction("android.provider.Telephony.SMS_RECEIVED")
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }
        context.registerReceiver(smsReceiver, filter)
    }

    fun stop() {
        runCatching { context.unregisterReceiver(smsReceiver) }
    }

    private fun checkForSilentSms() {
        val currentInbox = readInboxCount()
        // Delivery receipt var ama inbox artmadı = potansiyel silent SMS
        if (currentInbox <= inboxCountSnapshot && deliveryReceiptCount > 0) {
            _events.trySend(
                ThreatEvent(
                    type = EventType.SILENT_SMS_CANDIDATE,
                    rawValue = "delivery_receipts=$deliveryReceiptCount inbox_delta=${currentInbox - inboxCountSnapshot}",
                    score = 35,
                    context = "inbox=$currentInbox snapshot=$inboxCountSnapshot",
                )
            )
        }
        inboxCountSnapshot = currentInbox
    }

    private fun checkPduType(intent: Intent) {
        // PDU içeriğini parse et; Type-0 = Message-Class 0
        @Suppress("DEPRECATION")
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        for (msg in messages) {
            // messageClass == 0 → "class-0" (flash) SMS, ekranda gösterilir ama kaydedilmez
            // Type-0 farklı: PID (Protocol Identifier) 0x40 → "short message type 0"
            // PDU'dan okumak için raw bytes gerekir
            val pdu = msg.userData
            if (pdu != null && isType0Pdu(msg)) {
                _events.trySend(
                    ThreatEvent(
                        type = EventType.SILENT_SMS_CANDIDATE,
                        rawValue = "Type-0 PDU detected from ${msg.originatingAddress}",
                        score = 55,  // Daha yüksek güven; doğrudan PDU kanıtı
                        context = "originatingAddress=${msg.originatingAddress}",
                    )
                )
            }
        }
    }

    private fun isType0Pdu(msg: android.telephony.SmsMessage): Boolean {
        // TP-PID (Protocol Identifier) byte = 0x40 → "short message type 0"
        return try {
            val pduData = msg.encodedMessage
            if (pduData != null && pduData.size > 3) {
                val tpPid = pduData[2].toInt() and 0xFF
                tpPid == 0x40
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private fun readInboxCount(): Int {
        return try {
            context.contentResolver.query(
                android.net.Uri.parse("content://sms/inbox"),
                arrayOf("_id"),
                null, null, null
            )?.use { it.count } ?: 0
        } catch (e: SecurityException) {
            // READ_SMS izni yoksa 0 döner; detector partial çalışır
            -1
        }
    }
}
