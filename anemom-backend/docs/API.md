# Anemom: Kontrak API (v1)

Base URL: `https://<domain-vps>/api/v1`. Format JSON (UTF-8). Waktu dalam ISO 8601 UTC (`2026-11-17T04:41:00Z`); saat mengirim boleh memakai offset (`+08:00`). Tanggal kalender pengguna berupa `YYYY-MM-DD`.

**Autentikasi:** header `Authorization: Bearer <token>` (token dari register/login, berlaku 30 hari).

**Respons sukses:** `{"data": ..., "meta": {...}}`. **Galat:** `{"error": {"code": "...", "message": "...", "details": {...}}}`.

| Status | `error.code` |
|---|---|
| 400 | `invalid_json` |
| 401 | `unauthenticated`, `invalid_token`, `invalid_credentials` |
| 403 | `account_suspended`, `wrong_password`, `consent_required` |
| 404 | `not_found` |
| 405 | `method_not_allowed` |
| 409 | `email_taken` |
| 413 | `file_too_large` |
| 415 | `unsupported_media` |
| 422 | `validation_failed` (rinci per kolom di `details`) |
| 500 | `server_error` |

**Offline-sync.** Data buatan pengguna menerima `client_uuid` (UUID dari aplikasi). Mengirim ulang UUID yang sama mengembalikan data yang sudah ada (200), bukan duplikat. Endpoint list mendukung `?updated_since=<ISO8601>`; hasilnya juga memuat data yang dihapus (`deleted_at` terisi).

## Publik

| Metode | Path | Keterangan |
|---|---|---|
| GET | `/health` | Cek server + database |
| GET | `/config` | Kategori risiko Hb, model AI aktif, apakah YOLO server tersedia, batas unggah |
| POST | `/auth/register` | `name` (nama atau nama panggilan, min. 2 huruf), `email, password, hpht, birth_date` (+ `health_facility, device_name`). `birth_date` wajib, usia 10-60 tahun. Tidak ada nomor telepon. Mengembalikan `token, expires_at, user` |
| POST | `/auth/login` | `email, password, [device_name]` |

## Akun

| Metode | Path | Keterangan |
|---|---|---|
| POST | `/auth/logout` | Cabut token ini |
| GET / PUT | `/me` | Profil + `pregnancy {week, trimester, due_date}` (dihitung server). PUT (semua field opsional, hanya yang dikirim yang berubah): `name, email, hpht, birth_date, health_facility, height_cm, pre_pregnancy_weight_kg, gravida`. `email` yang sudah dipakai akun lain: 409 `email_taken`; jika email berubah, sesi di perangkat lain dicabut. `birth_date` usia 10-60 tahun. `health_facility: ""` menghapus fasilitas |
| PUT | `/me/password` | `current_password, new_password` (sesi lain dicabut) |
| DELETE | `/me` | `password`. Menghapus akun, semua data, dan berkas |
| GET / POST | `/consents` | POST: `type` (`privacy_policy`/`health_data`/`image_upload`/`research_use`), `version`, `accepted` |
| GET | `/dashboard` | Data Beranda: kehamilan, Hb terakhir, tren 5 titik, ringkasan TTD, 3 skrining terakhir |

## Hb laboratorium

| Metode | Path | Keterangan |
|---|---|---|
| GET | `/hb-results` | `?from=&to=&limit=&updated_since=` |
| POST | `/hb-results` | `hb_value` (3-20, koma/titik), `test_date`, `[facility, source, notes, client_uuid]`. `risk` dihitung server |
| GET / DELETE | `/hb-results/{id}` | |

## TTD

| Metode | Path | Keterangan |
|---|---|---|
| GET | `/ttd?month=YYYY-MM` | Hari yang sudah minum + `taken_today, taken_last_7_days, percent_last_7_days` |
| POST | `/ttd/take` | `[date, time]`, default hari ini. Dua kali di hari sama: 200 `created:false` |
| DELETE | `/ttd/{YYYY-MM-DD}` | Batalkan tanda |
| GET / PUT | `/ttd/settings` | `reminder_on, reminder_time` |

## Skrining

