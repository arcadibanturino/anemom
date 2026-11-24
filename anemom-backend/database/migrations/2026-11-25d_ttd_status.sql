-- Status TTD dari alarm: "Sudah" (diminum) dan "Belum" (dilaporkan belum diminum).
-- Hari tanpa catatan = "Belum dikonfirmasi". Kepatuhan hanya menghitung "Sudah".
-- Jalankan SEBELUM memasang HealthController.php / Services.php baru:
--   sudo mysql anemom < 2026-11-25d_ttd_status.sql
-- Aman dijalankan ulang. Semua catatan lama otomatis berstatus 'sudah'.
USE anemom;
ALTER TABLE ttd_logs
  ADD COLUMN IF NOT EXISTS status ENUM('sudah','belum') NOT NULL DEFAULT 'sudah' AFTER taken_time;
