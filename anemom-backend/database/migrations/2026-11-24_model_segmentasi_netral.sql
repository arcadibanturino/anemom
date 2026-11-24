-- Migrasi: model segmentasi mata dibuat netral (U-Net sekarang, YOLOv8 untuk perbandingan nanti).
-- Jalankan SEKALI pada database yang sudah berjalan:  sudo mysql < 2026-11-24_model_segmentasi_netral.sql
-- Khusus MariaDB 10.5+ (memakai IF NOT EXISTS). Data pengguna tidak berubah.
-- Database baru dari schema.sql + seed.sql terbaru TIDAK perlu migrasi ini.
--
-- Isi:
-- 1. ai_models.architecture  : arsitektur model (unet, yolov8n-seg, random_forest, ...), agar mudah dibandingkan.
-- 2. ai_models.runtime        : tambah nilai 'unet_server' (nilai lama tetap ada, tidak ada yang rusak).
-- 3. screenings.seg_model_id  : model SEGMENTASI yang memotong konjungtiva untuk skrining ini.
--                                screenings.model_id tetap = model KLASIFIKASI (anemia / non_anemia).
--    Dengan dua kolom ini, hasil U-Net dan YOLO tersimpan di tabel yang sama dan bisa dibandingkan
--    terhadap hasil lab (matched_hb_result_id).
-- 4. Baris ai_models: model YOLO lama diberi task yang benar (eye_segmentation) dan dinonaktifkan;
--    U-Net dan dua kandidat klasifikasi (RF/LR) ditambahkan.
USE anemom;

ALTER TABLE ai_models
  MODIFY runtime ENUM('tflite','yolov8_server','unet_server','sklearn_server') NOT NULL,
  ADD COLUMN IF NOT EXISTS architecture VARCHAR(30) NULL AFTER task;

ALTER TABLE screenings
  ADD COLUMN IF NOT EXISTS seg_model_id INT UNSIGNED NULL AFTER model_id,
  ADD KEY IF NOT EXISTS fk_scr_seg_model (seg_model_id),
  ADD CONSTRAINT fk_scr_seg_model FOREIGN KEY IF NOT EXISTS (seg_model_id)
      REFERENCES ai_models(id) ON DELETE SET NULL;

-- Baris YOLO lama: sebenarnya model segmentasi. Disimpan untuk eksperimen, tidak aktif.
UPDATE ai_models SET task = 'eye_segmentation', architecture = 'yolov8n-seg', is_active = 0
 WHERE name IN ('conjunctiva-yolov8-server', 'conjunctiva-yolov8-tflite');

-- Model yang dipakai sekarang. Satu baris aktif per (task, runtime).
INSERT INTO ai_models (name, task, architecture, version, runtime, file_path, metrics, is_active) VALUES
  ('conjunctiva-unet-tflite', 'eye_segmentation', 'unet', '2026.11.18', 'tflite',
   'android:assets/models/segmentation/unet/anemom_unet_fp32.tflite',
   JSON_OBJECT('input', '256x256x3', 'output', 'mask 256x256x1 sigmoid', 'loss', 'combined_loss', 'metric', 'dice_coef',
               'dataset', 'Eyes-defy-anemia (palpebral masks)'), 1),
  ('conjunctiva-unet-server', 'eye_segmentation', 'unet', '2026.11.18', 'unet_server',
   'ml-service/models/segmentation/unet/anemom_unet_fp32.tflite',
   JSON_OBJECT('input', '256x256x3', 'output', 'mask 256x256x1 sigmoid', 'note', 'berkas TFLite yang sama dengan di HP'), 1),
  ('conjunctiva-color-rf', 'eye_anemia_cls', 'random_forest', 'g2-rf27', 'sklearn_server',
   'ml-service/models/classifier/g2_two_candidates_RESEARCH_ONLY.joblib#RF_legacy27',
   JSON_OBJECT('features', 'G1-color-features-exact-v1 (27)', 'threshold_exploratory', 0.2848,
               'label_rule', 'adult dataset: F Hb<12; M Hb<13', 'research_only', TRUE), 1),
  ('conjunctiva-color-lr', 'eye_anemia_cls', 'logistic_regression', 'g2-lr35', 'sklearn_server',
   'ml-service/models/classifier/g2_two_candidates_RESEARCH_ONLY.joblib#LR_robust_extended',
   JSON_OBJECT('features', 'rob_* (35)', 'threshold_exploratory', 0.3676,
               'label_rule', 'adult dataset: F Hb<12; M Hb<13', 'research_only', TRUE), 0)
ON DUPLICATE KEY UPDATE task = VALUES(task), architecture = VALUES(architecture), runtime = VALUES(runtime),
  file_path = VALUES(file_path), metrics = VALUES(metrics);
