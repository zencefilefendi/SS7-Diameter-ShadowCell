package com.shadowcell.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.shadowcell.profiler.BaselineProfiler
import com.shadowcell.scoring.RiskLevel
import com.shadowcell.scoring.RiskSnapshot
import com.shadowcell.scoring.ThreatEvent
import com.shadowcell.service.MonitoringService
import com.shadowcell.storage.EvidenceExporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start monitoring service
        val serviceIntent = Intent(this, MonitoringService::class.java)
        startForegroundService(serviceIntent)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val riskSnapshot by viewModel.riskSnapshot.collectAsState()
    val recentEvents by viewModel.recentEvents.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isCalibrating = BaselineProfiler(context).isCalibrating()
    
    var showExportDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Evidence") },
            text = {
                Column {
                    Text("Enter a password to encrypt the export file.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val eventsToExport = viewModel.getExportData()
                            val file = EvidenceExporter(context).generateExport(eventsToExport, exportPassword)
                            
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Evidence"))
                            showExportDialog = false
                            exportPassword = ""
                        }
                    }
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ShadowCell Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showExportDialog = true }) {
                Text("Export")
            }
        }
        
        if (isCalibrating) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Kalibrasyon devam ediyor. Normal kullanım kalıpları öğreniliyor. Uyarılar geçici olarak hatalı (false positive) olabilir.",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        RiskIndicator(riskSnapshot)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Timeline (Last 24h)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        TimelineChart(recentEvents)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Recent Events", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (recentEvents.isEmpty()) {
            Text("No threats detected.", color = Color.Gray)
        } else {
            LazyColumn {
                items(recentEvents) { event ->
                    EventItem(event) { viewModel.markFalsePositive(it) }
                }
            }
        }
    }
}

@Composable
fun RiskIndicator(snapshot: RiskSnapshot) {
    val color = when (snapshot.level) {
        RiskLevel.SAFE -> Color.Green
        RiskLevel.MEDIUM -> Color.Yellow
        RiskLevel.HIGH -> Color.hsl(30f, 1f, 0.5f) // Orange
        RiskLevel.CRITICAL -> Color.Red
    }

    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            drawCircle(
                color = color.copy(alpha = 0.2f),
                radius = size.minDimension / 2
            )
            drawCircle(
                color = color,
                radius = size.minDimension / 2 - 16f,
                style = Stroke(width = 16f)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${snapshot.totalScore}",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = snapshot.level.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun TimelineChart(events: List<ThreatEvent>) {
    // Simple mock timeline implementation
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color.DarkGray.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L
            val width = size.width
            val height = size.height
            
            // Baseline
            drawLine(
                color = Color.Gray,
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = 2f
            )

            events.forEach { event ->
                val timeDiff = now - event.timestamp
                if (timeDiff <= dayMs) {
                    val x = width - (timeDiff.toFloat() / dayMs) * width
                    val eventColor = when (event.score) {
                        in 0..30 -> Color.Green
                        in 31..65 -> Color.Yellow
                        in 66..85 -> Color.hsl(30f, 1f, 0.5f)
                        else -> Color.Red
                    }
                    val eventHeight = (event.score / 100f) * height
                    
                    drawLine(
                        color = eventColor,
                        start = Offset(x, height),
                        end = Offset(x, height - eventHeight),
                        strokeWidth = 8f
                    )
                }
            }
        }
    }
}

@Composable
fun EventItem(event: ThreatEvent, onMarkFalsePositive: (ThreatEvent) -> Unit) {
    val dateFormat = SimpleDateFormat("HH:mm:ss dd/MM/yy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(event.timestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(event.type.name, fontWeight = FontWeight.Bold)
                    Text(event.rawValue, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dateStr, fontSize = 12.sp, color = Color.Gray)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Red.copy(alpha = (event.score / 100f).coerceIn(0.2f, 1f)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${event.score}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            if (event.score > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { onMarkFalsePositive(event) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Yanlış Alarm (False Positive)", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}