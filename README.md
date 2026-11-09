# Anemom

Aplikasi Android untuk skrining anemia pada ibu hamil (AI Scan Mata + sinyal PPG dari band/smartwatch, pengingat TTD, dan pemantauan tren Hb).
Proyek PJBL mata kuliah Aplikasi Mobile.

## Status checkpoint (Minggu 12, 11 November 2026)

| Item | Status |
|---|---|
| Splash Screen | Selesai |
| On-boarding (3 halaman) | Selesai |
| Login & Register (sesi lokal DataStore) | Selesai |
| Navigasi (bottom nav: Beranda, Skrining, TTD, Riwayat, Profil) | Selesai |
| Dashboard / Beranda | Selesai (data Hb, TTD, tren masih contoh) |
| Profil saya + Keluar akun | Selesai (tanggal lahir, fasilitas kesehatan, dan tombol Edit masih TODO) |
| Tentang Anemom | Selesai |
| Pengaturan pengingat, Privasi & disclaimer | TODO (halaman placeholder) |
| AI Scan Mata, Smartwatch/PPG, TTD, Riwayat, Nutrisi, Scan Makanan | TODO (halaman placeholder) |
| Backend (Retrofit) | TODO (kerangka `ApiService` sudah ada, belum dipanggil) |

Cari penanda `TODO(` di kode untuk melihat bagian yang belum dikerjakan.

## Tech stack

Kotlin, Jetpack Compose + Material 3, MVVM + Repository, Retrofit + OkHttp, Coroutines + Flow, DataStore, JUnit.

## Struktur

```
app/src/main/java/com/kelompok/anemom/
├── data/        local (DataStore), remote (Retrofit), repository, model
├── ui/          theme, components, navigation, splash, onboarding, auth, home, profile, about, placeholder
├── util/        validator, perhitungan kehamilan, format tanggal
├── AnemomApp.kt
└── MainActivity.kt
```
