<?php
declare(strict_types=1);

namespace Anemom;

/** Autentikasi token opak: token acak dikirim ke aplikasi, yang disimpan di DB hanya hash SHA-256. */
final class Auth
{
    public static function issue(int $userId, ?string $deviceName, string $ip): array
    {
        $token = bin2hex(random_bytes(32));
        $ttl = Env::int('TOKEN_TTL_DAYS', 30);
        $expires = gmdate('Y-m-d H:i:s', time() + $ttl * 86400);
        Db::insert(
            'INSERT INTO auth_tokens (user_id, token_hash, device_name, ip_address, expires_at) VALUES (?,?,?,?,?)',
            [$userId, hash('sha256', $token), $deviceName, $ip, $expires]
        );
        return ['token' => $token, 'expires_at' => Time::iso($expires)];
    }

    public static function authenticate(Request $req): void
    {
        $token = $req->bearer();
        if ($token === null) {
            throw new ApiException(401, 'unauthenticated', 'Token tidak ditemukan. Login terlebih dahulu.');
        }
        $row = Db::one(
            'SELECT t.id AS token_id, u.* FROM auth_tokens t JOIN users u ON u.id = t.user_id
             WHERE t.token_hash = ? AND t.revoked_at IS NULL AND t.expires_at > UTC_TIMESTAMP()',
            [hash('sha256', $token)]
        );
        if ($row === null) {
            throw new ApiException(401, 'invalid_token', 'Token tidak valid atau sudah kedaluwarsa.');
        }
        if ($row['status'] !== 'active') {
            throw new ApiException(403, 'account_suspended', 'Akun dinonaktifkan.');
        }
        $req->tokenId = (int)$row['token_id'];
        unset($row['token_id'], $row['password_hash']);
        $req->user = $row;
        Db::run('UPDATE auth_tokens SET last_used_at = UTC_TIMESTAMP() WHERE id = ?', [$req->tokenId]);
    }
}

/** Penyimpanan berkas di disk VPS (di luar folder public). */
final class Storage
{
    public static function root(): string
    {
        $p = Env::get('STORAGE_PATH', dirname(__DIR__) . '/storage');
        if (!is_dir($p) && !mkdir($p, 0750, true) && !is_dir($p)) {
            throw new ApiException(500, 'storage_unavailable', 'Folder penyimpanan tidak dapat dibuat.');
        }
        return rtrim($p, '/');
    }

    public static function put(string $relativeDir, string $ext, string $tmpFile, bool $uploaded = true): string
    {
        $dir = self::root() . '/' . $relativeDir;
        if (!is_dir($dir) && !mkdir($dir, 0750, true) && !is_dir($dir)) {
            throw new ApiException(500, 'storage_unavailable', 'Folder penyimpanan tidak dapat dibuat.');
        }
        $rel = $relativeDir . '/' . bin2hex(random_bytes(16)) . '.' . $ext;
        $ok = $uploaded ? move_uploaded_file($tmpFile, self::root() . '/' . $rel) : copy($tmpFile, self::root() . '/' . $rel);
        if (!$ok) {
            throw new ApiException(500, 'storage_failed', 'Gagal menyimpan berkas.');
        }
        return $rel;
    }

    public static function abs(string $rel): string
    {
        return self::root() . '/' . ltrim($rel, '/');
    }

    public static function delete(?string $rel): void
    {
        if ($rel !== null && $rel !== '' && !str_contains($rel, '..')) {
            @unlink(self::abs($rel));
        }
    }
}

/** Unggahan gambar (foto mata / makanan). */
final class Images
{
    private const MIME = ['image/jpeg' => 'jpg', 'image/png' => 'png', 'image/webp' => 'webp'];

