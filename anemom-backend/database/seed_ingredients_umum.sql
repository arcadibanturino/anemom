-- Daftar bahan makanan umum untuk semua ibu hamil (tanpa takaran/porsi dan tanpa angka gizi).
-- Takaran dan jumlah konsumsi ditentukan dokter gizi/tenaga kesehatan, bukan oleh aplikasi.
-- Aplikasi hanya menyarankan APA yang sebaiknya dimakan. Kolom iron_mg dan nilai gizi sengaja NULL
-- (belum diverifikasi ke TKPI 2017); isi nanti jika ada rujukan resmi.
--
-- Rujukan utama: Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id):
--   hewani (hati, ikan, daging, unggas) dan nabati (daun singkong, kangkung, sayuran hijau) = sumber zat besi;
--   vitamin C (jeruk, pepaya, mangga, jambu biji) membantu serapan; teh, kopi, susu, kalsium menghambat.
-- Baris dengan reference "Daftar umum ..." berasal dari daftar populer, bukan dokumen Kemenkes: tinjau dulu.
-- Hati ayam/sapi: is_recommended = 0 sampai ada keputusan tenaga kesehatan (Kemenkes menyebutnya sumber zat besi,
-- NHS menyarankan ibu hamil menghindarinya karena vitamin A).
-- Aman dijalankan ulang (ON DUPLICATE KEY UPDATE).
-- Nama mengikuti desain 25 Nov (Ayam, Telur, Ikan rendah merkuri, Pepaya matang); teks kartu/detail
-- dan bahan tambahan ada di migrations/2026-11-25_detail_bahan_makanan.sql: jalankan migrasi itu SESUDAH berkas ini
-- (berkas ini menimpa deskripsi dengan teks lama).
USE anemom;
INSERT INTO ingredients
 (name, category, iron_type, description, serving_desc, serving_grams, is_iron_booster, is_iron_inhibitor,
  is_recommended, recommend_order, pregnancy_warning, reference) VALUES
 ('Daging sapi', 'hewani', 'heme', 'Besi heme dari pangan hewani lebih mudah diserap. Pastikan dimasak matang.', 'porsi', 100, 0, 0, 1, 10, NULL, 'Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id)'),
 ('Ayam', 'hewani', 'heme', 'Besi heme dari pangan hewani lebih mudah diserap. Pastikan dimasak matang.', 'porsi', 100, 0, 0, 1, 11, NULL, 'Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id)'),
 ('Ikan rendah merkuri', 'hewani', 'heme', 'Besi heme dari pangan hewani lebih mudah diserap. Pastikan dimasak matang.', 'porsi', 100, 0, 0, 1, 12, NULL, 'Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id)'),
 ('Telur', 'hewani', 'heme', 'Besi heme dari pangan hewani lebih mudah diserap. Pastikan dimasak matang.', 'porsi', 100, 0, 0, 1, 13, NULL, 'Daftar umum sumber zat besi (detik.com, alodokter.com); belum diverifikasi ke TKPI'),
 ('Telur bebek', 'hewani', 'heme', 'Besi heme dari pangan hewani lebih mudah diserap. Pastikan dimasak matang.', 'porsi', 100, 0, 0, 1, 14, NULL, 'Daftar umum sumber zat besi (detik.com, alodokter.com); belum diverifikasi ke TKPI'),
 ('Udang', 'hewani', 'heme', 'Besi heme dari pangan hewani lebih mudah diserap. Pastikan dimasak matang.', 'porsi', 100, 0, 0, 1, 15, NULL, 'Daftar umum sumber zat besi (detik.com, alodokter.com); belum diverifikasi ke TKPI'),
 ('Ikan teri', 'hewani', 'heme', 'Besi heme dari pangan hewani lebih mudah diserap. Pastikan dimasak matang.', 'porsi', 100, 0, 0, 1, 16, NULL, 'Daftar umum sumber zat besi (detik.com, alodokter.com); belum diverifikasi ke TKPI'),
 ('Ikan baronang', 'hewani', 'heme', 'Besi heme dari pangan hewani lebih mudah diserap. Pastikan dimasak matang.', 'porsi', 100, 0, 0, 1, 17, NULL, 'Daftar umum sumber zat besi (detik.com, alodokter.com); belum diverifikasi ke TKPI'),
 ('Hati ayam', 'hewani', 'heme', 'Disebut sebagai sumber zat besi oleh Kemenkes, tetapi tinggi vitamin A. Tentukan bersama tenaga kesehatan apakah boleh ditampilkan.', 'porsi', 100, 0, 0, 0, 90, 'Tidak dianjurkan saat hamil: tinggi vitamin A', 'Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id); peringatan: NHS, Foods to avoid in pregnancy'),
 ('Hati sapi', 'hewani', 'heme', 'Disebut sebagai sumber zat besi oleh Kemenkes, tetapi tinggi vitamin A. Tentukan bersama tenaga kesehatan apakah boleh ditampilkan.', 'porsi', 100, 0, 0, 0, 91, 'Tidak dianjurkan saat hamil: tinggi vitamin A', 'Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id); peringatan: NHS, Foods to avoid in pregnancy'),
 ('Bayam', 'sayur', 'non_heme', 'Besi nabati; padukan dengan buah kaya vitamin C agar lebih terserap. Masak dahulu.', 'porsi', 100, 0, 0, 1, 30, NULL, 'Daftar umum sumber zat besi (detik.com, alodokter.com); belum diverifikasi ke TKPI'),
 ('Daun singkong', 'sayur', 'non_heme', 'Besi nabati; padukan dengan buah kaya vitamin C agar lebih terserap.', 'porsi', 100, 0, 0, 1, 31, NULL, 'Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id)'),
 ('Kangkung', 'sayur', 'non_heme', 'Besi nabati; padukan dengan buah kaya vitamin C agar lebih terserap.', 'porsi', 100, 0, 0, 1, 32, NULL, 'Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id)'),
 ('Sawi hijau', 'sayur', 'non_heme', 'Besi nabati; padukan dengan buah kaya vitamin C agar lebih terserap.', 'porsi', 100, 0, 0, 1, 33, NULL, 'Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id) (sayuran hijau lainnya)'),
 ('Tempe', 'nabati', 'non_heme', 'Besi nabati; padukan dengan buah kaya vitamin C agar lebih terserap.', 'porsi', 100, 0, 0, 1, 40, NULL, 'Daftar umum sumber zat besi (detik.com, alodokter.com); belum diverifikasi ke TKPI'),
 ('Tahu', 'nabati', 'non_heme', 'Besi nabati; padukan dengan buah kaya vitamin C agar lebih terserap.', 'porsi', 100, 0, 0, 1, 41, NULL, 'Daftar umum sumber zat besi (detik.com, alodokter.com); belum diverifikasi ke TKPI'),
 ('Kacang kedelai', 'nabati', 'non_heme', 'Besi nabati; padukan dengan buah kaya vitamin C agar lebih terserap.', 'porsi', 100, 0, 0, 1, 42, NULL, 'Daftar umum sumber zat besi (detik.com, alodokter.com); belum diverifikasi ke TKPI'),
 ('Kacang merah', 'nabati', 'non_heme', 'Besi nabati; padukan dengan buah kaya vitamin C agar lebih terserap.', 'porsi', 100, 0, 0, 1, 43, NULL, 'Daftar umum sumber zat besi (detik.com, alodokter.com); belum diverifikasi ke TKPI'),
 ('Kacang hijau', 'nabati', 'non_heme', 'Besi nabati; padukan dengan buah kaya vitamin C agar lebih terserap.', 'porsi', 100, 0, 0, 1, 44, NULL, 'Daftar umum sumber zat besi (detik.com, alodokter.com); belum diverifikasi ke TKPI'),
 ('Jeruk', 'buah', 'non_heme', 'Kaya vitamin C, membantu penyerapan zat besi. Cocok dimakan bersama menu zat besi.', 'porsi', 100, 1, 0, 1, 60, NULL, 'Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id)'),
 ('Pepaya matang', 'buah', 'non_heme', 'Kaya vitamin C, membantu penyerapan zat besi. Cocok dimakan bersama menu zat besi.', 'porsi', 100, 1, 0, 1, 61, NULL, 'Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id)'),
 ('Mangga', 'buah', 'non_heme', 'Kaya vitamin C, membantu penyerapan zat besi. Cocok dimakan bersama menu zat besi.', 'porsi', 100, 1, 0, 1, 62, NULL, 'Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id)'),
 ('Jambu biji', 'buah', 'non_heme', 'Kaya vitamin C, membantu penyerapan zat besi. Cocok dimakan bersama menu zat besi.', 'porsi', 100, 1, 0, 1, 63, NULL, 'Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id)'),
 ('Teh', 'minuman', NULL, 'Menghambat penyerapan zat besi. Beri jeda sekitar 2 jam dari TTD dan menu zat besi.', 'porsi', 100, 0, 1, 0, 100, NULL, 'Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id)'),
 ('Kopi', 'minuman', NULL, 'Menghambat penyerapan zat besi. Beri jeda sekitar 2 jam dari TTD dan menu zat besi.', 'porsi', 100, 0, 1, 0, 101, NULL, 'Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id)'),
 ('Susu dan olahannya', 'minuman', NULL, 'Menghambat penyerapan zat besi. Beri jeda sekitar 2 jam dari TTD dan menu zat besi. Susu tetap bergizi; cukup atur waktunya.', 'porsi', 100, 0, 1, 0, 102, NULL, 'Kemenkes RI, Buku Tablet Tambah Darah (promkes.kemkes.go.id)')
ON DUPLICATE KEY UPDATE
  category = VALUES(category), iron_type = VALUES(iron_type), description = VALUES(description),
  is_iron_booster = VALUES(is_iron_booster), is_iron_inhibitor = VALUES(is_iron_inhibitor),
  is_recommended = VALUES(is_recommended), recommend_order = VALUES(recommend_order),
  pregnancy_warning = VALUES(pregnancy_warning), reference = VALUES(reference);
