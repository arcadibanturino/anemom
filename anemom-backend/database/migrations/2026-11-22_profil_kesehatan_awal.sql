-- Migrasi untuk layar "Profil Kesehatan Awal" (langkah 2 pendaftaran).
-- Jalankan SEKALI:  sudo mysql < 2026-11-22_profil_kesehatan_awal.sql
-- Khusus MariaDB 10.5+ (memakai ADD COLUMN IF NOT EXISTS), jadi aman dijalankan ulang.
-- Data pengguna tidak berubah. Database baru dari schema.sql yang terbaru TIDAK perlu migrasi ini.
--
-- Tinggi badan dan berat sebelum hamil sudah ada di mother_profiles (height_cm, pre_pregnancy_weight_kg).
-- Status "profil lengkap" TIDAK disimpan sebagai kolom: API menghitungnya dari kolom wajib yang terisi
-- (tinggi, berat, riwayat anemia, status alergi), sehingga tidak bisa tidak sinkron dengan datanya.
-- Akun lama yang belum punya riwayat anemia/status alergi otomatis diminta melengkapinya saat masuk.
USE anemom;

ALTER TABLE mother_profiles
  ADD COLUMN IF NOT EXISTS anemia_history ENUM('ya','tidak','tidak_tahu') NULL AFTER pre_pregnancy_weight_kg,
  ADD COLUMN IF NOT EXISTS allergy_status ENUM('ada','tidak','tidak_tahu') NULL AFTER anemia_history,
  ADD COLUMN IF NOT EXISTS lila_cm DECIMAL(3,1) NULL AFTER allergy_status;   -- lingkar lengan atas, opsional

-- Bahan makanan yang dicentang ibu sebagai alergi (dipilih dari tabel ingredients, bukan diketik).
CREATE TABLE IF NOT EXISTS user_allergies (
  user_id       BIGINT UNSIGNED NOT NULL,
  ingredient_id INT UNSIGNED NOT NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, ingredient_id),
  KEY idx_allergy_ingredient (ingredient_id),
  CONSTRAINT fk_allergy_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_allergy_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients(id) ON DELETE CASCADE
) ENGINE=InnoDB;
