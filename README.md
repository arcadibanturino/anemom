# Anemom

Aplikasi Android untuk skrining anemia pada ibu hamil (AI Scan Mata + sinyal PPG dari band/smartwatch, pengingat TTD, dan pemantauan tren Hb).
Proyek PJBL mata kuliah Aplikasi Mobile.

## Status checkpoint (Minggu 12, 11 November 2026)

| Item | Status |
|---|---|
| Splash Screen | Selesai |
| On-boarding (3 halaman) | Selesai |
| Login & Register (sesi lokal DataStore) | Selesai |
| Navigasi (bottom nav: Beranda, TTD, Skrining, Riwayat, Profil) | Selesai |
| Dashboard / Beranda | Selesai (membaca data Hb & TTD lokal) |
| Profil saya + Keluar akun | Selesai (tanggal lahir, fasilitas kesehatan, dan tombol Edit masih TODO) |
| Tentang Anemom | Selesai |
| Pengaturan pengingat, Privasi & disclaimer | TODO (halaman placeholder) |
| Skrining (hub), TTD, Riwayat/Grafik Hb, Input Hb darah | Selesai (disimpan lokal di DataStore) |
| Smartwatch & PPG | Tampilan selesai, data SIMULASI (BLE + model PPG TODO) |
| AI Scan Mata | Tampilan selesai, kamera + model YOLOv8 TFLite TODO |
| Pengingat TTD sungguhan (notifikasi/FCM) | TODO (saat ini hanya menyimpan status tombol) |
| Nutrisi, Scan Makanan | TODO (halaman placeholder) |
| Backend (Retrofit) | TODO (kerangka `ApiService` sudah ada, belum dipanggil) |

Cari penanda `TODO(` di kode untuk melihat bagian yang belum dikerjakan.

## Tech stack

Kotlin, Jetpack Compose + Material 3, MVVM + Repository, Retrofit + OkHttp, Coroutines + Flow, DataStore, JUnit.

## Struktur

```
app/src/main/java/com/kelompok/anemom/
├── data/        local (DataStore), remote (Retrofit), repository, model
├── ui/          theme, components, navigation, splash, onboarding, auth, home, ttd, skrining, riwayat, profile, about, placeholder
├── util/        validator, perhitungan kehamilan, format tanggal
├── AnemomApp.kt
└── MainActivity.kt
```
