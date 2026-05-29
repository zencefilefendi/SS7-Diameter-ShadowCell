package com.shadowcell.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WearApp()
            }
        }
    }
}

@Composable
fun WearApp() {
    // These states would normally be updated by DataLayerListenerService receiving data from the phone
    val riskLevel = remember { mutableStateOf("SAFE") }
    val riskScore = remember { mutableStateOf(0) }

    val bgColor = when (riskLevel.value) {
        "SAFE" -> Color(0xFF003300)
        "MEDIUM" -> Color(0xFF555500)
        "HIGH" -> Color(0xFF552200)
        "CRITICAL" -> Color(0xFF550000)
        else -> Color.Black
    }

    val textColor = when (riskLevel.value) {
        "SAFE" -> Color.Green
        "MEDIUM" -> Color.Yellow
        "HIGH" -> Color(0xFFFFA500)
        "CRITICAL" -> Color.Red
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ShadowCell",
                fontSize = 12.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = riskScore.value.toString(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = riskLevel.value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}