-- Isi detail (Lihat Detail) untuk semua bahan yang belum punya. Lanjutan 2026-11-25_detail_bahan_makanan.sql.
-- Jalankan: sudo mysql anemom < 2026-11-25b_detail_semua_bahan.sql   (aman dijalankan ulang)
-- Dicocokkan lewat nama (ID bisa beda antar server). Teks belum diverifikasi bidan/TKPI.
-- Hati, teh, kopi, susu tetap is_recommended = 0 (tidak tampil di daftar); detailnya disiapkan bila nanti ditampilkan.
USE anemom;
START TRANSACTION;

UPDATE ingredients SET key_nutrients = 'Protein & omega-3',
  benefit = 'Ikan menyediakan protein, zat besi heme, dan asam lemak omega-3 (DHA) yang mendukung perkembangan otak janin. Pilih ikan rendah merkuri seperti sarden, salmon, lele, atau kembung.',
  preparation = 'Pilih ikan segar: mata jernih, insang merah, daging kenyal. Masak hingga matang; hindari ikan mentah atau setengah matang seperti sushi.',
  allergy_note = 'Jika alergi ikan, pilih ayam, daging sapi, telur, atau protein nabati yang aman. Hindari ikan tinggi merkuri seperti hiu dan ikan todak.'
WHERE name = 'Ikan rendah merkuri';

UPDATE ingredients SET key_nutrients = 'Protein & kolin',
  benefit = 'Telur menyediakan protein lengkap, kolin, dan vitamin B12. Kolin berperan dalam perkembangan otak dan saraf janin.',
  preparation = 'Pilih telur dengan cangkang bersih dan tidak retak. Masak hingga putih dan kuningnya padat; hindari telur mentah atau setengah matang.',
  allergy_note = 'Telur termasuk pemicu alergi yang umum. Jika alergi telur, pilih tempe, tahu, ikan, atau ayam yang aman.'
WHERE name = 'Telur';

UPDATE ingredients SET key_nutrients = 'Protein & mineral',
  benefit = 'Udang menyediakan protein, zat besi, seng, dan yodium dengan kandungan lemak yang rendah.',
  preparation = 'Pilih udang segar yang tidak berbau menyengat. Buang kotoran di punggungnya dan masak hingga warnanya merah-oranye merata; hindari udang mentah.',
  allergy_note = 'Udang termasuk pemicu alergi makanan laut yang umum. Jika alergi udang atau kerang, pilih ikan, ayam, atau protein nabati yang aman.'
WHERE name = 'Udang';

UPDATE ingredients SET key_nutrients = 'Protein & kalsium',
  benefit = 'Ikan teri dimakan utuh bersama tulangnya, sehingga menyediakan protein, kalsium, dan zat besi.',
  preparation = 'Pilih teri yang kering dan tidak berbau apek. Bilas atau rendam sebentar untuk mengurangi garam, lalu masak hingga matang. Batasi teri asin.',
  allergy_note = 'Jika alergi ikan, pilih ayam, daging sapi, telur, atau protein nabati yang aman.'
WHERE name = 'Ikan teri';

UPDATE ingredients SET key_nutrients = 'Protein & lemak sehat',
  benefit = 'Ikan baronang adalah ikan laut yang mudah ditemukan di pasar Sulawesi Selatan dan menyediakan protein serta lemak sehat untuk variasi lauk.',
  preparation = 'Pilih ikan segar dengan mata jernih dan insang merah. Hati-hati dengan siripnya yang tajam saat membersihkan. Bakar atau kukus hingga matang.',
  allergy_note = 'Jika alergi ikan, pilih ayam, daging sapi, telur, atau protein nabati yang aman.'
WHERE name = 'Ikan baronang';

UPDATE ingredients SET key_nutrients = 'Protein & vitamin B12',
  benefit = 'Telur bebek menyediakan protein, lemak, dan vitamin B12. Ukurannya lebih besar dari telur ayam.',
  preparation = 'Pilih telur dengan cangkang utuh dan bersih. Masak hingga matang sempurna. Batasi telur asin karena tinggi garam.',
  allergy_note = 'Bila alergi telur ayam, telur bebek juga sering memicu alergi. Pilih sumber protein lain yang aman.'
