# Tespit Heuristikleri ve False Positive Analizi

## Heuristik 1: Network Downgrade Attack

### Tespit Şartları
```
TRIGGER:
  networkGeneration(yeni) < networkGeneration(eski)
  
HIGH CONFIDENCE (+25 bonus) IF:
  same window: silentSmsCandidate OR locationUpdateBurst
  
SCORE: 40 (4G→3G) | 55 (4G→2G) | 30 (3G→2G)
```

### False Positive Kaynakları
| Senaryo | Frekans | Çözüm |
|---------|---------|-------|
| Tünel/bodrum giriş-çıkışı | Çok yüksek | Bağlam: hızlı upgrade (< 30s) + accelerometer hareketi |
| Zayıf kapsama bölgesi | Yüksek | Baseline: bu bölgede downgrade normalse es geç |
| Uçak modu açıp kapama | Orta | ServiceState: EMERGENCY_ONLY sonrası geçiş es geç |
| Carrier network maintenance | Düşük | Geniş alan etkileniyorsa (crowd-sourced data) |

### Optimal Eşik
- Standart: 4G→3G → 40 puan
- Downgrade + hızlı geri dönüş (< 120s): +25
- 3 kez art arda aynı pattern: score cap 85

---

## Heuristik 2: Silent SMS (Type-0)

### Tespit Şartları
```
TRIGGER (yöntem 1): 
  PDU içinde TP-PID byte = 0x40
  → Doğrudan Type-0 kanıtı; güvenilir

TRIGGER (yöntem 2 - dolaylı):
  delivery_receipt_count > inbox_delta
  → Zayıf sinyal; birden fazla false positive kaynağı var
  
SCORE: 55 (PDU kanıtı) | 35 (dolaylı)
```

### False Positive Kaynakları
| Senaryo | Çözüm |
|---------|-------|
| Binary SMS (WAP push, OTA update) | PDU class'ı kontrol et; class-0 ≠ type-0 |
| Silinmiş SMS sonrası sayım delta | 1 saatlik window kullan |
| Dual-SIM uyumsuzluğu | Her SIM için ayrı sayaç |

### Not
Android 10+ READ_SMS iznini kısıtladı. Yöntem 2 (dolaylı) artık daha az güvenilir. PDU kontrolü (Yöntem 1) için RECEIVE_SMS izni hâlâ çalışıyor.

---

## Heuristik 3: IMSI Catcher (Sahte Baz İstasyonu)

### Tespit Şartları
```
PATTERN A — Fleeting Tower:
  tower görünme süresi < 90 saniye
  AND snapshot_count < 5
  AND tower daha önce hiç görülmedi
  → SCORE: 50

PATTERN B — Zayıf Kayıt:
  cell.isRegistered == true
  AND signalDbm < -110
  → SCORE: 35

PATTERN C — Modern Bölgede 2G:
  generation == 2G
  AND networkOperator Türkiye/AB/ABD'de
  → SCORE: 45
```

### False Positive Kaynakları
| Senaryo | Çözüm |
|---------|-------|
| Gerçek zayıf kapsama noktası | Süre: tower > 90s görünüyorsa IMSI catcher değil |
| Seyahat (hızlı tower geçişi) | Accelerometer + GPS hız korelasyonu |
| Rural alanda 2G tower | Coğrafi 2G shutdown haritası ile filtrele |

---

## Heuristik 4: Location Update Storm

### Tespit Şartları
```
onServiceStateChanged event rate > 4/dakika

SCORE: 30 (5-8 event/dk) | 45 (>8 event/dk)
```

### False Positive
- Yüksek mobility: araçtayken tower değişimi hızlı olur
- Çözüm: hız eşiği (GPS/accelerometer < 5 km/h ise filtrele)

---

## Temporal Korelasyon Matrisi

```
Olay Kombinasyonu                          → Risk Bonusu
─────────────────────────────────────────────────────────
Downgrade + SilentSMS (60s window)         → +25
Downgrade + LocationBurst (60s window)     → +20
FleetingTower + SignalDrop (60s window)    → +20
Downgrade + SilentSMS + Tower (60s)        → +40 total
```

## Öneri: Kalibrasyonlu Tespit

1. **İlk 48 saat**: Sadece kayıt yap, alarm verme
2. **Baseline hesapla**:
   - Günlük ortalama downgrade sayısı
   - Ortalama tower değişim hızı
   - Normal sinyal gücü aralığı
3. **Kişisel eşikler**: Genel eşik yerine baseline'dan N standart sapma kullan
4. **Haftalık yeniden kalibrasyon**: Kullanıcı yeni bölgeye taşındıysa

## Karşılaştırma: SnoopSnitch

| Özellik | SnoopSnitch | ShadowCell |
|---------|-------------|------------|
| Root gereksinim | Evet | Hayır |
| Platform | Android/Qualcomm | Android + iOS |
| Tespit yöntemi | Baseband log | API yan etkileri |
| Son güncelleme | 2018 | 2026 |
| False positive rate | Düşük (daha derin erişim) | Orta (API seviyesi) |
| Güven skoru | Yüksek | Probabilistik |
