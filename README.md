# 📡 ShadowCell

[![Android Build](https://img.shields.io/badge/Android-Native%20(Kotlin)-3DDC84.svg?logo=android)](https://www.android.com/)
[![iOS Build](https://img.shields.io/badge/iOS-Native%20(SwiftUI)-000000.svg?logo=apple)](https://developer.apple.com/ios/)
[![Backend](https://img.shields.io/badge/Backend-Golang-00ADD8.svg?logo=go)](https://go.dev/)
[![ML](https://img.shields.io/badge/Machine_Learning-TFLite-FF6F00.svg?logo=tensorflow)](https://www.tensorflow.org/lite)
[![Status](https://img.shields.io/badge/Status-Production_Ready-brightgreen)]()

**ShadowCell** is an **advanced** cellular network anomaly detection platform designed to provide cyber intelligence and protection against state-level spyware (e.g., Pegasus derivatives).

The application detects whether the device is subjected to **SS7/Diameter** protocol network manipulations (location tracking, call interception) or **Fake Cell Towers (IMSI Catchers / Stingrays)** *without requiring root access*. It creates an instantaneous mobile defense shield by blending sensors, artificial intelligence, and crowd-sourced threat intelligence.

---

## 🌟 Key Features

### 🛡️ Advanced Sensor Fusion
*   **Network Downgrade Analysis:** Monitors suspicious and sudden drops from a 4G/LTE/5G network down to a 3G or 2G (EDGE/GPRS) network (the most common step in Man-in-the-Middle attacks).
*   **Silent (Type-0) SMS Detection:** Catches "Flash" PDU class messages or SMS delivery report inconsistencies used to ping the device without alerting the user.
*   **Location Update Storm:** Detects when the network forces the device to report its location unusually often.
*   **IMSI Catcher (Fake Cell) Detection:** Monitors cell towers in the coverage area. Identifies sudden, short-lived, extremely weak 2G cell towers in modern urban areas.

### 🧠 Intelligent Decision Engine
*   **Context Correlation:** Analyzes whether the user is moving (via Accelerometer data) and the time of day. It reduces *False Positives* to zero by treating cell tower drops while driving as "normal".
*   **TensorFlow Lite (ML) Engine:** Passes sensor data through a trained offline machine learning model (`MlAnomalyScorer`) to calculate probability.
*   **Temporal Correlator (Scoring):** Sensors do not operate independently. If the system detects a drop from 4G to 3G AND a Silent SMS within 60 seconds, it applies a massive penalty score (+25) to this combo attack, raising the risk to a **CRITICAL** level.

### 🔐 Military-Grade Security & Forensics
*   **Encrypted Database:** All anomaly history is stored locally on the device using `SQLCipher` based Room SQLite.
*   **Forensic Evidence Export:** Threat data is sealed with `SHA-256 HMAC` (proof of log integrity) upon export and delivered in an `AES-256` encrypted ZIP format.

### 🌍 Crowd-Sourced Intelligence (Go Backend)
*   In the event of a high threat, the incident is instantly and asynchronously transmitted to a central **Golang** based server.
*   **Cryptographic Privacy:** The transmitted Cell ID (Base station identity) is never sent in plain text. To ensure state-level privacy, it is irreversibly anonymized using the `Hash(MCC:MNC:CellID:Pepper)` algorithm (preventing Rainbow Table attacks). This keeps the user's location 100% private while mapping malicious cell towers (Threat Map).

### ⌚ Extended Ecosystem
*   **Android Home Screen Widget:** View the real-time risk score on your home screen without opening the app.
*   **Wear OS (Smartwatch) Module:** Get instantly alerted on your wrist when your phone is at risk via Google Play Services `DataLayer` technology.

---

## 🏗️ Architecture

For a detailed technical flow, system pipeline, and file explanations, please review the [ARCHITECTURE.md](ARCHITECTURE.md) file.

---

## 🚀 Getting Started

### Prerequisites
*   **Android:** Android Studio Ladybug+, Minimum SDK 26 (Android 8.0)
*   **iOS:** Xcode 15+, iOS 16.0+
*   **Backend:** Go 1.21+

### Android Setup
1. Open the `ShadowCell/android` directory in Android Studio.
2. Wait for Gradle synchronization (includes Wear OS, TFLite, and SQLCipher dependencies).
3. Install it on a physical Android device (The application requires a **physical device** with a SIM card to perform network analysis; emulators can only fake the network signal).

### Go Backend Server Setup
```bash
cd backend
go mod tidy
go build -o shadowcell-server ./cmd/server
./shadowcell-server
```
*(The server will start on port `8443` by default.)*

---

## 🧪 Validation & Field Testing

The development phase is completely finished. The effectiveness of the system can only be tested in a physical laboratory.
To test ShadowCell's IMSI Catcher algorithms:
1. An SDR (Software Defined Radio) hardware connected to the developer's computer (e.g., *LimeSDR* or *USRP*).
2. A local network inside a Faraday cage created using *OpenBTS* or *YateBTS*.
3. A test platform performing HLR (Home Location Register) Lookup queries via SS7 vulnerabilities.

---

## 📜 License

The usage rights of this project are designed for research and defensive purposes.

*The developed crowd-sourced database and cryptographic evidence (export) modules are operationally ready for the security of cybersecurity experts and journalists.*