    public static function store(Request $req, string $field, string $purpose, ?string $capturedUtc, ?string $clientUuid, bool $trainingFlag): array
    {
        $f = $req->files[$field] ?? null;
        if (!$f || ($f['error'] ?? UPLOAD_ERR_NO_FILE) === UPLOAD_ERR_NO_FILE) {
            throw new ApiException(422, 'validation_failed', 'Gambar wajib diunggah.', [$field => 'Wajib diisi (multipart file).']);
        }
        if ($f['error'] !== UPLOAD_ERR_OK) {
            throw new ApiException(422, 'upload_failed', 'Unggahan gagal (kode ' . $f['error'] . ').');
        }
        $max = Env::int('MAX_UPLOAD_MB', 8) * 1024 * 1024;
        if ($f['size'] > $max) {
            throw new ApiException(413, 'file_too_large', 'Ukuran gambar maksimal ' . Env::int('MAX_UPLOAD_MB', 8) . ' MB.');
        }
        $mime = (new \finfo(FILEINFO_MIME_TYPE))->file($f['tmp_name']);
        $info = @getimagesize($f['tmp_name']);
        if (!isset(self::MIME[$mime]) || $info === false) {
            throw new ApiException(415, 'unsupported_media', 'Format harus JPEG, PNG, atau WebP.');
        }
        $uid = $req->userId();
        if ($trainingFlag && !self::hasConsent($uid, 'research_use')) {
            throw new ApiException(403, 'consent_required', 'Persetujuan "research_use" belum diberikan.');
        }
        $sha = hash_file('sha256', $f['tmp_name']);
        $rel = Storage::put("images/$purpose/$uid/" . gmdate('Y'), self::MIME[$mime], $f['tmp_name']);
        $id = Db::insert(
            'INSERT INTO images (user_id, client_uuid, purpose, file_path, mime_type, file_size, width, height, sha256, captured_at, use_for_training)
             VALUES (?,?,?,?,?,?,?,?,?,?,?)',
            [$uid, $clientUuid, $purpose, $rel, $mime, $f['size'], $info[0], $info[1], $sha, $capturedUtc ?? Time::now(), $trainingFlag ? 1 : 0]
        );
        return self::row($id);
    }

    public static function hasConsent(int $uid, string $type): bool
    {
        return Db::one(
            'SELECT 1 FROM consents WHERE user_id = ? AND type = ? AND revoked_at IS NULL LIMIT 1',
            [$uid, $type]
        ) !== null;
    }

    public static function row(int $id): array
    {
        return Db::one('SELECT * FROM images WHERE id = ?', [$id]);
    }

    public static function fmt(array $r): array
    {
        return [
            'id' => (int)$r['id'],
            'client_uuid' => $r['client_uuid'],
            'purpose' => $r['purpose'],
            'mime_type' => $r['mime_type'],
            'file_size' => (int)$r['file_size'],
            'width' => $r['width'] === null ? null : (int)$r['width'],
            'height' => $r['height'] === null ? null : (int)$r['height'],
            'captured_at' => Time::iso($r['captured_at']),
            'use_for_training' => (bool)$r['use_for_training'],
            'file_url' => '/api/v1/images/' . $r['id'] . '/file',
        ];
    }
}

/**
 * Foto ditolak layanan AI karena kualitasnya (mis. konjungtiva tidak terlihat). Bukan galat server:
 * job tidak perlu diulang, ibu cukup memotret ulang.
 */
final class InferenceRejected extends \RuntimeException
{
    public function __construct(public readonly string $errorCode, string $message, public readonly array $details = [])
    {
        parent::__construct($message);
    }
}

/**
 * Klien layanan AI mata di VPS (ml-service/, proses Python terpisah, hanya listen di localhost).
 * Model segmentasi bisa ditukar (U-Net sekarang, YOLOv8 untuk perbandingan); kontraknya sama.
 * Kontrak: POST {INFERENCE_URL}/predict/eye  (multipart: image, [segmenter = unet|yolo])
 *   -> 200 {"label":"anemia|non_anemia","confidence":0.0-1.0,"est_hb":number|null,
 *           "model_name":"...","model_version":"...","seg_model_name":"...","seg_model_version":"...", ...}
 *   -> 422 {"error":{"code":"poor_quality",...}}  foto perlu diulang (InferenceRejected)
 *   -> lainnya: galat sementara, job dicoba ulang (RuntimeException)
 */
final class Inference
{
    public static function configured(): bool
    {
        return Env::get('INFERENCE_URL') !== null;
    }

