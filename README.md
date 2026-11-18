# Anemom

Aplikasi Android untuk skrining anemia pada ibu hamil (AI Scan Mata + sinyal PPG dari band/Smartband, pengingat TTD, dan pemantauan tren Hb).
Proyek PJBL mata kuliah Aplikasi Mobile.

## Status checkpoint (Minggu 13, 18 November 2026)

| Item | Status |
|---|---|
| Splash Screen, On-boarding (3 halaman), Navigasi (bottom nav: Beranda, TTD, Skrining, Riwayat, Profil) | Selesai |
| Login & Register (token sesi dari API, disimpan di DataStore) | Selesai (Register mewajibkan tanggal lahir dan HPHT) |
| Lupa kata sandi | Tampilan selesai, pengiriman email masih SIMULASI (endpoint belum ada) |
| Dashboard / Beranda | Selesai (data dari server: kehamilan, Hb terakhir, tren, ringkasan TTD) |
| Profil saya + Edit Profil + Keluar akun | Selesai (nama, tanggal lahir, email, HPHT, fasilitas kesehatan; usia, usia kehamilan, trimester, HPL dihitung otomatis) |
| Tentang Anemom | Selesai |
| Pengaturan pengingat, Privasi & disclaimer | TODO (halaman placeholder) |
| Skrining (hub), TTD, Riwayat/Grafik Hb, Input Hb darah | Selesai (disimpan di server lewat API) |
| Rekomendasi bahan makanan (Beranda + "Lihat Semua") | Selesai (dari server; saran harian berganti tiap hari dan menyesuaikan Hb terakhir; foto bahan dari server) |
| Detail bahan makanan, Scan Makanan | TODO (menunggu desain) |
| Smartband & PPG | Tampilan selesai, data SIMULASI (BLE + model PPG TODO) |
| AI Scan Mata | Tampilan selesai, kamera + model YOLOv8 TFLite TODO (repository pengiriman ke server sudah siap) |
| Pengingat TTD sungguhan (notifikasi/FCM) | TODO (server baru menyimpan status dan jam pengingat) |
| Backend | API PHP + database MySQL di VPS, tersambung lewat Retrofit; layanan YOLO di VPS (cadangan) TODO |


## Tech stack

Kotlin, Jetpack Compose + Material 3, MVVM + Repository, Retrofit + OkHttp, Coroutines + Flow, DataStore (token sesi dan salinan profil), JUnit. Backend: PHP + MySQL di VPS.

## Struktur

```
app/src/main/java/com/kelompok/anemom/
├── data/        local (DataStore), remote (Retrofit, DTO), repository, model
├── ui/          theme, components, navigation, splash, onboarding, auth, home, ttd, skrining,
│                riwayat, profile, nutrisi, about, placeholder
├── util/        validator, perhitungan kehamilan dan usia, format tanggal
├── AnemomApp.kt
└── MainActivity.kt
```

## Catatan

Hasil AI Scan Mata dan PPG hanya skrining awal, bukan diagnosis atau pengganti tes Hb darah. Saran makanan tidak menyertakan takaran; jenis dan jumlah konsumsi mengikuti anjuran dokter atau tenaga kesehatan.
