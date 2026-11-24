-- Jam pengingat TTD bawaan 19.00 (sebelumnya 20.00).
-- Sebelum 25 Nov aplikasi belum punya layar ubah jam, jadi semua 20.00 adalah nilai bawaan: ikut diganti.
-- Pengingat boleh dimatikan, kecuali Hb lab terakhir di bawah normal (dicek di server, tanpa kolom baru).
-- Jalankan: sudo mysql anemom < 2026-11-25e_pengingat_19.sql   (aman dijalankan ulang)
USE anemom;
ALTER TABLE ttd_settings ALTER COLUMN reminder_time SET DEFAULT '19:00:00';
UPDATE ttd_settings SET reminder_time = '19:00:00' WHERE reminder_time = '20:00:00';
