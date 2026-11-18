<?php
declare(strict_types=1);

namespace Anemom;

use DateTimeImmutable;
use DateTimeZone;
use PDO;

/** Konfigurasi dari api/.env atau environment. */
final class Env
{
    private static ?array $vars = null;

    public static function get(string $key, ?string $default = null): ?string
    {
        if (self::$vars === null) {
            self::$vars = [];
            $file = dirname(__DIR__) . '/.env';
            if (is_readable($file)) {
                foreach (file($file, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) as $line) {
                    $line = trim($line);
                    if ($line === '' || $line[0] === '#' || !str_contains($line, '=')) {
                        continue;
                    }
                    [$k, $v] = explode('=', $line, 2);
                    self::$vars[trim($k)] = trim($v);
                }
            }
        }
        $value = self::$vars[$key] ?? getenv($key);
        return ($value === false || $value === null || $value === '') ? $default : (string)$value;
    }

    public static function int(string $key, int $default): int
    {
        return (int)(self::get($key) ?? $default);
    }

    public static function bool(string $key): bool
    {
        return in_array(strtolower((string)self::get($key, 'false')), ['1', 'true', 'yes'], true);
    }
}

final class Db
{
    private static ?PDO $pdo = null;

    public static function pdo(): PDO
    {
        if (self::$pdo === null) {
            $dsn = sprintf(
                'mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4',
                Env::get('DB_HOST', '127.0.0.1'),
                Env::int('DB_PORT', 3306),
                Env::get('DB_NAME', 'anemom')
            );
            self::$pdo = new PDO($dsn, Env::get('DB_USER', 'root'), Env::get('DB_PASS', ''), [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES => false,
            ]);
            self::$pdo->exec("SET time_zone = '+00:00'");
        }
        return self::$pdo;
    }

    public static function one(string $sql, array $params = []): ?array
    {
        $st = self::pdo()->prepare($sql);
        $st->execute($params);
        $row = $st->fetch();
        return $row === false ? null : $row;
    }

    public static function all(string $sql, array $params = []): array
    {
        $st = self::pdo()->prepare($sql);
        $st->execute($params);
        return $st->fetchAll();
    }

    public static function run(string $sql, array $params = []): int
    {
        $st = self::pdo()->prepare($sql);
        $st->execute($params);
        return $st->rowCount();
    }

    public static function insert(string $sql, array $params = []): int
    {
        self::run($sql, $params);
        return (int)self::pdo()->lastInsertId();
    }
}

final class ApiException extends \Exception
{
    public function __construct(
        public readonly int $status,
        public readonly string $errorCode,
        string $message,
        public readonly array $details = []
    ) {
        parent::__construct($message);
    }
}

final class Request
{
    public ?array $user = null;
    public ?int $tokenId = null;
    public array $params = [];

    public function __construct(
        public readonly string $method,
        public readonly string $path,
        public readonly array $query,
        public readonly array $body,
        public readonly array $files,
        private readonly array $headers
    ) {
    }

    public static function capture(): self
    {
        $path = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
        $headers = [];
        foreach ($_SERVER as $k => $v) {
            if (str_starts_with($k, 'HTTP_')) {
                $headers[strtolower(str_replace('_', '-', substr($k, 5)))] = $v;
            }
        }
        if (isset($_SERVER['CONTENT_TYPE'])) {
            $headers['content-type'] = $_SERVER['CONTENT_TYPE'];
        }
        $body = $_POST;
        if (str_contains($headers['content-type'] ?? '', 'application/json')) {
            $raw = file_get_contents('php://input');
            $decoded = $raw === '' ? [] : json_decode($raw, true);
            if (!is_array($decoded)) {
                throw new ApiException(400, 'invalid_json', 'Body harus berupa JSON object yang valid.');
            }
            $body = $decoded;
        }
        return new self($_SERVER['REQUEST_METHOD'] ?? 'GET', $path, $_GET, $body, $_FILES, $headers);
    }

    public function header(string $name): ?string
    {
        return $this->headers[strtolower($name)] ?? null;
    }

    public function bearer(): ?string
    {
        $h = $this->header('authorization');
        return ($h !== null && preg_match('/^Bearer\s+(\S+)$/i', $h, $m)) ? $m[1] : null;
    }

    public function ip(): string
    {
        return $_SERVER['REMOTE_ADDR'] ?? '';
    }

    public function userId(): int
    {
        return (int)$this->user['id'];
    }

    public function param(string $name): string
    {
        return $this->params[$name];
    }

    public function intParam(string $name): int
    {
        return (int)$this->params[$name];
    }
}

