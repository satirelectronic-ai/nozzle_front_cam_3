# Nozul Merkezleme — Android MVP (Lite - telefon kamerası + OpenCV)

Telefona **USB-C ile bağlanan bir UVC kamerayı** okuyup, görüntüde nozul çemberi ile
kırmızı kılavuz ışın noktasını tespit eder; aradaki ofseti ölçer ve operatöre
**hangi yöne** ayar yapması gerektiğini (Sağa/Sola/Yukarı/Aşağı) + **kaç mm** kaydığını
gösterir. Merkeze gelince gösterge **yeşile** döner.

## İki kamera kaynağı (yeni)
Uygulama alt bardaki **"Kaynak: Telefon / USB"** düğmesiyle iki kaynak arasında geçer:
- **Telefon** (varsayılan): telefonun arka kamerasını CameraX ile kullanır — **hiç donanım gerektirmez**, hemen dene. Kareler otomatik dik konuma döndürülür.
- **USB**: USB-C ile bağlı UVC kamerayı (libausbc) kullanır. Düğmeye basınca kamerayı tak ve izni onayla.

İki kaynak da **aynı görüntü işleyiciyi ve kalibrasyonu** paylaşır; birinde kalibre edince diğerinde de geçerli olur.

> İlk deneme için **Telefon** modu en pratiği: kamera almadan, sadece telefonu (ideal olarak
> 3D baskı bir yuvayla) nozulun üstüne tutup algoritmanın çalışıp çalışmadığını görürsün.

> **Dürüst not:** Bu, çalışan bir **başlangıç iskeleti (MVP)**dir; test edilmiş bir sürüm değil.
> Derlenecek şekilde yazıldı ama cihazda denenmedi. Üçüncü parti UVC kütüphanesi (libausbc)
> ve OpenCV sürüm farkları nedeniyle küçük düzeltmeler gerekebilir. Aşağıdaki "Bilinen
> tuzaklar" kısmını mutlaka oku.

## Nasıl derlenir (APK üretimi)
1. **Android Studio** (Koala/2024.1+ önerilir) ile bu klasörü aç.
2. İlk açılışta Gradle bağımlılıkları iner:
   - `org.opencv:opencv:4.11.0` (Maven Central — native .so gömülü)
   - `com.github.jiangdg.AndroidUSBCamera:libausbc:3.3.3` (JitPack)
3. `Build > Build App Bundle(s) / APK(s) > Build APK(s)`.
4. Çıkan `app/build/outputs/apk/debug/app-debug.apk` dosyasını telefona kurup çalıştır.
   (İmzalı release için `Build > Generate Signed Bundle / APK`.)

## Gerekli donanım
- USB-C **OTG destekli** Android telefon (Android 7.0+; UVC harici kamera desteği cihaza göre değişir).
- **UVC USB kamera** (aşağıdaki öneri listesine bak) + **M12 ölçüm lensi**.
- Kısa bir **USB-C OTG kablosu** (kamera USB-A ise C↔A adaptörü).

## Kullanım
1. Kamerayı tak; Android izin sorunca uygulamayı seç.
2. Kamerayı nozulun altına, kırmızı kılavuz ışın açıkken konumla.
3. **Işık eşiği** kaydırıcısını, sadece kırmızı nokta beyaz kalacak şekilde ayarla.
4. **Kalibrasyon:** bilinen çaplı bir nozulu (ör. 1.5 mm) net göster, çapı kutuya yaz, **Kalibre**'ye bas.
   Uygulama piksel→mm ölçeğini hesaplayıp kaydeder (mm ofset artık doğru olur).
5. **X↔ / Y↕** düğmeleri: ekrandaki yön oku ile kafadaki vidanın gerçek yönü ters çıkarsa ekseni çevir.

## Bilinen tuzaklar (önemli)
- **Android UVC uyumu cihaza göre değişir.** İlk iş: hedef telefonda kamerayı gördüğünü doğrula.
  Görmezse libausbc sürümünü değiştir ya da `saki4510t/UVCCamera` alternatifini dene.
- **Önizleme formatı NV21 olmalı.** `UvcCameraFragment.onPreviewData` sadece NV21 işler; kütüphane
  RGBA veriyorsa `NozzleProcessor.process` içindeki renk dönüşümünü RGBA'ya göre uyarlaman gerekir.
- **libausbc API'si** sürümler arası değişebilir (`getCameraView`, `getCameraViewContainer`,
  `getCameraRequest`, `onCameraState`). Derlemede imza uyuşmazlığı olursa o sürümün örneğine bakıp
  eşitle.
- **Otomatik odak/pozlama/beyaz-denge kapalı olmalı.** Ölçümün tekrarlanabilirliği için sabit
  odaklı lens kullan; kütüphane destekliyorsa AF/AE'yi kilitle.
- **Doğruluk optik-mekanikte saklı.** 0.01 mm sınıfı ölçüm; sabit çalışma mesafesi, düşük
  distorsiyonlu lens ve rijit nozul yuvası gerektirir. Bu uygulama yazılım tarafını çözer; asıl
  hassasiyeti mekanik kafa belirler.

## Dosya haritası
- `NozzleProcessor.kt` — OpenCV görüntü işleme (nozul dairesi + ışın centroid + ofset/yön). **Çekirdek.**
- `UvcCameraFragment.kt` — libausbc kamera; her kareyi işlemciye verir.
- `OverlayView.kt` — çember, artı imleç, ışın noktası ve yön okunun çizimi.
- `MainActivity.kt` — arayüz, kalibrasyon, eşik ve eksen kontrolleri.

## Sonraki adımlar (öneri)
- Global shutter mono kamera ile kenarları sabitle; elips fiti (nozul) daha kararlı olur.
- Zamansal filtre (birkaç kare ortalaması) ekleyerek gösterge titremesini azalt.
- Sabit bir model telefon/tablet + 3D baskı optik-mekanik yuva ile "ürün" katmanına geç.
