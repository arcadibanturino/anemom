# Pemisahan model segmentasi: U-Net (utama) dan YOLOv8 (perbandingan)

**Prinsip:** satu repo, satu database, satu API. Yang berbeda hanya **implementasi model segmentasi**, dan
setiap tim punya folder sendiri. Tim U-Net dan tim YOLO tidak pernah mengubah berkas yang sama, jadi tidak saling timpa.

```
Foto ──► [Segmentasi: U-Net | YOLO] ──► mask 256x256 ──► MaskQuality ──► Fitur warna G1 ──► RF/LR ──► anemia / non_anemia
          ▲ berbeda per tim                           └──────────────── BERSAMA ───────────────────┘
```

## 1. Peta folder

| Bagian | BERSAMA (ubah hanya bila kedua tim sepakat) | Tim U-Net | Tim YOLO |
|---|---|---|---|
| Android | `app/src/main/java/com/kelompok/anemom/ml/segmentation/ConjunctivaSegmenter.kt`, `MaskQuality.kt`, layar, repository, API | `app/src/unet/` (kode + `assets/models/segmentation/unet/`) | `app/src/yolo/` (kode + `assets/models/segmentation/yolo/`) |
| Server AI | `ml-service/app.py`, `anemom_ml/segmenters/base.py`, `features.py`, `classifier.py` | `anemom_ml/segmenters/unet.py`, `models/segmentation/unet/` | `anemom_ml/segmenters/yolo.py`, `models/segmentation/yolo/` |
| Database | semua tabel (satu skema) | baris `ai_models` architecture `unet` | baris `ai_models` architecture `yolov8n-seg` |

## 2. Android: dua flavor dari satu kode

Flavor `unet`/`yolo`, CameraX, LiteRT, dan `noCompress "tflite"` sudah ada di `app/build.gradle.kts`.

- Pilih varian di **Build Variants**: `unetDebug` (sehari-hari) atau `yoloDebug` (eksperimen).
- Kode di `app/src/main` memanggil `SegmenterProvider.create(context)`. Berkas `SegmenterProvider.kt` ada dua (satu per flavor) dengan isi fungsi yang sama, sehingga flavor menentukan model tanpa `if`.
- Saat varian `unetDebug` aktif, Android Studio menampilkan folder `src/yolo` sebagai tidak aktif. Itu normal.
- Varian YOLO akan galat bila AI Scan dipakai sebelum `YoloSegmenter.kt` diisi tim YOLO.

## 3. Database: satu skema, hasil tetap terpisah

Jalankan sekali di VPS: `sudo mysql < anemom-backend/database/migrations/2026-11-24_model_segmentasi_netral.sql`

- `ai_models.architecture` baru: `unet`, `yolov8n-seg`, `random_forest`, `logistic_regression`.
- `screenings.seg_model_id` baru: model **segmentasi** yang dipakai. `screenings.model_id` tetap model **klasifikasi**.
- Baris YOLO lama diperbaiki menjadi `task = eye_segmentation` dan dinonaktifkan; U-Net dan RF/LR ditambahkan.

Perbandingan terhadap hasil lab (setelah skrining dikaitkan ke Hb lab lewat `PUT /screenings/{id}/link-hb`):

```sql
SELECT seg.architecture AS segmentasi, cls.name AS klasifikasi, COUNT(*) AS n,
       ROUND(AVG((s.label = 'anemia') = (h.hb_value < 11)), 3) AS kecocokan_dengan_lab
FROM screenings s
JOIN hb_results h       ON h.id = s.matched_hb_result_id
LEFT JOIN ai_models seg ON seg.id = s.seg_model_id
LEFT JOIN ai_models cls ON cls.id = s.model_id
WHERE s.method = 'ai_eye' AND s.deleted_at IS NULL
GROUP BY seg.architecture, cls.name;
```
Catatan: model dilatih dengan label dewasa (F Hb<12, M Hb<13), sedangkan batas anemia ibu hamil di query ini Hb<11.

## 4. Server AI (`ml-service/`)

Lihat `ml-service/README.md`. Satu layanan memuat U-Net dan (nanti) YOLO sekaligus. Aplikasi mengirim
`segmenter=unet|yolo`, API PHP meneruskannya, dan layanan memakai model yang diminta. Perbandingan Dice/IoU
di data uji yang sama: `ml-service/tools/compare_segmenters.py`.

## 5. Alur Git

```bash
git checkout main && git pull
git checkout -b fitur/scan-mata-unet       # tim U-Net + integrasi (kamera, unggah)
git checkout -b eksperimen/yolov8          # tim YOLO, dari main
```

Aturan:
1. Tim U-Net hanya mengubah folder U-Net dan berkas bersama yang disepakati; tim YOLO hanya folder YOLO.
2. Berkas BERSAMA diubah lewat Pull Request yang disetujui kedua tim (otomatis lewat `CODEOWNERS`).
3. Branch YOLO rutin mengambil perubahan terbaru: `git merge main`. Karena foldernya terpisah, hampir tidak ada konflik.
4. Branch YOLO boleh di-merge ke `main` kapan pun kodenya siap. Itu tidak mengganti U-Net, hanya mengisi flavor `yolo`.
5. Model yang menang dipilih lewat data (Dice/IoU, kecepatan, kecocokan dengan lab), lalu cukup ubah flavor bawaan dan `is_active` di `ai_models`.

## 6. Yang masih perlu dari tim

| # | Siapa | Apa |
|---|---|---|
| 1 | Tim AI | Konfirmasi praproses U-Net: dibagi 255? resize langsung ke 256x256? (`UnetTfliteSegmenter.kt`, `segmenters/unet.py`) |
| 2 | Tim AI | ~~Kode fitur G1~~ (selesai 25 Nov, `g1_feature_extraction.py`); pastikan kelas 1 = anemia |
| 3 | Tim AI | Pilih kandidat klasifikasi (RF atau LR) dan kalibrasi batas `MaskQuality` (luas mask) |
| 4 | Backend | ~~Sambungkan `ScreeningController` ke ml-service~~ (selesai 25 Nov) |
| 5 | Server | ~~scikit-learn 1.6.1~~ (terpasang) |
| 6 | Tim YOLO | Isi `YoloSegmenter.kt` dan `segmenters/yolo.py` sesuai komentar di dalamnya |
