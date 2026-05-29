# SS7 / Diameter Saldırı Vektörleri

## SS7 Temel Zafiyetler

### 1. SendRoutingInfo (SRI-SM) — Konum Tespiti
- Saldırgan, hedefin IMSI ve servising MSC/SGSN adresini sorgular
- HLR (Home Location Register) doğrulama yapmadan cevaplar
- Tespit edilebilir iz: **Location Update burst** (ağ, cihazı yeniden konumlandırır)

### 2. ProvideSubscriberInfo (PSI) — Gerçek Zamanlı Konum
- Servicing MSC'ye doğrudan konum sorgusu
- Cihazın cell ID + LAC'ı döner
- Tespit edilebilir iz: **Beklenmedik location query → paging**

### 3. UpdateLocation Attack — HLR Manipülasyonu  
- Saldırgan, kendi SS7 node'unu hedefin HLR'ına sahte MSC olarak kaydeder
- Tüm çağrı ve SMS'ler saldırgana yönlendirilir
- Tespit edilebilir iz: **Ani LU değişikliği + servis kesintisi**

### 4. ForwardSM Interception — SMS Dinleme
- Sahte MSC üzerinden SMS yönlendirme
- SRI-SM → UpdateLocation → ForwardSM zinciri
- Tespit edilebilir iz: **SMS gecikmesi + duplicate delivery receipt**

### 5. MAP CancelLocation — Servis Reddi
- Hedefin kaydını HLR'dan siler
- Cihaz "No Service" görür, ardından yeniden kayıt gerekir
- Tespit edilebilir iz: **ServiceState döngüsü: OUT_OF_SERVICE → IN_SERVICE**

---

## Diameter (4G) Saldırıları

### 1. Downgrade via Diameter RAR
- Diameter Request-Answer (RAR) mesajı, cihazı 4G'den düşürür
- 4G'den 3G'ye geçince klasik SS7 saldırı seti devreye girer
- **Bu, 2026'da hâlâ aktif olarak kullanılan birincil vektör**
- Tespit edilebilir iz: **Ani 4G→3G geçişi + kısa süre sonra geri dönüş**

### 2. S6a/S6d — IMSI Enumeration
- HSS (Home Subscriber Server) sorguları ile IMSI bulma
- Tespit edilebilir iz: Cihaz tarafında doğrudan görülmez; ağ tarafında

### 3. Gx Interface — Policy Manipulation
- Cihazın QoS/politika parametrelerini değiştirir
- Veri throttling veya belirli portları bloke etmek için kullanılır
- Tespit edilebilir iz: Beklenmedik bandwidth düşüşü

---

## IMSI Catcher (STINGRAY / DIRTBOX)

### Nasıl Çalışır
1. Sahte baz istasyonu yayın yapar (genellikle 2G/3G)
2. Yakındaki cihazlar sinyal gücüne göre en iyi tower'ı seçer
3. Cihaz sahte tower'a bağlanır → IMSI açık olarak iletilir
4. Saldırgan MitM pozisyonuna geçer

### Tespit İzleri (Cihaz Tarafında)
- Bilinmeyen/geçici cell ID kısa süre görünür, sonra kaybolur
- Handoff sonrası sinyal beklenenden daha zayıf
- Modern bölgede 2G-only tower
- Veri hızı ani düşüş (sahte tower tam stack çalıştırmaz)
- Battery drain artışı (sürekli yeniden kayıt)

---

## Circles Satıcı Zinciri

- **Circles/Rayzone**: SS7 erişimini ticari ürün olarak satan vendor
- **Malware + Circles**: Malware + ağ saldırısı kombinasyonu
- 2016 Citizens Lab raporu: 25 ülkede Circles altyapısı tespit edildi
- 2021 güncelleme: Hâlâ aktif, roaming ortaklarına enjekte edilmiş

## Referanslar

- P1Sec: Location Tracking Attacks (2024)
- SRLabs: SS7 Attacks on Real Networks (BlackHat 2014)
- GSMA FS.11: SS7 Baseline Security Controls
- Karsten Nohl: SS7 Hack 60 Minutes Demo (2014)
- Citizens Lab: State-Sponsored Spyware (2021)