WHERE name = 'Telur bebek';

UPDATE ingredients SET key_nutrients = 'Tinggi vitamin A',
  benefit = 'Hati ayam kaya zat besi heme, tetapi juga sangat tinggi vitamin A (retinol). Vitamin A berlebih saat hamil dapat membahayakan janin.',
  preparation = 'Tidak dianjurkan saat hamil kecuali atas saran tenaga kesehatan. Untuk zat besi heme, pilih daging sapi, ayam, atau ikan rendah merkuri.',
  allergy_note = 'Bila perlu sumber zat besi hewani, pilih daging sapi, ayam, atau ikan rendah merkuri yang aman.'
WHERE name = 'Hati ayam';

UPDATE ingredients SET key_nutrients = 'Tinggi vitamin A',
  benefit = 'Hati sapi kaya zat besi heme, tetapi juga sangat tinggi vitamin A (retinol). Vitamin A berlebih saat hamil dapat membahayakan janin.',
  preparation = 'Tidak dianjurkan saat hamil kecuali atas saran tenaga kesehatan. Untuk zat besi heme, pilih daging sapi, ayam, atau ikan rendah merkuri.',
  allergy_note = 'Bila perlu sumber zat besi hewani, pilih daging sapi, ayam, atau ikan rendah merkuri yang aman.'
WHERE name = 'Hati sapi';

UPDATE ingredients SET key_nutrients = 'Protein nabati',
  benefit = 'Tahu menyediakan protein nabati dan zat besi non-heme dari kedelai. Tahu yang dibuat dengan penggumpal kalsium juga mengandung kalsium.',
  preparation = 'Pilih tahu segar yang tidak berlendir dan tidak berbau asam. Simpan di kulkas dalam air bersih dan masak sebelum dimakan.',
  allergy_note = 'Mengandung kedelai. Hindari bila alergi kedelai; pilih telur, ikan, atau ayam yang aman.'
WHERE name = 'Tahu';

UPDATE ingredients SET key_nutrients = 'Protein & zat besi nabati',
  benefit = 'Kedelai menyediakan protein nabati, serat, folat, dan zat besi non-heme. Padukan dengan sumber vitamin C agar zat besinya lebih terserap.',
  preparation = 'Rendam beberapa jam, lalu rebus hingga empuk. Jangan dimakan mentah.',
  allergy_note = 'Kedelai termasuk pemicu alergi yang umum. Hindari bila alergi kedelai; pilih kacang merah atau kacang hijau jika aman.'
WHERE name = 'Kacang kedelai';

UPDATE ingredients SET key_nutrients = 'Serat, folat & zat besi',
  benefit = 'Kacang merah menyediakan serat, folat, protein nabati, dan zat besi non-heme.',
  preparation = 'Rendam semalaman, buang air rendamannya, lalu rebus hingga benar-benar empuk. Kacang merah mentah atau setengah matang dapat menyebabkan mual dan muntah.',
  allergy_note = 'Bila tidak cocok dengan kacang-kacangan, pilih tempe, tahu, atau protein hewani yang aman.'
WHERE name = 'Kacang merah';

UPDATE ingredients SET key_nutrients = 'Folat & protein nabati',
  benefit = 'Kacang hijau menyediakan folat, serat, protein nabati, dan zat besi non-heme. Sering diolah menjadi bubur.',
  preparation = 'Cuci dan rendam sebentar, lalu rebus hingga empuk. Batasi gula saat membuat bubur kacang hijau.',
  allergy_note = 'Alergi kacang hijau jarang terjadi. Bila muncul gatal atau ruam, hentikan dan pilih sumber protein lain.'
WHERE name = 'Kacang hijau';

UPDATE ingredients SET key_nutrients = 'Zat besi non-heme & folat',
  benefit = 'Daun kelor menyediakan zat besi non-heme, folat, kalsium, dan beta-karoten. Cocok dimasak sebagai sayur bening.',
  preparation = 'Petik daun yang hijau segar, cuci bersih, dan masak hingga layu. Konsumsi daunnya sebagai sayur; hindari suplemen atau ekstrak kelor tanpa saran tenaga kesehatan.',
  allergy_note = 'Bila alergi kelor, pilih bayam atau daun singkong jika aman bagi Bunda.'