    public static function predictEye(string $absImagePath, string $mime, ?string $segmenter = null): array
    {
        if (!self::configured()) {
            throw new \RuntimeException('INFERENCE_URL belum diatur');
        }
        $fields = ['image' => new \CURLFile($absImagePath, $mime, 'eye')];
        if ($segmenter !== null) {
            $fields['segmenter'] = $segmenter;
        }
        $ch = curl_init(rtrim((string)Env::get('INFERENCE_URL'), '/') . '/predict/eye');
        curl_setopt_array($ch, [
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => $fields,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_CONNECTTIMEOUT => 3,
            CURLOPT_TIMEOUT => Env::int('INFERENCE_TIMEOUT', 20),
        ]);
        $out = curl_exec($ch);
        $code = (int)curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
        $err = curl_error($ch);
        curl_close($ch);
        $j = is_string($out) ? json_decode($out, true) : null;
        if ($code === 422 && is_array($j) && ($j['error']['code'] ?? null) === 'poor_quality') {
            throw new InferenceRejected(
                'poor_quality',
                (string)($j['error']['message'] ?? 'Konjungtiva tidak terlihat jelas. Ulangi foto.'),
                is_array($j['error']['details'] ?? null) ? $j['error']['details'] : []
            );
        }
        if ($out === false || $code !== 200) {
            $detail = is_array($j) ? (string)($j['error']['code'] ?? '') : '';
            throw new \RuntimeException(trim("Layanan AI mata gagal (HTTP $code) $detail $err"));
        }
        if (!is_array($j) || !isset($j['label'], $j['confidence'])) {
            throw new \RuntimeException('Respons layanan AI mata tidak sesuai kontrak');
        }
        return $j;
    }
}

final class Router
{
    private array $routes = [];

    public function add(string $method, string $pattern, callable $handler, bool $auth = true): void
    {
        $regex = '#^' . preg_replace('#\{(\w+)\}#', '(?P<$1>[^/]+)', $pattern) . '/?$#';
        $this->routes[] = [$method, $regex, $handler, $auth];
    }

    public function dispatch(Request $req): never
    {
        $allowed = [];
        foreach ($this->routes as [$method, $regex, $handler, $auth]) {
            if (!preg_match($regex, $req->path, $m)) {
                continue;
            }
            if ($method !== $req->method) {
                $allowed[] = $method;
                continue;
            }
            foreach ($m as $k => $v) {
                if (is_string($k)) {
                    $req->params[$k] = urldecode($v);
                }
            }
            if ($auth) {
                Auth::authenticate($req);
            }
            $handler($req);
            throw new \LogicException('Handler harus mengirim respons');
        }
        if ($allowed) {
            header('Allow: ' . implode(', ', array_unique($allowed)));
            throw new ApiException(405, 'method_not_allowed', 'Metode HTTP tidak diizinkan untuk endpoint ini.');
        }
        throw new ApiException(404, 'not_found', 'Endpoint tidak ditemukan.');
    }
}

/**
 * Notifikasi untuk layar Notifikasi di aplikasi. Belum ada cron/FCM, jadi notifikasi dibuat saat dibutuhkan:
 * setiap aplikasi meminta GET /notifications atau /notifications/unread-count, sync() mengecek kondisi ibu
 * dan menambahkan notifikasi yang belum ada. dedupe_key + UNIQUE (user_id, dedupe_key) mencegah duplikat.
 *
 *  - ttd_reminder : pengingat TTD hari ini (jika pengingat aktif dan TTD hari ini belum dicatat).
 *                   Otomatis ditandai dibaca setelah TTD hari itu dicatat.
 *  - ttd_done     : "Hebat, TTD sudah dicatat!" untuk catatan 3 hari terakhir (langsung berstatus dibaca).
 *  - hb_reminder  : belum ada hasil Hb, atau hasil terakhir lebih lama dari HB_REMINDER_DAYS.
 *  - checkup/info : disiapkan untuk jadwal kontrol/pengumuman (belum ada sumber datanya).
 */
final class Notifier
{
    /** Jarak (hari) sejak Hb terakhir sebelum muncul pengingat. Ubah sesuai arahan bidan/dosen. */
    public const HB_REMINDER_DAYS = 30;

