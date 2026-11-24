# Anemom

Aplikasi Android untuk skrining anemia pada ibu hamil (AI Scan Mata + sinyal PPG dari band/Smartband, pengingat TTD, dan pemantauan tren Hb).
Proyek PJBL mata kuliah Aplikasi Mobile.

## Status (25 November 2026)

| Item | Status |
|---|---|
| Splash Screen, On-boarding (3 halaman), Navigasi (bottom nav: Beranda, TTD, Skrining, Riwayat, Profil) | Selesai |
| Login & Register (token sesi dari API, disimpan di DataStore) | Selesai (Register mewajibkan tanggal lahir dan HPHT) |
| Profil Kesehatan Awal (langkah 2 pendaftaran: TB, BB, riwayat anemia, alergi, LiLA) | Selesai (rekomendasi menu menyaring bahan yang membuat alergi) |
| Lupa kata sandi | Tampilan selesai, pengiriman email masih SIMULASI (endpoint belum ada) |
| Dashboard / Beranda | Selesai (data dari server: kehamilan, Hb terakhir, tren, ringkasan TTD) |
| Profil saya + Edit Profil + Keluar akun | Selesai (nama, tanggal lahir, email, HPHT, fasilitas kesehatan; usia, usia kehamilan, trimester, HPL dihitung otomatis) |
| Tentang Anemom | Selesai |
| Pengaturan pengingat, Privasi & disclaimer | TODO (halaman placeholder) |
| Skrining (hub), TTD, Riwayat/Grafik Hb, Input Hb darah | Selesai (disimpan di server lewat API) |
| Rekomendasi bahan makanan (Beranda + "Lihat Semua") | Selesai (dari server; saran harian berganti tiap hari dan menyesuaikan Hb terakhir; foto bahan dari server) |
| Detail bahan makanan | TODO (menunggu desain) |
| Smartband & PPG | Tampilan selesai, data SIMULASI (BLE + model PPG TODO) |
| AI Scan Mata | Selesai (prototipe penelitian): kamera + kotak panduan, U-Net TFLite di HP untuk cek kualitas foto, analisis di server (U-Net + fitur warna G1 + Random Forest), hasil berupa skor skrining |
| Pengingat TTD sungguhan (notifikasi/FCM) | TODO (server baru menyimpan status dan jam pengingat) |
| Backend | API PHP + database MySQL di VPS, tersambung lewat Retrofit; layanan AI mata Python (`ml-service/`) di VPS |


## Tech stack

Kotlin, Jetpack Compose + Material 3, MVVM + Repository, Retrofit + OkHttp, Coroutines + Flow, DataStore (token sesi dan salinan profil), CameraX, LiteRT (TFLite), JUnit. Backend: PHP + MySQL di VPS. Layanan AI: Python (Starlette, LiteRT, OpenCV, scikit-learn) di VPS.

## Struktur

```
app/                        Aplikasi Android
├── src/main/java/com/kelompok/anemom/
│   ├── data/               local (DataStore), remote (Retrofit, DTO), repository, model
│   ├── ml/segmentation/    kontrak model segmentasi konjungtiva (bersama U-Net & YOLO)
│   ├── ui/                 theme, components, navigation, splash, onboarding, auth, healthprofile, home,
│   │                       ttd, skrining (+ scan/: kamera AI Scan Mata), riwayat, profile, nutrisi, about
│   ├── util/               validator, perhitungan kehamilan dan usia, format tanggal
│   ├── AnemomApp.kt
│   └── MainActivity.kt
├── src/unet/               flavor U-Net: UnetTfliteSegmenter + model .tflite (dipakai sekarang)
└── src/yolo/               flavor YOLOv8: untuk eksperimen perbandingan (belum diisi)
anemom-backend/             API PHP + database (lihat anemom-backend/README.md)
ml-service/                 Layanan AI mata di VPS (lihat ml-service/README.md)
docs/PEMISAHAN-MODEL.md     Cara U-Net dan YOLO dikembangkan berdampingan tanpa saling timpa
```

Build Android: pilih varian `unetDebug` di Build Variants.

## AI Scan Mata: U-Net (dan rencana YOLOv8)

Proposal awal merencanakan YOLOv8 untuk segmentasi konjungtiva. Model yang dipakai sekarang adalah
**U-Net** (`anemom_palpebral_unet`), karena:
- dataset latih (Eyes-defy-anemia, IEEE DataPort, CC BY 4.0) menyediakan mask piksel konjungtiva palpebra,
  format yang langsung cocok untuk U-Net;
- foto diambil close-up lewat kotak panduan, kasus yang cocok untuk segmentasi semantik satu objek;
- modelnya kecil (sekitar 0,5 juta parameter, 1,9 MB) sehingga ringan di HP.

YOLOv8-seg tetap direncanakan sebagai **pembanding** (Dice/IoU, kecepatan, kecocokan dengan Hb lab) pada data
uji yang sama, lewat flavor `yolo` dan `ml-service/tools/compare_segmenters.py`. Lihat `docs/PEMISAHAN-MODEL.md`.

Alur:
```
HP:     kamera + kotak panduan -> foto dipotong -> U-Net (cek konjungtiva terlihat) -> kirim
Server: U-Net -> fitur warna G1 (tim AI) -> Random Forest -> skor + batas skrining (28,5%)
```

Temuan uji (25 Nov 2026):
- foto HP utuh gagal disegmentasi (konjungtiva terlalu kecil setelah dikecilkan ke 256x256); setelah dipotong
  sesuai kotak panduan, mask tepat di konjungtiva;
- foto buram bila HP terlalu dekat; zoom 2x + fokus otomatis memperbaikinya;
- skor dari mata yang sama bisa berbeda antar foto (10% vs 37%), jadi model belum layak untuk keputusan perorangan.

## Catatan

Hasil AI Scan Mata dan PPG hanya skrining awal, bukan diagnosis atau pengganti tes Hb darah. Model AI mata masih tahap penelitian (`research_only`): dilatih dari foto beralat bantu dengan label dewasa umum (bukan ibu hamil) dan belum divalidasi klinis; aplikasi menampilkan skor dan menyarankan tes Hb, bukan label penyakit. Saran makanan tidak menyertakan takaran; jenis dan jumlah konsumsi mengikuti anjuran dokter atau tenaga kesehatan.
