-- =====================================================================
-- Anemom: skema database MySQL 8 (kompatibel MariaDB 10.6+)
-- Jalankan:  mysql -u root -p < schema.sql   lalu   mysql -u root -p anemom < seed.sql
-- Aturan umum:
--  * Semua waktu (DATETIME/TIMESTAMP) disimpan dalam UTC. Tanggal kalender pengguna
--    (test_date, taken_date) disimpan sebagai DATE sesuai tanggal lokal pengguna.
--  * client_uuid: UUID yang dibuat aplikasi Android saat mencatat data. Pasangan
--    (user_id, client_uuid) UNIQUE, sehingga kirim ulang data (offline-sync) tidak membuat duplikat.
--  * deleted_at: hapus lunak agar sinkronisasi bisa memberi tahu perangkat lain.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS anemom
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE anemom;

-- ---------------------------------------------------------------------
-- 1. AKUN
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name          VARCHAR(100)  NOT NULL,
  email         VARCHAR(150)  NOT NULL,
  password_hash VARCHAR(255)  NOT NULL,                 -- password_hash() PHP (bcrypt/argon2)
  role          ENUM('ibu','tenaga_kesehatan','admin') NOT NULL DEFAULT 'ibu',
  status        ENUM('active','suspended') NOT NULL DEFAULT 'active',
  last_login_at DATETIME NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

