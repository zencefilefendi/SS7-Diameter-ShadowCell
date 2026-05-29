package com.shadowcell.api

import android.content.Context
import android.util.Log
import com.shadowcell.scoring.ThreatEvent
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ShadowCellApiClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Using a 10.0.2.2 localhost alias for Android Emulator testing by default
    private val BASE_URL = "http://10.0.2.2:8443/api/v1"

    fun reportAnomaly(event: ThreatEvent, mcc: String = "000", mnc: String = "00", cellId: String = "unknown") {
        try {
            val json = JSONObject().apply {
                put("mcc", mcc)
                put("mnc", mnc)
                put("cellId", cellId)
                put("eventType", event.type.name)
                put("riskScore", event.score)
                put("timestamp", event.timestamp)
                put("countryCode", "TR") // Example hardcoded, can be extracted from locale or TelephonyManager
            }

            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/report")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("ShadowCellAPI", "Failed to report anomaly: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        Log.i("ShadowCellAPI", "Anomaly successfully reported to crowd-sourced backend.")
                    } else {
                        Log.e("ShadowCellAPI", "Backend returned error: ${response.code}")
                    }
                    response.close()
                }
            })
        } catch (e: Exception) {
            Log.e("ShadowCellAPI", "Error building report request: ${e.message}")
        }
    }
}