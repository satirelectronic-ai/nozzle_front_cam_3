# Donanım Seçimi — Kamera + Lens (Türkiye'de temin edilebilir)

## Kamera (öncelik sırası)

1) **Arducam 2MP USB (Sony IMX291, 1/2.8", M12 yuvalı, USB-C, UVC)** — DENGELİ SEÇİM
   - Renkli, düşük ışıkta iyi, M12 lens **değiştirilebilir** (ölçüm lensi takabilirsin), Android OTG uyumlu.
   - MVP için ideal fiyat/performans. Türkiye: SAMM Market (Arducam distribütörü); Robotistan/Direnç'te de Arducam bulunur.

2) **Arducam 1MP OV9281 Global Shutter (B0332, M12, düşük distorsiyon, mono)** — ÖLÇÜM İÇİN DAHA İYİ
   - Global shutter = keskin kenar, alt-piksel merkezleme daha kararlı. Monokrom (kırmızı noktayı parlaklıkla ayırırsın).
   - Titreşim/elde kullanımda avantajlı. Biraz daha pahalı.

3) **ELP M12 yuvalı 2MP UVC modül** — EN UCUZ
   - Board-level, YUY2 (düşük sıkıştırma) seçeneği, M12 değiştirilebilir lens.
   - DİKKAT: ELP'nin **5MP OV5640 USB** modelini ALMA — Android OTG desteklemiyor. 2MP (OV2710) tercih et.

### Kaçınılacaklar
- **Otomatik odaklı** webcam'ler (ölçüm tekrarlanamaz) → sabit/manuel odak şart.
- **Ağır MJPEG-only** ucuz endoskoplar → sıkıştırma artefaktı alt-piksel doğruluğu bozar.
- Lens yuvası **sabit** (sökülemez) modüller → ölçüm lensi takamazsın.

## Lens

- **Başlangıç: Arducam M12 Lens Seti (LK004/LK005, 10 lens, 20°–180°)**
  - Çalışma mesafene göre doğru dar açılı lensi **deneyerek** bulmanı sağlar. Türkiye: SAMM Market / Botland (AB) / Amazon.
- Doğru odak bulununca **tek M12 lens** al: nozul (~1–4 mm) kareyi doldursun diye **dar FOV / uzun odak**
  (ör. 16 mm veya 25 mm M12, ya da bir **makro M12** lens). Telesentrik en iyisi ama pahalı; MVP'de gerekmez.

> Not: Kesin lens odağı, kameranın nozula mesafesine bağlı. Set alıp 2–3 lens deneyerek nozulun kareyi
> ~%60–80 doldurduğu odağı seç; sonra o tek lensi stoklarsın.
