package com.shadowcell.storage

import android.content.Context
import android.os.Build
import com.shadowcell.scoring.ThreatEvent
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class EvidenceExporter(private val context: Context) {

    fun generateExport(events: List<ThreatEvent>, password: String): File {
        // 1. Convert to JSON
        val jsonArray = JSONArray()
        events.forEach { event ->
            val jsonObj = JSONObject().apply {
                put("id", event.id)
                put("timestamp", event.timestamp)
                put("type", event.type.name)
                put("rawValue", event.rawValue)
                put("score", event.score)
                put("context", event.context)
                put("confirmed", event.confirmed)
                
                // HMAC signature (simplified for prototype)
                val rawData = "${event.id}:${event.timestamp}:${event.type.name}"
                put("hmac", generateHmac(rawData, "shadowcell_secret_key"))
            }
            jsonArray.put(jsonObj)
        }

        val exportObj = JSONObject().apply {
            put("deviceFingerprint", Build.FINGERPRINT)
            put("deviceModel", Build.MODEL)
            put("timestamp", System.currentTimeMillis())
            put("events", jsonArray)
        }

        val jsonString = exportObj.toString(4)

        // 2. Encrypt and ZIP
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val zipFile = File(exportDir, "shadowcell_evidence_${System.currentTimeMillis()}.zip")
        
        // Very basic AES implementation for prototype (Warning: in production use salt/IV + PBKDF2)
        val key = password.padEnd(32, '0').substring(0, 32).toByteArray(Charsets.UTF_8)
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val encryptedData = cipher.doFinal(jsonString.toByteArray(Charsets.UTF_8))

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val entry = ZipEntry("evidence.json.enc")
            zos.putNextEntry(entry)
            zos.write(encryptedData)
            zos.closeEntry()
        }

        return zipFile
    }

    private fun generateHmac(data: String, key: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest((data + key).toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}