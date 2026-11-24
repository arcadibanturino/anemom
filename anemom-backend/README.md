# Anemom Backend (PHP + MySQL di VPS)

```
database/   schema.sql, seed.sql, migrations/ (untuk DB yang sudah jalan), dev_sample_ingredients.sql (khusus uji)
api/        API PHP 8 tanpa framework (public/index.php = pintu masuk)
docs/       DATABASE.md (ERD + keputusan desain), API.md (kontrak endpoint)
postman/    koleksi Postman (variabel baseUrl + token otomatis)
tests/      smoke_test.py (menguji semua endpoint)
```

## Pasang di VPS (Ubuntu)

```bash
sudo apt install nginx php8.3-fpm php8.3-mysql php8.3-curl php8.3-mbstring mysql-server
# 1. Database (baru). Jika database sudah pernah dibuat dari versi lama, jalankan migrations/*.sql saja
sudo mysql < database/schema.sql && sudo mysql < database/seed.sql
sudo mysql -e "CREATE USER 'anemom_app'@'localhost' IDENTIFIED BY 'SANDI-KUAT'; GRANT ALL ON anemom.* TO 'anemom_app'@'localhost';"
# 2. API
sudo mkdir -p /var/www/anemom && sudo cp -r api /var/www/anemom/
sudo mkdir -p /var/www/anemom/storage && sudo chown -R www-data:www-data /var/www/anemom/storage
cd /var/www/anemom/api && cp .env.example .env && nano .env     # isi DB_PASS, STORAGE_PATH, dst.
# 3. nginx: arahkan root ke /var/www/anemom/api/public (server bersama: pakai port/subdomain sendiri), lalu  sudo nginx -t && sudo systemctl reload nginx
#    HTTPS:  sudo certbot --nginx -d api.contoh-domain.id
# 4. Ulang job AI mata yang tertunda (opsional): crontab -e  ->  * * * * * php /var/www/anemom/api/bin/process_jobs.php
# 5. Layanan AI mata (U-Net + fitur G1 + Random Forest): lihat ../ml-service/README.md, lalu isi di .env:
#    INFERENCE_URL=http://127.0.0.1:8001
```
Pada `php.ini` (FPM) pastikan `upload_max_filesize` dan `post_max_size` >= 10M.

## Uji lokal

```bash
cd api && cp .env.example .env              # sesuaikan DB_*
php -S 127.0.0.1:8080 -t public public/index.php
python3 tests/smoke_test.py http://127.0.0.1:8080     # pip install requests pillow
```
Tanpa `INFERENCE_URL`, AI Scan tanpa hasil on-device akan berstatus `queued` (normal bila `ml-service` belum dipasang).

## Keamanan yang sudah ada / belum
Ada: kata sandi di-hash (`password_hash`), token disimpan sebagai hash, query berparameter (PDO), validasi MIME gambar, data tiap pengguna terisolasi, penghapusan akun menyeluruh.
Belum: HTTPS (wajib di VPS, lihat certbot), pembatasan login (contoh di nginx.conf), pencadangan DB berkala (`mysqldump` + cron), pembersihan token kedaluwarsa.