final class Response
{
    public static function json(mixed $payload, int $status = 200): never
    {
        http_response_code($status);
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_PRESERVE_ZERO_FRACTION);
        exit;
    }

    public static function data(mixed $data, int $status = 200, array $meta = []): never
    {
        $out = ['data' => $data];
        if ($meta) {
            $out['meta'] = $meta;
        }
        self::json($out, $status);
    }

    public static function error(int $status, string $code, string $message, array $details = []): never
    {
        $err = ['code' => $code, 'message' => $message];
        if ($details) {
            $err['details'] = $details;
        }
        self::json(['error' => $err], $status);
    }
}

/** Validasi masukan; kumpulkan semua galat lalu lempar 422 sekaligus. */
final class Validator
{
    private array $errors = [];

    public function __construct(private readonly array $in)
    {
    }

    public function has(string $key): bool
    {
        return array_key_exists($key, $this->in) && $this->in[$key] !== null && $this->in[$key] !== '';
    }

    public function string(string $key, int $min = 1, int $max = 255, bool $required = true): ?string
    {
        if (!$this->has($key)) {
            if ($required) {
                $this->errors[$key] = 'Wajib diisi.';
            }
            return null;
        }
        $v = trim((string)$this->in[$key]);
        $len = mb_strlen($v);
        if ($len < $min || $len > $max) {
            $this->errors[$key] = "Panjang harus $min-$max karakter.";
            return null;
        }
        return $v;
    }

    public function email(string $key = 'email'): ?string
    {
        $v = $this->string($key, 3, 150);
        if ($v !== null && !filter_var($v, FILTER_VALIDATE_EMAIL)) {
            $this->errors[$key] = 'Format email tidak valid.';
            return null;
        }
        return $v === null ? null : strtolower($v);
    }

    public function password(string $key = 'password'): ?string
    {
        if (!$this->has($key)) {
            $this->errors[$key] = 'Wajib diisi.';
            return null;
        }
        $v = (string)$this->in[$key];
        if (strlen($v) < 8) {
            $this->errors[$key] = 'Kata sandi minimal 8 karakter.';
            return null;
        }
        return $v;
    }

    public function date(string $key, bool $required = true, bool $notFuture = false): ?string
    {
        if (!$this->has($key)) {
            if ($required) {
                $this->errors[$key] = 'Wajib diisi (YYYY-MM-DD).';
            }
            return null;
        }
        $v = (string)$this->in[$key];
        $d = DateTimeImmutable::createFromFormat('!Y-m-d', $v);
        if (!$d || $d->format('Y-m-d') !== $v) {
            $this->errors[$key] = 'Format tanggal harus YYYY-MM-DD.';
            return null;
        }
        if ($notFuture && $v > Time::today()) {   // bandingkan string tanggal (hindari beda zona waktu)
            $this->errors[$key] = 'Tanggal tidak boleh di masa depan.';
            return null;
        }
        return $v;
    }

    public function time(string $key, bool $required = false): ?string
    {
        if (!$this->has($key)) {
            if ($required) {
                $this->errors[$key] = 'Wajib diisi (HH:MM).';
            }
            return null;
        }
        $v = (string)$this->in[$key];
        if (!preg_match('/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/', $v)) {
            $this->errors[$key] = 'Format jam harus HH:MM.';
            return null;
        }
        return strlen($v) === 5 ? $v . ':00' : $v;
    }

    /** ISO 8601 (mis. 2026-11-17T04:41:00+08:00) -> 'Y-m-d H:i:s' UTC. */
    public function datetime(string $key, bool $required = true): ?string
    {
        if (!$this->has($key)) {
            if ($required) {
                $this->errors[$key] = 'Wajib diisi (ISO 8601).';
            }
            return null;
        }
        $utc = Time::parseToUtc((string)$this->in[$key]);
        if ($utc === null) {
            $this->errors[$key] = 'Format harus ISO 8601, mis. 2026-11-17T04:41:00+08:00.';
        }
        return $utc;
    }

    public function number(string $key, float $min, float $max, bool $required = true, int $decimals = 1): ?float
    {
        if (!$this->has($key)) {
            if ($required) {
                $this->errors[$key] = 'Wajib diisi.';
            }
            return null;
        }
        $raw = is_string($this->in[$key]) ? str_replace(',', '.', $this->in[$key]) : $this->in[$key];
        if (!is_numeric($raw) || (float)$raw < $min || (float)$raw > $max) {
            $this->errors[$key] = "Harus angka antara $min dan $max.";
            return null;
        }
        return round((float)$raw, $decimals);
    }

    public function int(string $key, int $min, int $max, bool $required = true): ?int
    {
        $n = $this->number($key, $min, $max, $required, 0);
        return $n === null ? null : (int)$n;
    }

