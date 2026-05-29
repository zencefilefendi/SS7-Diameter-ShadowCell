# Kaynaklar

## Akademik / Teknik

- **Nohl, K. (2014)** — "Mobile Self-Defense", CCC. SS7 saldırılarının kamuoyuna açıklandığı ilk major sunum.
- **Engel, T. (2014)** — "SS7: Locate. Track. Manipulate.", 31C3.
- **SRLabs (2016)** — "SS7map: Mapping vulnerability of the international SS7 network"
- **3GPP TS 29.002** — MAP (Mobile Application Part) protokol spesifikasyonu
- **3GPP TS 29.272** — Diameter S6a/S6d interface spesifikasyonu

## Güvenlik Araştırmaları

- **P1Sec (2024)**: "Location Tracking Attacks: How Adversaries Exploit Mobile Networks"
  https://www.p1sec.com/blog/location-tracking-attacks-how-adversaries-exploit-mobile-networks-to-follow-you

- **Cellcrypt**: "Mobile Threats: SS7 and Diameter"
  https://www.cellcrypt.com/mobile-threats-ss7-and-diameter

- **Citizens Lab (2021)**: "Running in Circles: Uncovering the Clients of Cyberespionage Firm Circles"

- **Positive Technologies (2018)**: "SS7 Attack Discovery" — signaling firewall vendor raporu

## Mevcut Araçlar

- **SnoopSnitch** (Android, root, Qualcomm)
  - Repo: github.com/SecUpwN/Android-IMSI-Catcher-Detector
  - Sorun: 2018'de terk edildi, Android 10+ çalışmıyor

- **AIMSICD** (Android IMSI Catcher Detector)
  - Repo: github.com/CellularPrivacy/Android-IMSI-Catcher-Detector
  - Durum: Arşivlendi (2020)

- **GsmMap** (passive network mapping)
  - Operatör bazlı, cihaz sensörü değil

## Operatör Kaynakları

- **GSMA FS.11**: SS7 Baseline Security Controls (operator tarafı)
- **GSMA IR.82**: Security Aspects of Diameter (2019)
- **FCC (2017)**: SS7 Security Report — ABD'de ilk resmi federal rapor

## Yasal Çerçeve

- Türkiye: 5651 sayılı Kanun (internet içerik değil, ağ güvenliği ayrı)
- AB: GDPR + ePrivacy Directive — SS7 dinleme AB'de yasadışı
- ABD: CALEA (lawful intercept için SS7 kullanımı operatöre verilmiş)

## Yararlı Topluluklar

- Chaos Computer Club (CCC): security.ccc.de
- Osmocom: açık kaynak GSM/3G stack (test için)
- srsRAN: açık kaynak LTE/5G (lab test için)
