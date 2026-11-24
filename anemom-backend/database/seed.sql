USE anemom;

-- Kategori risiko (sama dengan logika aplikasi saat ini; asumsi, konfirmasi bidan).
INSERT INTO hb_risk_levels (code, label, min_hb) VALUES
  ('normal',   'Normal',        11.0),
  ('moderate', 'Risiko sedang', 10.0),
  ('high',     'Risiko tinggi',  0.0)
ON DUPLICATE KEY UPDATE label = VALUES(label), min_hb = VALUES(min_hb);

-- Model AI. Segmentasi mata dibuat netral: U-Net aktif sekarang, YOLOv8 disiapkan untuk perbandingan.
-- Satu baris aktif per (task, runtime). screenings.seg_model_id = segmentasi, screenings.model_id = klasifikasi.
INSERT INTO ai_models (name, task, architecture, version, runtime, file_path, is_active) VALUES
  ('conjunctiva-unet-tflite',   'eye_segmentation', 'unet',                '2026.11.18', 'tflite',         'android:assets/models/segmentation/unet/anemom_unet_fp32.tflite', 1),
  ('conjunctiva-unet-server',   'eye_segmentation', 'unet',                '2026.11.18', 'unet_server',    'ml-service/models/segmentation/unet/anemom_unet_fp32.tflite', 1),
  ('conjunctiva-yolov8-tflite', 'eye_segmentation', 'yolov8n-seg',         '0.0.1',      'tflite',         NULL, 0),
  ('conjunctiva-yolov8-server', 'eye_segmentation', 'yolov8n-seg',         '0.0.1',      'yolov8_server',  NULL, 0),
  ('conjunctiva-color-rf',      'eye_anemia_cls',   'random_forest',       'g2-rf27',    'sklearn_server', 'ml-service/models/classifier/g2_two_candidates_RESEARCH_ONLY.joblib#RF_legacy27', 1),
  ('conjunctiva-color-lr',      'eye_anemia_cls',   'logistic_regression', 'g2-lr35',    'sklearn_server', 'ml-service/models/classifier/g2_two_candidates_RESEARCH_ONLY.joblib#LR_robust_extended', 0),
  ('ppg-hb-regressor',          'ppg_hb',           NULL,                  '0.0.1',      'sklearn_server', NULL, 0)
ON DUPLICATE KEY UPDATE name = name;

-- Bahan makanan: SENGAJA kosong. Belum ada rujukan gizi dari kelompok/ahli gizi.
-- Cara menambah bahan: lihat docs/DATABASE.md (bagian "Mengisi bahan makanan rekomendasi").

-- Artikel draf (is_published = 0). Isi medis harus ditinjau tenaga kesehatan sebelum diterbitkan.
INSERT INTO articles (slug, category, title, summary, body, is_published) VALUES
 ('minum-ttd-dengan-benar', 'ttd', 'Minum TTD dengan benar',
  'Draf, perlu ditinjau bidan.',
  'DRAF: ikuti dosis dari tenaga kesehatan. Hindari minum TTD berdekatan dengan teh, kopi, atau susu.', 0),
 ('ai-bukan-pengganti-tes-darah', 'panduan_aman', 'AI dan PPG bukan pengganti tes darah',
  'Draf, perlu ditinjau bidan.',
  'DRAF: hasil AI Scan Mata dan PPG hanya skrining awal. Konfirmasi dengan pemeriksaan Hb darah.', 0)
ON DUPLICATE KEY UPDATE slug = slug;
