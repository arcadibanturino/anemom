-- Detail bahan makanan + daftar bahan sesuai desain "Rekomendasi Makanan" (Sayur / Buah / Protein).
-- Jalankan sekali di server yang sudah berjalan:
--   sudo mysql anemom < 2026-11-25_detail_bahan_makanan.sql
-- Aman dijalankan ulang (ADD COLUMN IF NOT EXISTS, UPDATE IGNORE, ON DUPLICATE KEY UPDATE). Khusus MariaDB.
--
-- Isi:
--  1. Kolom baru di ingredients untuk layar Detail Bahan Makanan.
--  2. Nama disesuaikan desain (Daging ayam -> Ayam, Telur ayam -> Telur, Ikan segar -> Ikan rendah merkuri,
--     Pepaya -> Pepaya matang). ID tidak berubah, jadi user_allergies tetap benar.
--  3. Bahan baru dari desain (Kelor, Wortel, Brokoli, Apel, Pisang) + deskripsi kartu.
--  4. Isi detail untuk Bayam, Tempe, Ayam, Daging sapi (yang punya "Lihat Detail" di desain).
--  5. image_path untuk bahan yang belum punya foto (berkasnya disalin terpisah ke STORAGE_PATH/ingredients/).
--
-- Teks gizi diambil dari desain kelompok dan BELUM diverifikasi ke TKPI/bidan; kolom reference menandainya.
USE anemom;

-- 1. Kolom baru ------------------------------------------------------------------------------------
ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS subcategory_label VARCHAR(60) NULL AFTER category;   -- mis. "Sayuran hijau"
ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS key_nutrients VARCHAR(80) NULL AFTER description;    -- judul kartu sorotan di detail
ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS benefit TEXT NULL AFTER key_nutrients;               -- "Manfaat Nutrisi"
ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS preparation TEXT NULL AFTER benefit;                 -- "Cara memilih & menyiapkan"
ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS allergy_note TEXT NULL AFTER preparation;            -- alergi di "Sesuai risiko & alergi"

-- 2. Nama mengikuti desain ---------------------------------------------------------------------------
UPDATE IGNORE ingredients SET name = 'Ayam' WHERE name = 'Daging ayam';
UPDATE IGNORE ingredients SET name = 'Telur' WHERE name = 'Telur ayam';
UPDATE IGNORE ingredients SET name = 'Ikan rendah merkuri' WHERE name = 'Ikan segar';
UPDATE IGNORE ingredients SET name = 'Pepaya matang' WHERE name = 'Pepaya';