-- Data khusus ibu hamil (1 baris per akun). Usia kehamilan, trimester, dan HPL
-- TIDAK disimpan: dihitung dari hpht saat dibaca, supaya tidak pernah basi.
CREATE TABLE IF NOT EXISTS mother_profiles (
  user_id        BIGINT UNSIGNED NOT NULL,
  hpht           DATE NOT NULL,                          -- Hari Pertama Haid Terakhir
  birth_date     DATE NULL,
  health_facility VARCHAR(150) NULL,                     -- puskesmas/RS langganan
  height_cm      DECIMAL(4,1) NULL,
  pre_pregnancy_weight_kg DECIMAL(4,1) NULL,
  anemia_history ENUM('ya','tidak','tidak_tahu') NULL,   -- layar Profil Kesehatan Awal
  allergy_status ENUM('ada','tidak','tidak_tahu') NULL,  -- 'ada' = daftar bahan ada di user_allergies
  lila_cm        DECIMAL(3,1) NULL,                      -- lingkar lengan atas, opsional
  gravida        TINYINT UNSIGNED NULL,                  -- jumlah kehamilan
  photo_path     VARCHAR(255) NULL,
  updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Token sesi API (token asli hanya dikirim sekali ke aplikasi; di DB disimpan hash SHA-256).
CREATE TABLE IF NOT EXISTS auth_tokens (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id      BIGINT UNSIGNED NOT NULL,
  token_hash   CHAR(64) NOT NULL,
  device_name  VARCHAR(100) NULL,
  ip_address   VARCHAR(45) NULL,
  expires_at   DATETIME NOT NULL,
  last_used_at DATETIME NULL,
  revoked_at   DATETIME NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_token_hash (token_hash),
  KEY idx_token_user (user_id),
  CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Persetujuan penggunaan data (data kesehatan itu sensitif; tercatat versi + waktunya).
CREATE TABLE IF NOT EXISTS consents (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  type        ENUM('privacy_policy','health_data','image_upload','research_use') NOT NULL,
  version     VARCHAR(20) NOT NULL,
  accepted_at DATETIME NOT NULL,
  revoked_at  DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_consent (user_id, type, version),
  CONSTRAINT fk_consent_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. PERANGKAT IoT (band ESP32-C3 + MAX30102)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS devices (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id          BIGINT UNSIGNED NOT NULL,
  device_uid       VARCHAR(64) NOT NULL,                 -- ID unik band (mis. alamat MAC BLE)
  name             VARCHAR(100) NOT NULL DEFAULT 'Anemom Band',
  model            VARCHAR(60)  NOT NULL DEFAULT 'ESP32-C3 + MAX30102',
  firmware_version VARCHAR(20)  NULL,
  battery_pct      TINYINT UNSIGNED NULL,
  paired_at        DATETIME NOT NULL,
  last_seen_at     DATETIME NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_device (user_id, device_uid),
  CONSTRAINT fk_device_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. REFERENSI KLINIS (bisa diubah tanpa merilis ulang aplikasi)
-- ---------------------------------------------------------------------
-- Kategori risiko Hb. Kategori = baris dengan min_hb terbesar yang <= nilai Hb.
-- Nilai awal mengikuti logika aplikasi sekarang (asumsi, perlu konfirmasi bidan).
CREATE TABLE IF NOT EXISTS hb_risk_levels (
  code    VARCHAR(20) NOT NULL,                 -- normal | moderate | high (boleh ditambah, mis. severe)
  label   VARCHAR(30) NOT NULL,
  min_hb  DECIMAL(3,1) NOT NULL,
  PRIMARY KEY (code)
) ENGINE=InnoDB;

-- Versi model AI. Aplikasi menanyakan model aktif lewat GET /config.
CREATE TABLE IF NOT EXISTS ai_models (
  id         INT UNSIGNED NOT NULL AUTO_INCREMENT,
  name       VARCHAR(60) NOT NULL,
  task       ENUM('eye_segmentation','eye_anemia_cls','ppg_hb','food_cls') NOT NULL,
  architecture VARCHAR(30) NULL,                          -- unet, yolov8n-seg, random_forest, ... (untuk perbandingan)
  version    VARCHAR(20) NOT NULL,
  runtime    ENUM('tflite','yolov8_server','unet_server','sklearn_server') NOT NULL,
  file_path  VARCHAR(255) NULL,
  metrics    JSON NULL,                                   -- akurasi, F1, dataset, dll.
  is_active  TINYINT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_model (name, version)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. BERKAS GAMBAR (foto mata dan foto makanan). Berkas di disk VPS, metadata di sini.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS images (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id      BIGINT UNSIGNED NOT NULL,
  client_uuid  CHAR(36) NULL,
  purpose      ENUM('eye','food') NOT NULL,
  file_path    VARCHAR(255) NOT NULL,                     -- relatif terhadap folder storage
  mime_type    VARCHAR(40)  NOT NULL,
  file_size    INT UNSIGNED NOT NULL,
  width        SMALLINT UNSIGNED NULL,
  height       SMALLINT UNSIGNED NULL,
  sha256       CHAR(64) NOT NULL,
  captured_at  DATETIME NOT NULL,
  mask_path    VARCHAR(255) NULL,                         -- hasil segmentasi konjungtiva
  use_for_training TINYINT(1) NOT NULL DEFAULT 0,         -- hanya 1 jika ada consent research_use
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at   DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_image_client (user_id, client_uuid),
  KEY idx_image_user (user_id, purpose, captured_at),
  CONSTRAINT fk_image_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. DATA SENSOR PPG DARI BAND
-- ---------------------------------------------------------------------
-- Sinyal mentah (60 detik x ~100 Hz x kanal Red+IR) terlalu besar untuk baris tabel,
-- jadi disimpan sebagai berkas (CSV gzip) dan jalurnya dicatat di raw_path.
-- Fitur hasil ekstraksi (untuk model) disimpan di kolom JSON features.
CREATE TABLE IF NOT EXISTS ppg_sessions (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id        BIGINT UNSIGNED NOT NULL,
  device_id      BIGINT UNSIGNED NULL,
  client_uuid    CHAR(36) NULL,
  started_at     DATETIME NOT NULL,
  duration_s     SMALLINT UNSIGNED NOT NULL,
  sample_rate_hz SMALLINT UNSIGNED NOT NULL,
  sample_count   INT UNSIGNED NULL,
  raw_path       VARCHAR(255) NULL,
  hr_bpm         SMALLINT UNSIGNED NULL,
  spo2_pct       DECIMAL(4,1) NULL,
  signal_quality ENUM('buruk','cukup','baik') NULL,
  quality_score  DECIMAL(4,3) NULL,                       -- 0.000 - 1.000
  features       JSON NULL,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at     DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_ppg_client (user_id, client_uuid),
  KEY idx_ppg_user (user_id, started_at),
  CONSTRAINT fk_ppg_user   FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE,
  CONSTRAINT fk_ppg_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. HASIL Hb LABORATORIUM (kebenaran dasar; juga label untuk melatih model)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hb_results (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id        BIGINT UNSIGNED NOT NULL,
  client_uuid    CHAR(36) NULL,
  test_date      DATE NOT NULL,
  hb_value       DECIMAL(4,1) NOT NULL,                   -- g/dL
  risk           VARCHAR(20) NOT NULL,                    -- kode dari hb_risk_levels, dihitung server
  facility       VARCHAR(150) NULL,
  source         VARCHAR(30) NOT NULL DEFAULT 'Laboratorium',
  pregnancy_week SMALLINT UNSIGNED NULL,                  -- usia kehamilan saat tes
  notes          VARCHAR(255) NULL,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at     DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_hb_client (user_id, client_uuid),
  KEY idx_hb_user_date (user_id, test_date),
  CONSTRAINT fk_hb_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_hb_range CHECK (hb_value BETWEEN 3 AND 20)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 7. SKRINING (AI Scan Mata / PPG Band) + antrean inferensi layanan AI di VPS (U-Net / YOLO)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inference_jobs (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id       BIGINT UNSIGNED NOT NULL,
  image_id      BIGINT UNSIGNED NULL,
  ppg_session_id BIGINT UNSIGNED NULL,
  model_id      INT UNSIGNED NULL,
  status        ENUM('queued','processing','done','failed') NOT NULL DEFAULT 'queued',
  attempts      TINYINT UNSIGNED NOT NULL DEFAULT 0,
  result        JSON NULL,
  error_message VARCHAR(255) NULL,
  latency_ms    INT UNSIGNED NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at   DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_job_status (status, created_at),
  KEY idx_job_user (user_id),
  CONSTRAINT fk_job_user  FOREIGN KEY (user_id)        REFERENCES users(id)        ON DELETE CASCADE,
  CONSTRAINT fk_job_image FOREIGN KEY (image_id)       REFERENCES images(id)       ON DELETE SET NULL,
  CONSTRAINT fk_job_ppg   FOREIGN KEY (ppg_session_id) REFERENCES ppg_sessions(id) ON DELETE SET NULL,
  CONSTRAINT fk_job_model FOREIGN KEY (model_id)       REFERENCES ai_models(id)    ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS screenings (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id          BIGINT UNSIGNED NOT NULL,
  client_uuid      CHAR(36) NULL,
  method           ENUM('ai_eye','ppg_band') NOT NULL,
  measured_at      DATETIME NOT NULL,
  pregnancy_week   SMALLINT UNSIGNED NULL,
  label            ENUM('anemia','non_anemia') NULL,      -- keluaran klasifikasi
  est_hb           DECIMAL(4,1) NULL,                     -- estimasi (BUKAN hasil lab)
  risk             VARCHAR(20) NULL,                      -- kode dari hb_risk_levels
  confidence       DECIMAL(4,3) NULL,
  image_id         BIGINT UNSIGNED NULL,
  ppg_session_id   BIGINT UNSIGNED NULL,
  model_id         INT UNSIGNED NULL,                     -- model KLASIFIKASI (anemia / non_anemia)
  seg_model_id     INT UNSIGNED NULL,                     -- model SEGMENTASI konjungtiva (U-Net / YOLO); NULL untuk PPG
  inference_source ENUM('on_device','server') NULL,
  inference_job_id BIGINT UNSIGNED NULL,
  matched_hb_result_id BIGINT UNSIGNED NULL,              -- tautan ke hasil lab untuk validasi model
  notes            VARCHAR(255) NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at       DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_screening_client (user_id, client_uuid),
  KEY idx_screening_user (user_id, measured_at),
  CONSTRAINT fk_scr_user  FOREIGN KEY (user_id)        REFERENCES users(id)         ON DELETE CASCADE,
  CONSTRAINT fk_scr_image FOREIGN KEY (image_id)       REFERENCES images(id)        ON DELETE SET NULL,
  CONSTRAINT fk_scr_ppg   FOREIGN KEY (ppg_session_id) REFERENCES ppg_sessions(id)  ON DELETE SET NULL,
  CONSTRAINT fk_scr_model FOREIGN KEY (model_id)       REFERENCES ai_models(id)     ON DELETE SET NULL,
  CONSTRAINT fk_scr_seg_model FOREIGN KEY (seg_model_id) REFERENCES ai_models(id)   ON DELETE SET NULL,
  CONSTRAINT fk_scr_job   FOREIGN KEY (inference_job_id) REFERENCES inference_jobs(id) ON DELETE SET NULL,
  CONSTRAINT fk_scr_hb    FOREIGN KEY (matched_hb_result_id) REFERENCES hb_results(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 8. TTD (Tablet Tambah Darah)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ttd_settings (
  user_id       BIGINT UNSIGNED NOT NULL,
  reminder_on   TINYINT(1) NOT NULL DEFAULT 1,
  reminder_time TIME NOT NULL DEFAULT '20:00:00',
  updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_ttdset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Satu catatan per hari (sama seperti aplikasi: menandai dua kali di hari yang sama diabaikan).
CREATE TABLE IF NOT EXISTS ttd_logs (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  taken_date  DATE NOT NULL,
  taken_time  TIME NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_ttd_day (user_id, taken_date),
  CONSTRAINT fk_ttd_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 9. BAHAN MAKANAN, REKOMENDASI, CATATAN MAKAN
-- ---------------------------------------------------------------------
-- Katalog bahan makanan. Dipakai untuk (1) layar Rekomendasi Bahan Makanan dan (2) catatan makan.
-- Bahan baru tampil di Rekomendasi HANYA jika is_recommended = 1, sehingga isinya baru terlihat
-- di aplikasi setelah kelompok punya rujukan gizi yang dapat dipertanggungjawabkan.
-- Semua nilai gizi per 1 porsi (serving_desc / serving_grams).
CREATE TABLE IF NOT EXISTS ingredients (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  name          VARCHAR(120) NOT NULL,
  category      ENUM('hewani','nabati','sayur','buah','minuman','lainnya') NOT NULL DEFAULT 'lainnya',
  description   TEXT NULL,                                -- keterangan singkat / cara konsumsi
  iron_type     ENUM('heme','non_heme') NULL,             -- besi heme (hewani) vs non-heme (nabati)
  serving_desc  VARCHAR(60) NOT NULL DEFAULT '100 g',
  serving_grams DECIMAL(6,1) NOT NULL DEFAULT 100,
  calories_kcal DECIMAL(6,1) NULL,
  protein_g     DECIMAL(5,1) NULL,
  iron_mg       DECIMAL(5,2) NULL,
  vitamin_c_mg  DECIMAL(6,1) NULL,
  folate_mcg    DECIMAL(6,1) NULL,
  is_iron_booster   TINYINT(1) NOT NULL DEFAULT 0,        -- membantu serapan zat besi (kaya vitamin C)
  is_iron_inhibitor TINYINT(1) NOT NULL DEFAULT 0,        -- menghambat serapan (teh, kopi, susu)
  is_recommended    TINYINT(1) NOT NULL DEFAULT 0,        -- 1 = tampil di Rekomendasi
  recommend_order   SMALLINT UNSIGNED NOT NULL DEFAULT 100,
  pregnancy_warning VARCHAR(255) NULL,                    -- diisi jika perlu dibatasi/dihindari saat hamil
  reference     VARCHAR(255) NULL,                        -- rujukan gizi (NULL = belum ada rujukan)
  image_path    VARCHAR(255) NULL,                        -- relatif terhadap STORAGE_PATH
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_ingredient_name (name),
  KEY idx_ingredient_cat (category),
  KEY idx_ingredient_reco (is_recommended, recommend_order)
) ENGINE=InnoDB;

-- Bahan makanan yang dicentang ibu sebagai alergi (langkah 2 pendaftaran). Dipakai untuk menyaring rekomendasi.
CREATE TABLE IF NOT EXISTS user_allergies (
  user_id       BIGINT UNSIGNED NOT NULL,
  ingredient_id INT UNSIGNED NOT NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, ingredient_id),
  KEY idx_allergy_ingredient (ingredient_id),
  CONSTRAINT fk_allergy_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_allergy_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS food_logs (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id       BIGINT UNSIGNED NOT NULL,
  client_uuid   CHAR(36) NULL,
  eaten_at      DATETIME NOT NULL,
  meal_type     ENUM('sarapan','makan_siang','makan_malam','camilan') NOT NULL,
  ingredient_id INT UNSIGNED NULL,
  custom_name   VARCHAR(120) NULL,                        -- jika tidak ada di katalog bahan
  portions      DECIMAL(5,2) NOT NULL DEFAULT 1,
  calories_kcal DECIMAL(7,1) NULL,                        -- sudah dikali porsi (snapshot)
  iron_mg       DECIMAL(6,2) NULL,
  source        ENUM('manual','scan') NOT NULL DEFAULT 'manual',
  image_id      BIGINT UNSIGNED NULL,
  ai_label      VARCHAR(80) NULL,
  ai_confidence DECIMAL(4,3) NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at    DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_foodlog_client (user_id, client_uuid),
  KEY idx_foodlog_user (user_id, eaten_at),
  CONSTRAINT fk_foodlog_user  FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
  CONSTRAINT fk_foodlog_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients(id) ON DELETE SET NULL,
  CONSTRAINT fk_foodlog_image FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 10. KONTEN + NOTIFIKASI
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS articles (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
  slug         VARCHAR(120) NOT NULL,
  category     ENUM('panduan_aman','nutrisi','ttd','kehamilan') NOT NULL,
  title        VARCHAR(160) NOT NULL,
  summary      VARCHAR(255) NULL,
  body         MEDIUMTEXT NOT NULL,
  is_published TINYINT(1) NOT NULL DEFAULT 0,
  published_at DATETIME NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_article_slug (slug),
  KEY idx_article_cat (category, is_published)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fcm_tokens (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  token       VARCHAR(255) NOT NULL,
  platform    ENUM('android') NOT NULL DEFAULT 'android',
  device_name VARCHAR(100) NULL,
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_fcm_token (token),
  KEY idx_fcm_user (user_id),
  CONSTRAINT fk_fcm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notifications (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id      BIGINT UNSIGNED NOT NULL,
  type         ENUM('ttd_reminder','hb_reminder','info') NOT NULL,
  title        VARCHAR(120) NOT NULL,
  body         VARCHAR(255) NOT NULL,
  scheduled_at DATETIME NOT NULL,
  sent_at      DATETIME NULL,
  read_at      DATETIME NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notif_due (sent_at, scheduled_at),
  KEY idx_notif_user (user_id, created_at),
  CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Pengaturan ulang kata sandi lewat email. token_hash = SHA-256 dari token (link) atau dari
-- "<user_id>:<kode 6 digit>" (OTP). attempts membatasi tebakan kode. Dipakai setelah jalur email diputuskan.
CREATE TABLE IF NOT EXISTS password_resets (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  token_hash  CHAR(64) NOT NULL,
  attempts    TINYINT UNSIGNED NOT NULL DEFAULT 0,
  ip_address  VARCHAR(45) NULL,
  expires_at  DATETIME NOT NULL,
  used_at     DATETIME NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_reset_user (user_id, expires_at),
  KEY idx_reset_token (token_hash),
  CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