WHERE name = 'Kelor';

UPDATE ingredients SET key_nutrients = 'Beta-karoten & serat',
  benefit = 'Wortel menyediakan beta-karoten yang diubah tubuh menjadi vitamin A sesuai kebutuhan, serta serat untuk pencernaan.',
  preparation = 'Pilih wortel yang keras dan berwarna oranye cerah. Cuci dan kupas; bisa dimasak atau dimakan mentah setelah dicuci bersih.',
  allergy_note = 'Bila alergi wortel, pilih labu kuning atau sayuran lain yang aman.'
WHERE name = 'Wortel';

UPDATE ingredients SET key_nutrients = 'Vitamin C & folat',
  benefit = 'Brokoli menyediakan vitamin C, folat, dan serat. Vitamin C membantu penyerapan zat besi non-heme dari bahan lain di piring yang sama.',
  preparation = 'Pilih kuntum yang hijau rapat dan tidak menguning. Rendam dan cuci bersih, lalu kukus atau rebus sebentar agar vitamin C tidak banyak hilang.',
  allergy_note = 'Bila alergi brokoli, pilih bayam atau sawi hijau jika aman bagi Bunda.'
WHERE name = 'Brokoli';

UPDATE ingredients SET key_nutrients = 'Zat besi non-heme',
  benefit = 'Daun singkong disebut Kemenkes sebagai sayuran sumber zat besi nabati, dan juga menyediakan protein serta vitamin C.',
  preparation = 'Wajib dimasak matang: rebus hingga empuk lalu buang air rebusannya, karena daun mentah mengandung senyawa sianida alami.',
  allergy_note = 'Bila tidak cocok, pilih bayam, kangkung, atau kelor jika aman bagi Bunda.'
WHERE name = 'Daun singkong';

UPDATE ingredients SET key_nutrients = 'Zat besi non-heme & beta-karoten',
  benefit = 'Kangkung menyediakan zat besi non-heme, beta-karoten, dan serat.',
  preparation = 'Cuci batang dan daun di air mengalir karena kangkung sering tumbuh di air. Masak hingga matang, misalnya ditumis atau dibuat sayur bening.',
  allergy_note = 'Bila tidak cocok, pilih bayam atau sawi hijau jika aman bagi Bunda.'
WHERE name = 'Kangkung';

UPDATE ingredients SET key_nutrients = 'Folat & vitamin C',
  benefit = 'Sawi hijau menyediakan folat, vitamin C, kalsium, dan zat besi non-heme.',
  preparation = 'Pilih daun yang segar dan tidak berlubang. Cuci per helai, lalu masak sebentar agar tetap renyah.',
  allergy_note = 'Bila tidak cocok, pilih bayam atau brokoli jika aman bagi Bunda.'
WHERE name = 'Sawi hijau';

UPDATE ingredients SET key_nutrients = 'Vitamin C & serat',
  benefit = 'Pepaya matang kaya vitamin C yang membantu penyerapan zat besi, serta serat yang membantu mengatasi sembelit saat hamil.',
  preparation = 'Pilih pepaya yang benar-benar matang: kulit kuning-oranye dan daging lembut. Hindari pepaya muda atau setengah matang karena getahnya.',
  allergy_note = 'Bila alergi pepaya atau lateks, pilih jeruk atau jambu biji jika aman.'
WHERE name = 'Pepaya matang';

UPDATE ingredients SET key_nutrients = 'Serat',
  benefit = 'Apel menyediakan serat (pektin) yang membantu pencernaan dan praktis sebagai camilan.',
  preparation = 'Cuci bersih sebelum dimakan; makan bersama kulitnya agar seratnya lebih banyak.',
  allergy_note = 'Bila alergi apel, pilih pepaya matang, pisang, atau buah lain yang aman.'
WHERE name = 'Apel';

