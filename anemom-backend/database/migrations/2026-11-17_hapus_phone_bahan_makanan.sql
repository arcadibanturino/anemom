-- Migrasi untuk database yang SUDAH dibuat dari schema.sql versi lama (sebelum 17 Nov 2026 siang).
-- Jalankan SEKALI:  sudo mysql < 2026-11-17_hapus_phone_bahan_makanan.sql
-- Khusus MariaDB 10.5+ (memakai DROP COLUMN IF EXISTS). Aman untuk data pengguna: hanya kolom phone yang hilang.
-- Database baru dari schema.sql yang terbaru TIDAK perlu migrasi ini.
USE anemom;

-- 1. Nomor telepon tidak dipakai lagi
ALTER TABLE users DROP COLUMN IF EXISTS phone;

-- 2. Tabel foods menjadi ingredients (bahan makanan + rekomendasi)
ALTER TABLE food_logs DROP FOREIGN KEY fk_foodlog_food;
RENAME TABLE foods TO ingredients;
ALTER TABLE ingredients
  RENAME INDEX uq_food_name TO uq_ingredient_name,
  RENAME INDEX idx_food_cat TO idx_ingredient_cat,
  CHANGE source reference VARCHAR(255) NULL,
  ADD COLUMN description TEXT NULL AFTER category,
  ADD COLUMN iron_type ENUM('heme','non_heme') NULL AFTER description,
  ADD COLUMN is_recommended TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN recommend_order SMALLINT UNSIGNED NOT NULL DEFAULT 100,
  ADD COLUMN pregnancy_warning VARCHAR(255) NULL,
  ADD COLUMN image_path VARCHAR(255) NULL,
  ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  ADD KEY idx_ingredient_reco (is_recommended, recommend_order);
ALTER TABLE food_logs CHANGE food_id ingredient_id INT UNSIGNED NULL;

-- 3. Buang data contoh lama (perkiraan USDA, belum ada rujukan resmi) beserta catatan makan ujinya
DELETE FROM food_logs WHERE ingredient_id IN (SELECT id FROM ingredients WHERE reference LIKE '%perkiraan%');
DELETE FROM ingredients WHERE reference LIKE '%perkiraan%';

ALTER TABLE food_logs
  ADD CONSTRAINT fk_foodlog_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients(id) ON DELETE SET NULL;

-- 4. Tabel pengaturan ulang kata sandi
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
