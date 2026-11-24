# ml-service: layanan AI mata Anemom

Proses Python terpisah di VPS, hanya mendengarkan `127.0.0.1:8001`. Dipanggil API PHP (`INFERENCE_URL`), bukan oleh aplikasi.

```
app.py                       endpoint /health, /segment/eye, /predict/eye        (BERSAMA)
anemom_ml/config.py          pengaturan lewat environment                         (BERSAMA)
anemom_ml/segmenters/base.py kontrak segmenter (mask 256 x 256)                   (BERSAMA)
anemom_ml/features.py        fitur warna G1 (tahap 2)                             (BERSAMA, belum diisi)
anemom_ml/classifier.py      RF / LR dari bundle joblib                           (BERSAMA)
anemom_ml/segmenters/unet.py U-Net (.tflite yang sama dengan di HP)               (tim U-Net)
anemom_ml/segmenters/yolo.py YOLOv8-seg                                           (tim YOLO, belum dibuat)
tools/compare_segmenters.py  Dice/IoU/waktu tiap segmenter pada data uji yang sama
models/                      berkas model (dataset TIDAK disimpan di repo)
```

## Menjalankan

```bash
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt            # eksperimen YOLO: pip install -r requirements-yolo.txt
uvicorn app:app --host 127.0.0.1 --port 8001
curl -s localhost:8001/health
curl -s -F image=@foto_mata.jpg localhost:8001/predict/eye
```

| Environment | Bawaan | Keterangan |
|---|---|---|
| `ENABLED_SEGMENTERS` | `unet` | Dimuat saat start, mis. `unet,yolo` |
| `DEFAULT_SEGMENTER` | `unet` | Dipakai bila permintaan tidak mengirim `segmenter` |
| `CLASSIFIER_CANDIDATE` | `RF_legacy27` | Atau `LR_robust_extended` |
| `UNET_MODEL`, `YOLO_MODEL`, `CLASSIFIER_BUNDLE` | folder `models/` | Jalur berkas model |
| `MIN_MASK_AREA`, `MAX_MASK_AREA` | `0.03`, `0.60` | Harus sama dengan `MaskQuality.kt` |

## Status saat ini

- `/segment/eye` dan `/predict/eye` berjalan di VPS (diuji 25 Nov 2026).
- Fitur warna: `anemom_ml/g1_feature_extraction.py` disalin APA ADANYA dari tim AI (`Anemom_G1_Untuk_Backend.zip`);
  `features.py` hanya pembungkus (resize `cv2.INTER_AREA`, komponen mask terbesar, minimal 80 piksel).
- Klasifikasi: `RF_legacy27` (sesuai `models/classifier/g1_backend_config.json`), ambang skrining 0,2848.
- **scikit-learn harus 1.6.1** (versi saat bundle joblib dibuat). `/health` menampilkan `sklearn_warning` bila beda.
- Model `research_only`, label dewasa (F Hb<12, M Hb<13), dilatih dari foto beralat bantu. Hasil adalah skor
  skrining, bukan diagnosis. Foto berbeda dari mata yang sama bisa memberi skor berbeda (10% vs 37% pada uji).
- Setelah menyalin berkas ke VPS: `sudo chmod -R a+rX /opt/anemom/ml-service` (layanan berjalan sebagai user
  `anemom-ml`; berkas yang tidak bisa dibaca membuat layanan gagal start).
- Unit systemd: `deploy/anemom-ml.service`.

## Membandingkan U-Net dan YOLO

```bash
ENABLED_SEGMENTERS=unet,yolo python tools/compare_segmenters.py /data/eyes-defy-anemia/India \
    --ids data_uji_ids.txt --out hasil_perbandingan.csv
```
`--ids` berisi nama folder data **uji** (satu per baris). Pakai daftar yang sama untuk kedua model, dan jangan memasukkan folder yang dipakai melatih model mana pun.