    public function enum(string $key, array $allowed, bool $required = true): ?string
    {
        if (!$this->has($key)) {
            if ($required) {
                $this->errors[$key] = 'Wajib diisi.';
            }
            return null;
        }
        $v = (string)$this->in[$key];
        if (!in_array($v, $allowed, true)) {
            $this->errors[$key] = 'Harus salah satu dari: ' . implode(', ', $allowed) . '.';
            return null;
        }
        return $v;
    }

    public function uuid(string $key = 'client_uuid'): ?string
    {
        if (!$this->has($key)) {
            return null;
        }
        $v = strtolower((string)$this->in[$key]);
        if (!preg_match('/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/', $v)) {
            $this->errors[$key] = 'Harus UUID.';
            return null;
        }
        return $v;
    }

    public function bool(string $key, ?bool $default = null): ?bool
    {
        if (!$this->has($key)) {
            return $default;
        }
        $b = filter_var($this->in[$key], FILTER_VALIDATE_BOOLEAN, FILTER_NULL_ON_FAILURE);
        if ($b === null) {
            $this->errors[$key] = 'Harus true/false.';
        }
        return $b;
    }

    /** Objek JSON: boleh sudah array (body JSON) atau string JSON (multipart). */
    public function object(string $key): ?array
    {
        if (!$this->has($key)) {
            return null;
        }
        $v = $this->in[$key];
        if (is_string($v)) {
            $v = json_decode($v, true);
        }
        if (!is_array($v)) {
            $this->errors[$key] = 'Harus objek JSON.';
            return null;
        }
        return $v;
    }

    public function check(): void
    {
        if ($this->errors) {
            throw new ApiException(422, 'validation_failed', 'Data yang dikirim tidak valid.', $this->errors);
        }
    }
}

final class Time
{
    public static function tz(): DateTimeZone
    {
        return new DateTimeZone(Env::get('APP_TIMEZONE', 'Asia/Makassar'));
    }

    public static function now(): string
    {
        return gmdate('Y-m-d H:i:s');
    }

    public static function today(): string
    {
        return (new DateTimeImmutable('now', self::tz()))->format('Y-m-d');
    }

    public static function parseToUtc(string $iso): ?string
    {
        try {
            $d = new DateTimeImmutable($iso);
        } catch (\Exception) {
            return null;
        }
        return $d->setTimezone(new DateTimeZone('UTC'))->format('Y-m-d H:i:s');
    }

    /** 'Y-m-d H:i:s' (UTC) -> '2026-11-17T04:41:00Z'. */
    public static function iso(?string $utc): ?string
    {
        return $utc === null ? null : str_replace(' ', 'T', $utc) . 'Z';
    }

    /** Awal dan akhir sebuah tanggal lokal, dalam UTC. */
    public static function dayRangeUtc(string $date): array
    {
        $start = new DateTimeImmutable($date . ' 00:00:00', self::tz());
        $end = $start->modify('+1 day');
        $utc = new DateTimeZone('UTC');
        return [$start->setTimezone($utc)->format('Y-m-d H:i:s'), $end->setTimezone($utc)->format('Y-m-d H:i:s')];
    }
}

/** Logika domain: klasifikasi Hb dan kalkulasi kehamilan (sama dengan aplikasi Android). */
final class Domain
{
    private static ?array $levels = null;

    public static function levels(): array
    {
        if (self::$levels === null) {
            self::$levels = Db::all('SELECT code, label, min_hb FROM hb_risk_levels ORDER BY min_hb DESC');
        }
        return self::$levels;
    }

    public static function classify(float $hb): string
    {
        foreach (self::levels() as $lvl) {
            if ($hb >= (float)$lvl['min_hb']) {
                return $lvl['code'];
            }
        }
        return 'high';
    }

    public static function riskLabel(?string $code): ?string
    {
        foreach (self::levels() as $lvl) {
            if ($lvl['code'] === $code) {
                return $lvl['label'];
            }
        }
        return null;
    }

    public static function weeksBetween(string $hpht, ?string $on = null): int
    {
        $a = new DateTimeImmutable($hpht);
        $b = new DateTimeImmutable($on ?? Time::today());
        return max(0, intdiv((int)$a->diff($b)->format('%r%a'), 7));
    }

    public static function trimester(int $weeks): int
    {
        return $weeks < 14 ? 1 : ($weeks < 28 ? 2 : 3);
    }

    public static function dueDate(string $hpht): string
    {
        return (new DateTimeImmutable($hpht))->modify('+280 days')->format('Y-m-d');
    }

    public static function hphtOf(int $userId): ?string
    {
        $r = Db::one('SELECT hpht FROM mother_profiles WHERE user_id = ?', [$userId]);
        return $r['hpht'] ?? null;
    }
}