-- 3 + 4. Bahan desain: kartu dan detail -------------------------------------------------------------
-- reference hanya diisi bila masih kosong; image_path tidak disentuh di sini.
INSERT INTO ingredients
 (name, category, subcategory_label, iron_type, description, key_nutrients, benefit, preparation, allergy_note,
  is_iron_booster, is_iron_inhibitor, is_recommended, recommend_order, reference) VALUES
 -- Protein
 ('Daging sapi', 'hewani', 'Protein hewani', 'heme',
  'Zat besi heme, protein, dan vitamin B12.',
  'Zat besi heme & protein',
  'Daging sapi menyediakan zat besi heme yang lebih mudah diserap tubuh, protein, dan vitamin B12. Pilih bagian daging tanpa lemak.',
  'Pisahkan daging mentah dari bahan siap makan. Masak hingga matang sempurna; hindari daging setengah matang.',
  'Jika alergi daging sapi, pilih ayam, ikan rendah merkuri, atau protein nabati yang aman.',
  0, 0, 1, 10, 'Desain Anemom (Nov 2026); belum diverifikasi ke TKPI'),
 ('Ayam', 'hewani', 'Protein hewani', 'heme',
  'Protein untuk pertumbuhan jaringan; pilih daging, bukan hati.',
  'Protein & vitamin B12',
  'Daging ayam menyediakan protein untuk pertumbuhan jaringan dan vitamin B12. Pilih daging ayam, bukan hati ayam.',
  'Pisahkan ayam mentah dari bahan siap makan. Masak hingga matang sempurna; hindari bagian yang masih mentah.',
  'Jika alergi ayam, pilih sapi, ikan rendah merkuri, atau protein nabati yang aman.',
  0, 0, 1, 11, 'Desain Anemom (Nov 2026); belum diverifikasi ke TKPI'),
 ('Ikan rendah merkuri', 'hewani', 'Protein hewani', 'heme',
  'Protein dan omega-3; contoh: sarden, salmon, atau lele.',
  NULL, NULL, NULL, NULL,
  0, 0, 1, 12, 'Desain Anemom (Nov 2026); belum diverifikasi ke TKPI'),
 ('Telur', 'hewani', 'Protein hewani', 'heme',
  'Protein dan kolin; konsumsi setelah matang sempurna.',
  NULL, NULL, NULL, NULL,
  0, 0, 1, 13, 'Desain Anemom (Nov 2026); belum diverifikasi ke TKPI'),
 ('Tempe', 'nabati', 'Protein nabati', 'non_heme',
  'Protein nabati dan zat besi non-heme dari kedelai.',
  'Protein & zat besi nabati',
  'Tempe adalah sumber protein nabati dan zat besi non-heme dari kedelai. Padukan dengan sumber vitamin C.',
  'Simpan dengan baik dan masak hingga matang. Variasikan dengan sayur dan buah tanpa menambah porsi berlebihan.',
  'Mengandung kedelai. Hindari bila alergi kedelai; pilih sumber protein lain yang aman.',
  0, 0, 1, 14, 'Desain Anemom (Nov 2026); belum diverifikasi ke TKPI'),
 ('Tahu', 'nabati', 'Protein nabati', 'non_heme',
  'Protein nabati; kandungan kalsium bergantung pengolahan.',
  NULL, NULL, NULL, NULL,
  0, 0, 1, 15, 'Desain Anemom (Nov 2026); belum diverifikasi ke TKPI'),
 -- Sayur
 ('Kelor', 'sayur', 'Sayuran hijau', 'non_heme',
  'Zat besi non-heme dan folat untuk melengkapi gizi harian.',
  NULL, NULL, NULL, NULL,
  0, 0, 1, 30, 'Desain Anemom (Nov 2026); belum diverifikasi ke TKPI'),
 ('Bayam', 'sayur', 'Sayuran hijau', 'non_heme',
  'Folat, serat, dan zat besi non-heme. Padukan vitamin C.',
  'Folat & zat besi non-heme',
  'Bayam menyediakan folat, serat, dan zat besi non-heme. Vitamin C dari jeruk atau jambu membantu penyerapan zat besi.',
  'Cuci dengan air mengalir dan masak hingga matang. Sajikan segar sebagai bagian menu seimbang.',
  'Bila alergi bayam, pilih kelor atau brokoli jika aman bagi Bunda.',
  0, 0, 1, 31, 'Desain Anemom (Nov 2026); belum diverifikasi ke TKPI'),
 ('Wortel', 'sayur', 'Sayuran', NULL,
  'Beta-karoten dan serat untuk variasi sayur Bunda.',
  NULL, NULL, NULL, NULL,
  0, 0, 1, 32, 'Desain Anemom (Nov 2026); belum diverifikasi ke TKPI'),
 ('Brokoli', 'sayur', 'Sayuran hijau', 'non_heme',
  'Vitamin C, folat, dan serat untuk menu seimbang.',
  NULL, NULL, NULL, NULL,
  1, 0, 1, 33, 'Desain Anemom (Nov 2026); belum diverifikasi ke TKPI'),
 -- Buah
 ('Pepaya matang', 'buah', 'Buah', 'non_heme',
  'Vitamin C dan serat. Pilih pepaya yang matang.',
  NULL, NULL, NULL, NULL,
  1, 0, 1, 60, 'Desain Anemom (Nov 2026); belum diverifikasi ke TKPI'),
 ('Apel', 'buah', 'Buah', NULL,
  'Serat untuk mendukung kesehatan pencernaan.',
  NULL, NULL, NULL, NULL,
  0, 0, 1, 61, 'Desain Anemom (Nov 2026); belum diverifikasi ke TKPI'),
 ('Pisang', 'buah', 'Buah', NULL,
  'Kalium, vitamin B6, dan energi untuk aktivitas Bunda.',
  NULL, NULL, NULL, NULL,
  0, 0, 1, 62, 'Desain Anemom (Nov 2026); belum diverifikasi ke TKPI'),
 ('Jeruk', 'buah', 'Buah', 'non_heme',
  'Vitamin C membantu penyerapan zat besi non-heme.',
  NULL, NULL, NULL, NULL,
  1, 0, 1, 63, 'Desain Anemom (Nov 2026); belum diverifikasi ke TKPI'),
 ('Jambu biji', 'buah', 'Buah', 'non_heme',
  'Vitamin C dan serat; cuci sebelum dikonsumsi.',
  NULL, NULL, NULL, NULL,
  1, 0, 1, 64, 'Desain Anemom (Nov 2026); belum diverifikasi ke TKPI')
