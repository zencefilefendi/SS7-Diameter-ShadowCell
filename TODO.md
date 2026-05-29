# ShadowCell — Tasks & Roadmap

Last updated: 2026-05-29

## COMPLETED

- [x] Project name and core concept determined (ShadowCell)
- [x] Project directory structure created
- [x] README.md created
- [x] Full architecture design completed (ARCHITECTURE.md)
- [x] Research notes compiled (ss7_attack_vectors.md, detection_heuristics.md)
- [x] Core data model implemented (ThreatEvent.kt, EventType.kt)
- [x] NetworkDowngradeDetector.kt implemented
- [x] SilentSmsDetector.kt implemented
- [x] CellTowerMonitor.kt implemented
- [x] SignalAnomalyDetector.kt implemented
- [x] AnomalyScorer.kt (temporal correlation) implemented
- [x] EventDatabase.kt + EventRepository.kt implemented
- [x] ShadowCellApp.kt (Application class) implemented
- [x] AndroidManifest.xml (permissions) configured
- [x] iOS NetworkMonitor.swift implemented
- [x] iOS CellularMonitor.swift implemented
- [x] iOS AnomalyDetector.swift implemented
- [x] iOS ThreatEvent.swift implemented
- [x] iOS DashboardView.swift implemented
- [x] Android build.gradle.kts (root + app) configured

---

## ROADMAP & FUTURE TASKS

### PHASE 1 — Android UI & Services (COMPLETE)
- [x] Complete `MainActivity.kt` + `DashboardViewModel.kt`
  - Risk level indicator (large color-coded gauge)
  - Event list (RecyclerView)
  - Last 24h timeline chart
- [x] Setup Foreground Service (`MonitoringService.kt`)
  - Keep sensors running when the app goes to the background
  - Persistent notification ("ShadowCell is monitoring")
- [x] Periodic CellInfo snapshot via `WorkManager`
- [x] Room migration test (EventDatabase v1→v2)

### PHASE 2 — Baseline Profiler (COMPLETE)
- [x] Implement `BaselineProfiler.kt`
  - Learn "normal" device behavior over the first 48 hours
  - Calculate personal thresholds for each detection module
  - Network type transition frequency baseline
  - Cell tower change rate baseline
- [x] Show "Calibration in progress" warning in UI before baseline is set
- [x] Persist Baseline to SharedPreferences + Room DB

### PHASE 3 — False Positive Mitigation (COMPLETE)
- [x] Add context correlation filter:
  - If the device is moving (accelerometer) → tower change is normal
  - If WiFi calling is active → 4G→3G downgrade is normal
  - If in power save mode → filter network changes
- [x] User feedback loop ("mark this alert as false positive")
- [x] Time-zone analysis (signal drop at 3 AM should be interpreted differently)

### PHASE 4 — iOS Completion (COMPLETE)
- [x] Generate iOS Xcode project files (.xcodeproj)
- [x] `ShadowCellApp.swift` + App delegate
- [x] Integrate BackgroundTasks framework for background fetch
- [x] CoreLocation permissions + user consent flow
- [x] Connect iOS DashboardView to real data
- [x] iOS `AnomalyDetector` scoring calibration

### PHASE 5 — Evidence Export (COMPLETE)
- [x] Implement `EvidenceExporter.kt`
  - Last N events → JSON
  - Timestamp + device fingerprint + event details
  - AES-256 encrypted ZIP
  - Lock with user password
- [x] Share export via Email / File Provider
- [x] Log integrity: SHA-256 HMAC signature for every event

### PHASE 6 — Validation & Field Testing (PENDING)
- [ ] Setup test environment:
  - Old Android phone + physical SIM
  - Find a known SS7 simulator or lab environment
  - Compare with P1Sec / SRLabs tools
- [ ] Measure false positive rate (7 days log → how many false alarms)
- [ ] Test across different carriers (minimum 3 carriers)
- [ ] Test roaming scenarios

### PHASE 7 — Enhancements (PARTIALLY COMPLETE)
- [x] Integrate TensorFlow Lite model (Offline ML for advanced scoring)
- [x] Crowd-sourced anomaly database (Anonymous event sharing via Go Backend)
- [x] Android Home Screen Widget for continuous risk monitoring
- [x] Wear OS companion app integration
- [ ] Publish to F-Droid (Open source, independent of Google Play)