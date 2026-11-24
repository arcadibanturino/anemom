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
| GET | `/config` | Kategori risiko Hb, model AI aktif, apakah layanan AI mata di server tersedia, batas unggah |
| POST | `/auth/register` | `name` (nama atau nama panggilan, min. 2 huruf), `email, password, hpht, birth_date` (+ `health_facility, device_name`). `birth_date` wajib, usia 10-60 tahun. Tidak ada nomor telepon. Mengembalikan `token, expires_at, user` |
| POST | `/auth/login` | `email, password, [device_name]` |

## Akun

| Metode | Path | Keterangan |
|---|---|---|
| POST | `/auth/logout` | Cabut token ini |
| GET / PUT | `/me` | Profil + `pregnancy {week, trimester, due_date}` (dihitung server). PUT (semua field opsional, hanya yang dikirim yang berubah): `name, email, hpht, birth_date, health_facility, height_cm, pre_pregnancy_weight_kg, gravida`. `email` yang sudah dipakai akun lain: 409 `email_taken`; jika email berubah, sesi di perangkat lain dicabut. `birth_date` usia 10-60 tahun. `health_facility: ""` menghapus fasilitas. Respons memuat `profile_completed` (false = Profil Kesehatan Awal belum diisi) dan di `profile`: `anemia_history, allergy_status, lila_cm` |
| GET / PUT | `/me/health-profile` | **Langkah 2 pendaftaran** (akun sudah dibuat di `/auth/register`). PUT menyimpan lengkap (bukan sebagian): `height_cm` (100-220), `pre_pregnancy_weight_kg` (30-200), `anemia_history` (`ya`/`tidak`/`tidak_tahu`), `allergy_status` (`ada`/`tidak`/`tidak_tahu`), `[lila_cm]` (15-50; tidak dikirim = dihapus), `allergen_ids` (id dari `/ingredients`; wajib minimal 1 jika `allergy_status = ada`, diabaikan dan dikosongkan jika bukan). Mengembalikan user seperti GET `/me`. GET mengembalikan isi form + `allergen_ids` + `profile_completed` |
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
| POST | `/screenings/eye` | multipart: `image`, `[captured_at, client_uuid, use_for_training]`, `[segmenter]` (`unet`/`yolo`; kosong = default server), `[on_device_result]` (JSON: `label, confidence, [est_hb, model_version, segmenter, seg_model_version]`). **Dengan** `on_device_result`: hanya disimpan (201). **Tanpa**: layanan AI mata di VPS dijalankan (segmentasi + klasifikasi); 201 jika selesai, 202 jika masih antre (poll `GET /screenings/{id}`, lihat `status`). `screenings.seg_model_id` = model segmentasi, `screenings.model_id` = model klasifikasi |
| POST | `/screenings/ppg` | JSON atau multipart: `started_at, duration_s, sample_rate_hz, [device_uid, sample_count, hr_bpm, spo2_pct, signal_quality, quality_score, features{}, result{est_hb, confidence, label, model_version, inference_source}, client_uuid]` + berkas `raw` opsional |
| GET | `/screenings` | `?method=ai_eye` atau `ppg_band`, `&limit=&updated_since=` |
| GET / DELETE | `/screenings/{id}` | `status`: `done`/`queued`/`processing`/`failed`. Hasil AI mata: `label` (`anemia` = skor melewati batas skrining, BUKAN diagnosis), `score` (peluang anemia menurut model, 0-1), `score_threshold` (batas skrining), `confidence` (keyakinan terhadap `label`; jangan ditampilkan ke ibu), `model` (klasifikasi), `seg_model` (segmentasi), `research_only` |
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
| GET | `/recommendations` | Bahan makanan yang direkomendasikan (hanya `is_recommended = 1`, urut `recommend_order`). **Kosong sampai kelompok mengisi data.** Bahan ber-`pregnancy_warning` tetap dikirim; aplikasi yang memutuskan menampilkannya sebagai peringatan atau menyembunyikannya di Beranda. **Bahan yang dicentang ibu sebagai alergi (`user_allergies`) tidak ikut dikirim** |
| GET | `/ingredients`, `/ingredients/{id}` | Katalog bahan. `?q=&category=&limit=` |
| GET | `/ingredients/{id}/image` | Gambar bahan (**publik**, tanpa token, agar bisa dimuat pemuat gambar) |
| GET / POST | `/food-logs` | Catatan makan (opsional, belum dipakai UI). GET: `?date=YYYY-MM-DD&updated_since=`. POST: `eaten_at, meal_type, (ingredient_id atau custom_name), [portions, image_id, source, ai_label, ai_confidence, client_uuid]` |
| GET | `/food-logs/summary?date=` | Total kalori/zat besi + `has_iron_booster`, `has_iron_inhibitor` |
| DELETE | `/food-logs/{id}` | |
| GET | `/articles`, `/articles/{slug}` | Hanya artikel yang sudah terbit |

`group` = kelompok tampilan di aplikasi: `sayur`, `buah`, `protein` (hewani + nabati), atau `lainnya` (minuman dst.). Dihitung server dari `category` (`NutritionController::groupOf`); kolom `category` tidak berubah.

Bentuk satu bahan: `id, name, category, group, description, iron_type (heme/non_heme), serving_desc, serving_grams, calories_kcal, protein_g, iron_mg, vitamin_c_mg, folate_mcg, is_iron_booster, is_iron_inhibitor, is_recommended, recommend_order, pregnancy_warning, reference, image_url`. Semua nilai gizi per **satu porsi** (`serving_desc`).

## Notifikasi

| Metode | Path | Keterangan |
|---|---|---|
| POST / DELETE | `/fcm-tokens` | `token, [device_name]` |
| GET | `/notifications` | |
| PUT | `/notifications/{id}/read` | |

## Kontrak layanan AI mata di VPS (proses Python terpisah `ml-service/`, hanya localhost)

Model segmentasi bisa ditukar (U-Net sekarang, YOLOv8 untuk perbandingan); kontrak ini sama untuk keduanya.

`POST http://127.0.0.1:8001/predict/eye`, multipart field `image` dan `[segmenter]` (diteruskan dari permintaan aplikasi). Respons 200:
```json
{"label": "anemia", "confidence": 0.91, "est_hb": null, "model_version": "g2-rf27", "model_name": "conjunctiva-color-rf",
 "segmenter": "unet", "seg_model_name": "conjunctiva-unet-server", "seg_model_version": "2026.11.18",
 "mask_area": 0.12, "quality": "ok", "p_anemia": 0.91, "threshold": 0.2848, "research_only": true}
```
`label` = `anemia` / `non_anemia`; `est_hb` `null` karena model sekarang hanya klasifikasi. `model_name`/`model_version` (klasifikasi) dan `seg_model_name`/`seg_model_version` (segmentasi) dicocokkan ke `ai_models (name, version)` untuk mengisi `screenings.model_id` dan `screenings.seg_model_id`.

Galat (`{"error": {"code", "message", "details"}}`): 422 `poor_quality` (konjungtiva tidak terlihat; `details.mask_area`), 400 `unknown_segmenter`, 503 `segmenter_disabled` / `features_not_ready` / `classifier_not_ready`, 415 `unsupported_media`, 500 `inference_failed`. Untuk `poor_quality`, API sebaiknya mengembalikan 422 ke aplikasi agar ibu memotret ulang (bukan menandai job `failed` lalu mencoba ulang).

`POST /segment/eye` (hanya tahap 1, untuk uji/perbandingan): respons `segmenter, seg_model_name, seg_model_version, mask_area, quality`, dan `mask_png_base64` bila `?mask=1`.