ON DUPLICATE KEY UPDATE
  category = VALUES(category), subcategory_label = VALUES(subcategory_label), iron_type = VALUES(iron_type),
  description = VALUES(description),
  key_nutrients = VALUES(key_nutrients), benefit = VALUES(benefit),
  preparation = VALUES(preparation), allergy_note = VALUES(allergy_note),
  is_iron_booster = VALUES(is_iron_booster), is_iron_inhibitor = VALUES(is_iron_inhibitor),
  is_recommended = VALUES(is_recommended), recommend_order = VALUES(recommend_order),
  reference = COALESCE(reference, VALUES(reference));

-- Bahan lama yang tidak ada di desain tetap tampil, diurutkan sesudah bahan desain di kelompoknya.
UPDATE ingredients SET recommend_order = 16 WHERE name = 'Udang';
UPDATE ingredients SET recommend_order = 17 WHERE name = 'Ikan teri';
UPDATE ingredients SET recommend_order = 18 WHERE name = 'Ikan baronang';
UPDATE ingredients SET recommend_order = 19 WHERE name = 'Telur bebek';
UPDATE ingredients SET recommend_order = 34 WHERE name = 'Daun singkong';
UPDATE ingredients SET recommend_order = 35 WHERE name = 'Kangkung';
UPDATE ingredients SET recommend_order = 36 WHERE name = 'Sawi hijau';
UPDATE ingredients SET recommend_order = 65 WHERE name = 'Mangga';

-- Label kecil di atas nama bahan (detail) untuk bahan yang belum diisi.
UPDATE ingredients SET subcategory_label = 'Protein hewani' WHERE subcategory_label IS NULL AND category = 'hewani';
UPDATE ingredients SET subcategory_label = 'Protein nabati' WHERE subcategory_label IS NULL AND category = 'nabati';
UPDATE ingredients SET subcategory_label = 'Sayuran hijau'
  WHERE subcategory_label IS NULL AND name IN ('Daun singkong', 'Kangkung', 'Sawi hijau');
UPDATE ingredients SET subcategory_label = 'Sayuran' WHERE subcategory_label IS NULL AND category = 'sayur';
UPDATE ingredients SET subcategory_label = 'Buah' WHERE subcategory_label IS NULL AND category = 'buah';

-- 5. Foto: jalur saja. Salin berkasnya ke /var/www/anemom/storage/ingredients/ (nama sama).
--    Selama berkas belum ada, aplikasi menampilkan ikon pengganti (server menjawab 404).
--    Bahan yang sudah punya image_path tidak diubah.
UPDATE ingredients SET image_path = CASE name
    WHEN 'Daging sapi' THEN 'ingredients/daging-sapi.jpg'
    WHEN 'Ayam' THEN 'ingredients/ayam.jpg'
    WHEN 'Ikan rendah merkuri' THEN 'ingredients/ikan-rendah-merkuri.jpg'
    WHEN 'Telur' THEN 'ingredients/telur.jpg'
    WHEN 'Tempe' THEN 'ingredients/tempe.jpg'
    WHEN 'Tahu' THEN 'ingredients/tahu.jpg'
    WHEN 'Kelor' THEN 'ingredients/kelor.jpg'
    WHEN 'Bayam' THEN 'ingredients/bayam.jpg'
    WHEN 'Wortel' THEN 'ingredients/wortel.jpg'
    WHEN 'Brokoli' THEN 'ingredients/brokoli.jpg'
    WHEN 'Pepaya matang' THEN 'ingredients/pepaya-matang.jpg'
    WHEN 'Apel' THEN 'ingredients/apel.jpg'
    WHEN 'Pisang' THEN 'ingredients/pisang.jpg'
    WHEN 'Jeruk' THEN 'ingredients/jeruk.jpg'
    WHEN 'Jambu biji' THEN 'ingredients/jambu-biji.jpg'
  END
WHERE image_path IS NULL
  AND name IN ('Daging sapi', 'Ayam', 'Ikan rendah merkuri', 'Telur', 'Tempe', 'Tahu', 'Kelor', 'Bayam',
               'Wortel', 'Brokoli', 'Pepaya matang', 'Apel', 'Pisang', 'Jeruk', 'Jambu biji');

-- Cek hasil:
-- SELECT name, category, subcategory_label, recommend_order, benefit IS NOT NULL AS ada_detail, image_path
--   FROM ingredients WHERE is_recommended = 1 ORDER BY recommend_order;
