# Anemom

Aplikasi Android untuk skrining anemia pada ibu hamil (AI Scan Mata + sinyal PPG dari band/Smartband, pengingat TTD, dan pemantauan tren Hb).
Proyek PJBL mata kuliah Aplikasi Mobile.

## Status (25 November 2026)

| Item | Status |
|---|---|
| Splash Screen, On-boarding (3 halaman), Navigasi (bottom nav: Beranda, TTD, Skrining, Riwayat, Profil) | Selesai. Layar utama tab tanpa tombol kembali |
| Panduan pertama kali (FTUX) di Beranda | Selesai: 5 langkah sorotan (Dashboard, Skrining, Tren Hb, Pengingat TTD, Rekomendasi Makanan), sekali per HP |
| Login & Register (token sesi dari API, disimpan di DataStore) | Selesai (Register mewajibkan tanggal lahir dan HPHT) |
| Profil Kesehatan Awal (langkah 2 pendaftaran: TB, BB, riwayat anemia, alergi, LiLA) | Selesai (rekomendasi makanan mengecualikan bahan pemicu alergi) |
| Lupa kata sandi | Tampilan selesai, pengiriman email masih SIMULASI (endpoint belum ada) |
| Dashboard / Beranda | Selesai: kehamilan, Hb terakhir, kepatuhan TTD, rekomendasi makanan ("Lihat Detail"), Tren Hb ("Lihat detail" -> Grafik Hb) |
| Notifikasi | Selesai: ikon hati di Beranda + titik merah belum dibaca; pengingat TTD, "TTD sudah dicatat", dan pengingat periksa Hb dibuat server |
| Profil saya + Edit Profil + Keluar akun | Selesai |
| Tentang Anemom, Privasi & disclaimer | Selesai (persetujuan disimpan di server, tabel `consents`) |
| Skrining (hub) + Input Hb darah | Selesai (disimpan di server lewat API) |
| AI Scan Mata | Selesai (prototipe penelitian): kamera + kotak panduan, U-Net TFLite di HP untuk cek kualitas foto, analisis di server (U-Net + fitur warna G1 + Random Forest), hasil berupa skor skrining |
| Smartband & PPG | Tampilan selesai (Koneksi Bluetooth, Rekam PPG 10 dtk persiapan + 60 dtk rekaman, kanal merah + inframerah), data SIMULASI. TODO: firmware ESP32-C3 + MAX30102, BLE, endpoint dan model PPG |
| Pelacak TTD | Selesai: status Sudah / Belum / Belum Dikonfirmasi, kalender, kepatuhan 4 minggu, riwayat 7 hari |
| Alarm & pengingat TTD | Selesai: alarm layar penuh di atas layar kunci (tetap berbunyi walau aplikasi ditutup, dipasang ulang setelah HP menyala ulang), geser Sudah/Belum, tunda 10/15/30 menit. Bawaan menyala pukul 19.00, bisa dimatikan, **wajib aktif bila Hb lab terakhir di bawah normal**. Push FCM belum |
| Riwayat pemeriksaan | Selesai: Hb lab + AI Scan + PPG per bulan, filter, Detail riwayat + ekspor PDF |
| Grafik Hb darah | Selesai (dari kartu Tren Hb di Beranda; hanya hasil laboratorium) |
| Rekomendasi bahan makanan + Detail bahan | Selesai: tab Sayur / Buah / Protein, disesuaikan risiko Hb dan alergi; detail 27 bahan dari server (teks gizi belum diverifikasi bidan/TKPI) |
| Backend | API PHP + database MySQL di VPS, tersambung lewat Retrofit; layanan AI mata Python (`ml-service/`) di VPS |


## Tech stack

Kotlin, Jetpack Compose + Material 3, MVVM + Repository, Retrofit + OkHttp, Coroutines + Flow, DataStore (token sesi dan salinan profil), CameraX, LiteRT (TFLite), AlarmManager + notifikasi layar penuh, PdfDocument (ekspor riwayat), JUnit. Backend: PHP + MySQL di VPS. Layanan AI: Python (Starlette, LiteRT, OpenCV, scikit-learn) di VPS.

## Struktur

```
app/                        Aplikasi Android
├── src/main/java/com/kelompok/anemom/
│   ├── data/               local (DataStore), remote (Retrofit, DTO), repository, model, smartband
│   ├── ml/segmentation/    kontrak model segmentasi konjungtiva (bersama U-Net & YOLO)
│   ├── alarm/              alarm TTD (AlarmManager, notifikasi layar penuh, layar alarm)
│   ├── ui/                 theme, components, navigation, splash, onboarding, auth, healthprofile, home, ftux,
│   │                       ttd, pengingat, notifikasi, skrining (+ scan/, smartband/), riwayat, privasi,
│   │                       profile, nutrisi, about
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