| Metode | Path | Keterangan |
|---|---|---|
| POST | `/images` | multipart: `file`, `purpose` (`eye`/`food`), `[captured_at, client_uuid, use_for_training]` |
| GET | `/images/{id}/file` | Unduh gambar milik sendiri |
| DELETE | `/images/{id}` | |
| POST | `/screenings/eye` | multipart: `image`, `[captured_at, client_uuid, use_for_training]`, `[on_device_result]` (JSON: `label, confidence, [est_hb, model_version]`). **Dengan** `on_device_result`: hanya disimpan (201). **Tanpa**: YOLO di VPS dijalankan; 201 jika selesai, 202 jika masih antre (poll `GET /screenings/{id}`, lihat `status`) |
| POST | `/screenings/ppg` | JSON atau multipart: `started_at, duration_s, sample_rate_hz, [device_uid, sample_count, hr_bpm, spo2_pct, signal_quality, quality_score, features{}, result{est_hb, confidence, label, model_version, inference_source}, client_uuid]` + berkas `raw` opsional |
| GET | `/screenings` | `?method=ai_eye` atau `ppg_band`, `&limit=&updated_since=` |
| GET / DELETE | `/screenings/{id}` | `status`: `done`/`queued`/`processing`/`failed` |
| PUT | `/screenings/{id}/link-hb` | `hb_result_id`: kaitkan ke hasil lab |

## Band

| Metode | Path | Keterangan |
|---|---|---|
| GET | `/devices` | |
| POST | `/devices` | `device_uid, [name, firmware_version]` (daftar ulang = 200) |
| PUT | `/devices/{id}/status` | `[battery_pct, firmware_version]` |
| DELETE | `/devices/{id}` | |

## Bahan makanan, rekomendasi, dan konten

| Metode | Path | Keterangan |
|---|---|---|
| GET | `/recommendations` | Bahan makanan yang direkomendasikan (hanya `is_recommended = 1`, urut `recommend_order`). **Kosong sampai kelompok mengisi data.** Bahan ber-`pregnancy_warning` tetap dikirim; aplikasi yang memutuskan menampilkannya sebagai peringatan atau menyembunyikannya di Beranda |
| GET | `/ingredients`, `/ingredients/{id}` | Katalog bahan. `?q=&category=&limit=` |
| GET | `/ingredients/{id}/image` | Gambar bahan (**publik**, tanpa token, agar bisa dimuat pemuat gambar) |
| GET / POST | `/food-logs` | Catatan makan (opsional, belum dipakai UI). GET: `?date=YYYY-MM-DD&updated_since=`. POST: `eaten_at, meal_type, (ingredient_id atau custom_name), [portions, image_id, source, ai_label, ai_confidence, client_uuid]` |
| GET | `/food-logs/summary?date=` | Total kalori/zat besi + `has_iron_booster`, `has_iron_inhibitor` |
| DELETE | `/food-logs/{id}` | |
| GET | `/articles`, `/articles/{slug}` | Hanya artikel yang sudah terbit |

Bentuk satu bahan: `id, name, category, description, iron_type (heme/non_heme), serving_desc, serving_grams, calories_kcal, protein_g, iron_mg, vitamin_c_mg, folate_mcg, is_iron_booster, is_iron_inhibitor, is_recommended, recommend_order, pregnancy_warning, reference, image_url`. Semua nilai gizi per **satu porsi** (`serving_desc`).

## Notifikasi

| Metode | Path | Keterangan |
|---|---|---|
| POST / DELETE | `/fcm-tokens` | `token, [device_name]` |
| GET | `/notifications` | |
| PUT | `/notifications/{id}/read` | |

## Kontrak layanan YOLO di VPS (proses Python terpisah, hanya localhost)

`POST http://127.0.0.1:8001/predict/eye`, multipart field `image`. Respons 200:
```json
{"label": "anemia", "confidence": 0.91, "est_hb": 9.8, "model_version": "0.0.1"}
```
`label` = `anemia` / `non_anemia`; `est_hb` boleh `null` jika model hanya klasifikasi. Layanan ini belum dibuat; API sudah memanggilnya dan menyimpan job di `inference_jobs`.
