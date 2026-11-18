-- HANYA UNTUK UJI / DEVELOPMENT. JANGAN dimuat di server produksi.
-- Nilai gizi adalah perkiraan (USDA FoodData Central per 100 g), bukan rujukan resmi, dan semuanya
-- is_recommended = 0 sehingga tidak tampil di layar Rekomendasi.
USE anemom;
INSERT INTO ingredients
 (name, category, iron_type, serving_desc, serving_grams, calories_kcal, protein_g, iron_mg, vitamin_c_mg, folate_mcg,
  is_iron_booster, is_iron_inhibitor, pregnancy_warning, reference) VALUES
 ('Hati ayam (mentah)', 'hewani', 'heme', '100 g', 100, 119, 16.9, 9.00, 17.9, 588, 0, 0, 'Tidak dianjurkan saat hamil: tinggi vitamin A', 'contoh uji'),
 ('Daging sapi tanpa lemak (mentah)', 'hewani', 'heme', '100 g', 100, 143, 21.0, 2.60, 0, NULL, 0, 0, NULL, 'contoh uji'),
 ('Telur ayam rebus', 'hewani', 'heme', '100 g', 100, 155, 12.6, 1.19, 0, 44, 0, 0, NULL, 'contoh uji'),
 ('Bayam (mentah)', 'sayur', 'non_heme', '100 g', 100, 23, 2.9, 2.71, 28.1, 194, 1, 0, NULL, 'contoh uji'),
 ('Tempe', 'nabati', 'non_heme', '100 g', 100, 192, 20.3, 2.70, 0, NULL, 0, 0, NULL, 'contoh uji'),
 ('Kacang merah rebus', 'nabati', 'non_heme', '100 g', 100, 127, 8.7, 2.22, 1.2, 130, 0, 0, NULL, 'contoh uji'),
 ('Jeruk', 'buah', 'non_heme', '100 g', 100, 47, 0.9, 0.10, 53.2, 30, 1, 0, NULL, 'contoh uji'),
 ('Pepaya', 'buah', 'non_heme', '100 g', 100, 43, 0.5, 0.25, 60.9, 37, 1, 0, NULL, 'contoh uji'),
 ('Teh seduh', 'minuman', NULL, '1 cangkir (240 ml)', 240, 2, 0, 0.05, 0, NULL, 0, 1, NULL, 'contoh uji'),
 ('Kopi seduh', 'minuman', NULL, '1 cangkir (240 ml)', 240, 2, 0.3, 0.05, 0, NULL, 0, 1, NULL, 'contoh uji'),
 ('Susu sapi', 'minuman', NULL, '1 gelas (240 ml)', 240, 149, 7.7, 0.07, 0, 12, 0, 1, NULL, 'contoh uji')
ON DUPLICATE KEY UPDATE name = name;
