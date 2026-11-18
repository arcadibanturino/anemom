USE anemom;

-- Kategori risiko (sama dengan logika aplikasi saat ini; asumsi, konfirmasi bidan).
INSERT INTO hb_risk_levels (code, label, min_hb) VALUES
  ('normal',   'Normal',        11.0),
  ('moderate', 'Risiko sedang', 10.0),
  ('high',     'Risiko tinggi',  0.0)
ON DUPLICATE KEY UPDATE label = VALUES(label), min_hb = VALUES(min_hb);

-- Model AI (baris penampung; ganti metrics/file_path setelah model dilatih).
INSERT INTO ai_models (name, task, version, runtime, file_path, is_active) VALUES
  ('conjunctiva-yolov8-server', 'eye_anemia_cls', '0.0.1', 'yolov8_server', NULL, 0),
  ('conjunctiva-yolov8-tflite', 'eye_anemia_cls', '0.0.1', 'tflite',        NULL, 0),
  ('ppg-hb-regressor',          'ppg_hb',         '0.0.1', 'sklearn_server', NULL, 0)
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