UPDATE ingredients SET key_nutrients = 'Kalium & vitamin B6',
  benefit = 'Pisang menyediakan kalium, vitamin B6, dan energi. Vitamin B6 dapat membantu mengurangi mual pada awal kehamilan.',
  preparation = 'Pilih pisang matang dengan kulit kuning. Cocok sebagai camilan atau dicampur ke sarapan.',
  allergy_note = 'Bila alergi pisang (sering terkait alergi lateks), pilih apel atau pepaya matang jika aman.'
WHERE name = 'Pisang';

UPDATE ingredients SET key_nutrients = 'Vitamin C',
  benefit = 'Jeruk kaya vitamin C. Makan bersama sumber zat besi seperti sayur hijau, tempe, atau daging agar zat besinya lebih mudah diserap.',
  preparation = 'Pilih jeruk yang segar dan terasa berat di tangan. Lebih baik dimakan utuh daripada jus bergula.',
  allergy_note = 'Bila tidak cocok dengan jeruk, pilih jambu biji atau pepaya matang sebagai sumber vitamin C.'
WHERE name = 'Jeruk';

UPDATE ingredients SET key_nutrients = 'Vitamin C tinggi',
  benefit = 'Jambu biji termasuk buah dengan vitamin C sangat tinggi dan kaya serat, sehingga baik dipadukan dengan menu sumber zat besi.',
  preparation = 'Cuci bersih sebelum dimakan. Bijinya keras; kunyah perlahan atau buat jus tanpa gula tambahan.',
  allergy_note = 'Bila tidak cocok, pilih jeruk atau pepaya matang jika aman.'
WHERE name = 'Jambu biji';

UPDATE ingredients SET key_nutrients = 'Vitamin C & beta-karoten',
  benefit = 'Mangga matang menyediakan vitamin C dan beta-karoten. Vitamin C membantu penyerapan zat besi dari menu Bunda.',
  preparation = 'Pilih mangga matang yang harum dan cuci kulitnya sebelum dikupas. Mangga cukup manis; sesuaikan bila Bunda perlu membatasi gula.',
  allergy_note = 'Getah kulit mangga dapat memicu gatal pada sebagian orang. Bila alergi, pilih jeruk atau jambu biji.'
WHERE name = 'Mangga';

UPDATE ingredients SET key_nutrients = 'Penghambat zat besi',
  benefit = 'Teh mengandung tanin yang menghambat penyerapan zat besi, juga mengandung kafein.',
  preparation = 'Beri jeda sekitar 2 jam dari TTD dan menu sumber zat besi. Jangan minum TTD dengan teh.',
  allergy_note = 'Batasi kafein selama hamil sesuai anjuran bidan atau dokter.'
WHERE name = 'Teh';

UPDATE ingredients SET key_nutrients = 'Penghambat zat besi & kafein',
  benefit = 'Kopi menghambat penyerapan zat besi dan mengandung kafein. Kafein berlebih saat hamil sebaiknya dihindari.',
  preparation = 'Beri jeda sekitar 2 jam dari TTD dan menu sumber zat besi. Batasi jumlahnya sesuai anjuran tenaga kesehatan.',
  allergy_note = 'Bila ingin minuman hangat, pilih air putih hangat atau minuman tanpa kafein.'
WHERE name = 'Kopi';

UPDATE ingredients SET key_nutrients = 'Kalsium & protein',
  benefit = 'Susu dan olahannya menyediakan kalsium dan protein, tetapi kalsium menghambat penyerapan zat besi bila dikonsumsi bersamaan.',
  preparation = 'Pilih susu yang sudah dipasteurisasi. Beri jeda sekitar 2 jam dari TTD dan menu sumber zat besi; susu tetap boleh diminum di waktu lain.',
  allergy_note = 'Bila alergi susu sapi atau intoleransi laktosa, diskusikan sumber kalsium pengganti dengan bidan atau dokter.'
WHERE name = 'Susu dan olahannya';

UPDATE ingredients SET subcategory_label = 'Minuman' WHERE subcategory_label IS NULL AND category = 'minuman';
COMMIT;

-- Cek: semua bahan harus detail = 1
SELECT name, is_recommended AS rek, benefit IS NOT NULL AS detail FROM ingredients ORDER BY category, recommend_order;
