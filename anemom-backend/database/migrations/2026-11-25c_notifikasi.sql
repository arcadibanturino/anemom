-- Notifikasi dalam aplikasi (layar Notifikasi + titik merah di Beranda).
-- Jalankan SEBELUM memasang SystemController.php / Services.php baru:
--   sudo mysql anemom < 2026-11-25c_notifikasi.sql
-- Aman dijalankan ulang. Khusus MariaDB.
USE anemom;

-- Jenis baru: ttd_done ("TTD sudah dicatat") dan checkup (jadwal kontrol, belum ada sumber datanya).
ALTER TABLE notifications
  MODIFY COLUMN type ENUM('ttd_reminder','ttd_done','hb_reminder','checkup','info') NOT NULL;

-- Kunci pencegah duplikat untuk notifikasi yang dibuat otomatis (mis. 'ttd:2026-11-25').
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS dedupe_key VARCHAR(60) NULL AFTER body;
ALTER TABLE notifications ADD UNIQUE INDEX IF NOT EXISTS uq_notif_dedupe (user_id, dedupe_key);

-- Cek:
-- SHOW CREATE TABLE notifications;
