# ShadowCell — Architecture & System Design (Military Grade)

## Overview

ShadowCell is a rootless, hybrid cyber-defense architecture designed to detect advanced telecommunication anomalies (such as SS7/Diameter attacks and IMSI Catchers/Stingrays) without requiring device modification.

The system relies on a sophisticated pipeline that performs Sensor Fusion, Context Correlation, and evaluations using both offline Machine Learning (TensorFlow Lite) and a Crowd-Sourced Global Threat Database (Go Backend).

```text
┌─────────────────────────────────────────────────────────────────┐
│                        ShadowCell App                           │
├────────────────────────┬────────────────────────────────────────┤
│   SENSOR LAYER         │  Working Principle                     │
│   (NO Root Required)   │                                        │
│                        │  NetworkDowngradeDetector              │
│                        │    → TelephonyManager.getNetworkType() │
│                        │    → 4G → 3G → 2G transition tracking  │
│                        │                                        │
│                        │  SilentSmsDetector                     │
│                        │    → SMS_RECEIVED / DELIVER tracking   │
│                        │    → PDU Type-0 (Flash) analysis       │
│                        │                                        │
│                        │  CellTowerMonitor                      │
│                        │    → getAllCellInfo() 10s polling      │
│                        │    → LAC/CID handoff + signal drops    │
├────────────────────────┼────────────────────────────────────────┤
│   FUSION & FILTERING   │  ContextCorrelationFilter              │
│                        │    → Accelerometer (Is it moving?)     │
│                        │    → Time of day (Night maintenance?)  │
│                        │    → "False Positive" rejection        │
├────────────────────────┼────────────────────────────────────────┤
│   DECISION ENGINES     │  1. Temporal Correlator (AnomalyScorer)│
│                        │     → 60s / 300s sliding window        │
│                        │     → Rule-based combo score (+25)     │
│                        │                                        │
│                        │  2. ML Scorer (TensorFlow Lite)        │
│                        │     → shadowcell_anomaly_model.tflite  │
│                        │     → Learned model probability        │
│                        │     → Multiplier if risk > 70%         │
├────────────────────────┼────────────────────────────────────────┤
│   STORAGE & EVIDENCE   │  Room DB (SQLite + SQLCipher encrypted)│
│                        │  EvidenceExporter                      │
│                        │    → JSON format compilation           │
│                        │    → SHA-256 HMAC signature (Integrity)│
│                        │    → AES Encrypted ZIP export          │
├────────────────────────┼────────────────────────────────────────┤
│   CROWD-SOURCED API    │  ShadowCellApiClient (OkHttp)          │
│                        │    → Asynchronous /report request      │
│                        │    → Golang Backend (Secure API)       │
│                        │    → SHA-256 + Pepper Cell ID Anonymity│
├────────────────────────┼────────────────────────────────────────┤
│   UI & EXTENSIONS      │  Jetpack Compose Dashboard             │
│                        │  Android Home Screen Widget (Live)     │
│                        │  Wear OS Companion (Smartwatch)        │
└────────────────────────┴────────────────────────────────────────┘
```

## Pipeline Logic: `MonitoringService`

All analysis takes place within the `MonitoringService.kt` (Foreground Service), which acts as the heart of the application:

1. **Ingestion:** 3 distinct detectors emit `Flow<ThreatEvent>`.
2. **Fusion:** All sensors are merged into a single channel via `merge()`.
3. **Context Evaluation:** `ContextCorrelationFilter` reduces the risk if the user is driving (accelerometer) or if network maintenance is expected at 3:00 AM.
4. **ML Evaluation:** The TFLite model is invoked; if unusual patterns are detected, the risk score is elevated.
5. **Scoring:** If consecutive attacks (e.g., a downgrade followed by a type-0 SMS) are detected, the score multiplies exponentially.
6. **Persistence:** Events are saved to the SQLCipher-protected Room database.
7. **Broadcast:** If the risk score is high (Score >= 40), the anomaly is instantly reported to the Go Backend API.

## Detailed Directory Structure

```text
ShadowCell/
├── android/
│   ├── app/src/main/kotlin/com/shadowcell/
│   │   ├── api/                   # Go Backend OkHttp client
│   │   ├── detectors/             # Sensors (Tower, SMS, Downgrade)
│   │   ├── profiler/              # 48-Hour Baseline profiling
│   │   ├── scoring/               # TFLite ML, Context Filter, Scorer
│   │   ├── service/               # Core Monitoring Service, Worker
│   │   ├── storage/               # Room DB, SQLCipher, AES ZIP Export
│   │   ├── ui/                    # Jetpack Compose UI
│   │   └── widget/                # Android Widget Provider
│   └── wear/                      # Wear OS module for smartwatches
├── ios/
│   └── ShadowCell/                # CoreLocation, BGTasks, SwiftUI
├── backend/                       # Golang API Server
│   ├── cmd/server/main.go         # Go Entrypoint
│   └── internal/
│       ├── api/                   # REST Handlers (/report, /check)
│       ├── crypto/                # SHA-256 + Pepper (Cell Anonymization)
│       └── db/                    # SQLite Global Threat Database
└── README.md
```

## Security & Cryptography Measures

1. **Evidence Export:** Logs on the device are encrypted with AES-256. To prove file integrity, the device's unique identifier (Fingerprint) and an `SHA-256 HMAC` signature for each row are sealed into the ZIP file.
2. **Crowd-Sourcing Privacy:** Location (Cell ID) data sent to the Go backend is irreversibly anonymized using the `Hash(MCC:MNC:CellID:Pepper)` formula via SHA-256 (preventing rainbow table attacks) before leaving the device.
3. **Database Security:** The Room database (`EventDatabase`) is directly encrypted with AES using Android's SQLCipher module.
4. **Fail-Safe Mechanism:** If the TFLite model fails to load or the device lacks an accelerometer, the services will not crash (wrapped in try-catch) and will continue to operate using the default heuristic scoring engine.

## Limitations & Future Improvements

1. **No Baseband (RIL) Access:** Because the app runs without root privileges (relying on standard APIs), it cannot read raw SS7 packets; it only monitors the side-effects on the network.
2. **Type-0 SMS Sensitivity:** SMS blocking permissions vary by Android version. On some devices (Android 12+), reading the "SMS_RECEIVED" broadcast might be restricted.
3. **Physical Testing Requirement:** IMSI Catcher detection algorithms must be simulated in the field (e.g., using LimeSDR and YateBTS) or inside a Faraday cage. Scoring should be normalized based on laboratory tests.