    private const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'Mei', 'Jun', 'Jul', 'Agu', 'Sep', 'Okt', 'Nov', 'Des'];

    public static function sync(int $uid): void
    {
        $today = Time::today();
        $now = Time::now();
        $tz = Time::tz();
        $utc = new \DateTimeZone('UTC');

        // 1. Pengingat TTD hari ini
        $set = Db::one('SELECT reminder_on, reminder_time FROM ttd_settings WHERE user_id = ?', [$uid])
            ?? ['reminder_on' => 1, 'reminder_time' => '19:00:00'];
        if (\Anemom\Controllers\HealthController::reminderLocked($uid)) {
            $set['reminder_on'] = 1;
        }
        $takenToday = Db::one("SELECT 1 FROM ttd_logs WHERE user_id = ? AND taken_date = ? AND status = 'sudah'", [$uid, $today]) !== null;
        $ttdKey = 'ttd:' . $today;
        if ($takenToday) {
            Db::run(
                'UPDATE notifications SET read_at = ? WHERE user_id = ? AND dedupe_key = ? AND read_at IS NULL',
                [$now, $uid, $ttdKey]
            );
        } elseif ((int)$set['reminder_on'] === 1) {
            $time = substr((string)$set['reminder_time'], 0, 5);
            $evening = $time >= '15:00';
            self::add(
                $uid, 'ttd_reminder', $ttdKey,
                $evening ? 'TTD malam ini, Bunda' : 'TTD hari ini, Bunda',
                'Jadwal TTD pukul ' . str_replace(':', '.', $time) . ' setelah makan. Pastikan pengingat Bunda tetap aktif, ya.',
                $now
            );
        }

        // 2. TTD sudah dicatat (3 hari terakhir), langsung dibaca supaya tidak memunculkan titik merah
        $from = (new \DateTimeImmutable($today, $tz))->modify('-2 days')->format('Y-m-d');
        foreach (Db::all("SELECT taken_date, taken_time FROM ttd_logs WHERE user_id = ? AND taken_date >= ? AND status = 'sudah'", [$uid, $from]) as $log) {
            $at = (new \DateTimeImmutable($log['taken_date'] . ' ' . $log['taken_time'], $tz))->setTimezone($utc)->format('Y-m-d H:i:s');
            self::add(
                $uid, 'ttd_done', 'ttd_done:' . $log['taken_date'],
                'Hebat, TTD sudah dicatat!',
                'Bunda sudah minum TTD pada ' . self::dateText($log['taken_date']) . '. Lanjutkan kebiasaan baik ini setiap hari.',
                $at,
                $now
            );
        }

        // 3. Pengingat periksa Hb
        $last = Db::one(
            'SELECT test_date FROM hb_results WHERE user_id = ? AND deleted_at IS NULL ORDER BY test_date DESC, id DESC LIMIT 1',
            [$uid]
        );
        if ($last === null) {
            self::add(
                $uid, 'hb_reminder', 'hb:none',
                'Saatnya periksa Hb darah',
                'Belum ada hasil Hb yang tercatat. Periksa Hb di puskesmas atau laboratorium, lalu catat hasilnya. AI dan PPG tidak menggantikan tes darah.',
                $now
            );
        } else {
            $days = (int)(new \DateTimeImmutable($last['test_date'], $tz))->diff(new \DateTimeImmutable($today, $tz))->format('%a');
            if ($days >= self::HB_REMINDER_DAYS) {
                self::add(
                    $uid, 'hb_reminder', 'hb:after:' . $last['test_date'],
                    'Saatnya periksa Hb darah',
                    'Pemeriksaan Hb terakhir pada ' . self::dateText($last['test_date']) . '. Diskusikan jadwal periksa berikutnya dengan bidan. AI dan PPG tidak menggantikan tes darah.',
                    $now
                );
            }
        }
    }

    public static function unread(int $uid): int
    {
        return (int)Db::one(
            'SELECT COUNT(*) c FROM notifications WHERE user_id = ? AND read_at IS NULL AND scheduled_at <= UTC_TIMESTAMP()',
            [$uid]
        )['c'];
    }

    private static function add(
        int $uid, string $type, string $key, string $title, string $body, string $atUtc, ?string $readAtUtc = null
    ): void {
        Db::run(
            'INSERT IGNORE INTO notifications (user_id, type, title, body, dedupe_key, scheduled_at, sent_at, read_at)
             VALUES (?,?,?,?,?,?,?,?)',
            [$uid, $type, $title, $body, $key, $atUtc, $atUtc, $readAtUtc]
        );
    }

    /** '2026-11-03' -> '3 Nov 2026' */
    private static function dateText(string $ymd): string
    {
        [$y, $m, $d] = array_map('intval', explode('-', $ymd));
        return $d . ' ' . self::MONTHS[$m - 1] . ' ' . $y;
    }
}
