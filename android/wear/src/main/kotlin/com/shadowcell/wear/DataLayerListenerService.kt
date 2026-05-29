package com.shadowcell.wear

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class DataLayerListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                if (path == "/shadowcell/risk") {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val score = dataMap.getInt("score")
                    val level = dataMap.getString("level") ?: "SAFE"
                    
                    // Broadcast to WearMainActivity if it's active
                    val intent = Intent("RISK_UPDATE")
                    intent.putExtra("score", score)
                    intent.putExtra("level", level)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    
                    // TODO: Optional - Trigger a notification on the watch if critical
                }
            }
        }
    }
}