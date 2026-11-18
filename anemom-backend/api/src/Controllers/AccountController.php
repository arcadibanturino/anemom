<?php
declare(strict_types=1);

namespace Anemom\Controllers;

use Anemom\ApiException;
use Anemom\Auth;
use Anemom\Db;
use Anemom\Domain;
use Anemom\Request;
use Anemom\Response;
use Anemom\Storage;
use Anemom\Time;
use Anemom\Validator;

final class AccountController
{
    private const MAX_WEEKS = 42;
    private const MIN_AGE = 10;
    private const MAX_AGE = 60;

    public static function register(Request $req): never
    {
        $v = new Validator($req->body);
        $name = $v->string('name', 2, 100);
        $email = $v->email();
        $password = $v->password();
        $hpht = $v->date('hpht', true, true);
        $birth = $v->date('birth_date', true, true);
        $facility = $v->string('health_facility', 1, 150, false);
        $device = $v->string('device_name', 1, 100, false);
        $v->check();

        if (Domain::weeksBetween($hpht) > self::MAX_WEEKS) {
            throw new ApiException(422, 'validation_failed', 'Data yang dikirim tidak valid.', ['hpht' => 'HPHT lebih dari 42 minggu lalu.']);
        }
        self::checkBirthDate($birth);
        if (Db::one('SELECT 1 FROM users WHERE email = ?', [$email])) {
            throw new ApiException(409, 'email_taken', 'Email sudah terdaftar.');
        }

        $pdo = Db::pdo();
        $pdo->beginTransaction();
        try {
            $uid = Db::insert(
                'INSERT INTO users (name, email, password_hash) VALUES (?,?,?)',
                [$name, $email, password_hash($password, PASSWORD_DEFAULT)]
            );
            Db::run('INSERT INTO mother_profiles (user_id, hpht, birth_date, health_facility) VALUES (?,?,?,?)', [$uid, $hpht, $birth, $facility]);
            Db::run('INSERT INTO ttd_settings (user_id) VALUES (?)', [$uid]);
            $pdo->commit();
        } catch (\Throwable $e) {
            $pdo->rollBack();
            throw $e;
        }
        $session = Auth::issue($uid, $device, $req->ip());
        Response::data($session + ['user' => self::userPayload($uid)], 201);
    }

    public static function login(Request $req): never
    {
        $v = new Validator($req->body);
        $email = $v->email();
        $password = $v->string('password', 1, 200);
        $device = $v->string('device_name', 1, 100, false);
        $v->check();

        $u = Db::one('SELECT id, password_hash, status FROM users WHERE email = ?', [$email]);
        if (!$u || !password_verify($password, $u['password_hash'])) {
            throw new ApiException(401, 'invalid_credentials', 'Email atau kata sandi salah.');
        }
        if ($u['status'] !== 'active') {
            throw new ApiException(403, 'account_suspended', 'Akun dinonaktifkan.');
        }
        Db::run('UPDATE users SET last_login_at = UTC_TIMESTAMP() WHERE id = ?', [$u['id']]);
        $session = Auth::issue((int)$u['id'], $device, $req->ip());
        Response::data($session + ['user' => self::userPayload((int)$u['id'])]);
    }

    public static function logout(Request $req): never
    {
        Db::run('UPDATE auth_tokens SET revoked_at = UTC_TIMESTAMP() WHERE id = ?', [$req->tokenId]);
        Response::data(['logged_out' => true]);
    }

    public static function me(Request $req): never
    {
        Response::data(self::userPayload($req->userId()));
    }

    public static function updateMe(Request $req): never
    {
        $v = new Validator($req->body);
        $name = $v->string('name', 2, 100, false);
        $email = $v->has('email') ? $v->email() : null;
        $hpht = $v->date('hpht', false, true);
        $birth = $v->date('birth_date', false, true);
        $facility = $v->string('health_facility', 1, 150, false);
        $height = $v->number('height_cm', 100, 220, false);
        $weight = $v->number('pre_pregnancy_weight_kg', 25, 200, false);
        $gravida = $v->int('gravida', 1, 20, false);
        $v->check();

        $uid = $req->userId();
        if ($hpht !== null && Domain::weeksBetween($hpht) > self::MAX_WEEKS) {
            throw new ApiException(422, 'validation_failed', 'Data yang dikirim tidak valid.', ['hpht' => 'HPHT lebih dari 42 minggu lalu.']);
        }
        if ($birth !== null) {
            self::checkBirthDate($birth);
        }

        $emailChanged = false;
        if ($email !== null) {
            $current = Db::one('SELECT email FROM users WHERE id = ?', [$uid]);
            if ($current['email'] !== $email) {
                if (Db::one('SELECT 1 FROM users WHERE email = ? AND id <> ?', [$email, $uid])) {
                    throw new ApiException(409, 'email_taken', 'Email sudah terdaftar.');
                }
                $emailChanged = true;
            }
        }

        self::patch('users', 'id', $uid, ['name' => $name, 'email' => $emailChanged ? $email : null]);
        self::patch('mother_profiles', 'user_id', $uid, [
            'hpht' => $hpht, 'birth_date' => $birth, 'health_facility' => $facility,
            'height_cm' => $height, 'pre_pregnancy_weight_kg' => $weight, 'gravida' => $gravida,
        ]);
        // health_facility dikirim kosong ("") = hapus (patch() melewati nilai null, jadi dikosongkan di sini).
        if (array_key_exists('health_facility', $req->body) && trim((string)$req->body['health_facility']) === '') {
            Db::run('UPDATE mother_profiles SET health_facility = NULL WHERE user_id = ?', [$uid]);
        }
        if ($emailChanged) {
            // Email dipakai untuk masuk dan reset kata sandi: sesi di perangkat lain dicabut.
            Db::run('UPDATE auth_tokens SET revoked_at = UTC_TIMESTAMP() WHERE user_id = ? AND id <> ? AND revoked_at IS NULL', [$uid, $req->tokenId]);
        }
        Response::data(self::userPayload($uid));
    }

    public static function changePassword(Request $req): never
    {
        $v = new Validator($req->body);
        $current = $v->string('current_password', 1, 200);
        $new = $v->password('new_password');
        $v->check();
        $row = Db::one('SELECT password_hash FROM users WHERE id = ?', [$req->userId()]);
        if (!password_verify($current, $row['password_hash'])) {
            throw new ApiException(403, 'wrong_password', 'Kata sandi saat ini salah.');
        }
        Db::run('UPDATE users SET password_hash = ? WHERE id = ?', [password_hash($new, PASSWORD_DEFAULT), $req->userId()]);
        // Cabut semua sesi lain.
        Db::run('UPDATE auth_tokens SET revoked_at = UTC_TIMESTAMP() WHERE user_id = ? AND id <> ? AND revoked_at IS NULL', [$req->userId(), $req->tokenId]);
        Response::data(['changed' => true]);
    }

    /** Hapus akun + seluruh data + berkas (hak penghapusan data pribadi). */
    public static function deleteMe(Request $req): never
    {
        $v = new Validator($req->body);
        $password = $v->string('password', 1, 200);
        $v->check();
        $uid = $req->userId();
        $row = Db::one('SELECT password_hash FROM users WHERE id = ?', [$uid]);
        if (!password_verify($password, $row['password_hash'])) {
            throw new ApiException(403, 'wrong_password', 'Kata sandi salah.');
        }
        foreach (Db::all('SELECT file_path, mask_path FROM images WHERE user_id = ?', [$uid]) as $i) {
            Storage::delete($i['file_path']);
            Storage::delete($i['mask_path']);
        }
        foreach (Db::all('SELECT raw_path FROM ppg_sessions WHERE user_id = ?', [$uid]) as $p) {
            Storage::delete($p['raw_path']);
        }
        Db::run('DELETE FROM users WHERE id = ?', [$uid]);   // sisanya ikut terhapus lewat ON DELETE CASCADE
        Response::data(['deleted' => true]);
    }

    public static function consents(Request $req): never
    {
        $rows = Db::all('SELECT type, version, accepted_at, revoked_at FROM consents WHERE user_id = ? ORDER BY accepted_at DESC', [$req->userId()]);
        Response::data(array_map(fn($r) => [
            'type' => $r['type'], 'version' => $r['version'],
            'accepted_at' => Time::iso($r['accepted_at']), 'revoked_at' => Time::iso($r['revoked_at']),
        ], $rows));
    }

    public static function setConsent(Request $req): never
    {
        $v = new Validator($req->body);
        $type = $v->enum('type', ['privacy_policy', 'health_data', 'image_upload', 'research_use']);
        $version = $v->string('version', 1, 20);
        $accepted = $v->bool('accepted', true);
        $v->check();
        $uid = $req->userId();
        if ($accepted) {
            Db::run(
                'INSERT INTO consents (user_id, type, version, accepted_at) VALUES (?,?,?,UTC_TIMESTAMP())
                 ON DUPLICATE KEY UPDATE accepted_at = UTC_TIMESTAMP(), revoked_at = NULL',
                [$uid, $type, $version]
            );
        } else {
            Db::run('UPDATE consents SET revoked_at = UTC_TIMESTAMP() WHERE user_id = ? AND type = ? AND revoked_at IS NULL', [$uid, $type]);
            if ($type === 'research_use') {
                Db::run("UPDATE images SET use_for_training = 0 WHERE user_id = ?", [$uid]);
            }
        }
        self::consents($req);
    }

    // ---------------------------------------------------------------

    public static function userPayload(int $uid): array
    {
        $u = Db::one(
            'SELECT u.id, u.name, u.email, u.role, u.created_at,
                    p.hpht, p.birth_date, p.health_facility, p.height_cm, p.pre_pregnancy_weight_kg, p.gravida
             FROM users u JOIN mother_profiles p ON p.user_id = u.id WHERE u.id = ?',
            [$uid]
        );
        $weeks = Domain::weeksBetween($u['hpht']);
        return [
            'id' => (int)$u['id'],
            'name' => $u['name'],
            'email' => $u['email'],
            'role' => $u['role'],
            'created_at' => Time::iso($u['created_at']),
            'profile' => [
                'hpht' => $u['hpht'],
                'birth_date' => $u['birth_date'],
                'health_facility' => $u['health_facility'],
                'height_cm' => $u['height_cm'] === null ? null : (float)$u['height_cm'],
                'pre_pregnancy_weight_kg' => $u['pre_pregnancy_weight_kg'] === null ? null : (float)$u['pre_pregnancy_weight_kg'],
                'gravida' => $u['gravida'] === null ? null : (int)$u['gravida'],
            ],
            'pregnancy' => [
                'week' => $weeks,
                'trimester' => Domain::trimester($weeks),
                'due_date' => Domain::dueDate($u['hpht']),
            ],
        ];
    }

    /** Usia dari tanggal lahir harus 10-60 tahun (batas yang sama dengan aplikasi). */
    private static function checkBirthDate(string $birth): void
    {
        $age = (new \DateTimeImmutable($birth))->diff(new \DateTimeImmutable(Time::today()))->y;
        if ($age < self::MIN_AGE || $age > self::MAX_AGE) {
            throw new ApiException(
                422, 'validation_failed', 'Data yang dikirim tidak valid.',
                ['birth_date' => 'Usia harus ' . self::MIN_AGE . '-' . self::MAX_AGE . ' tahun.']
            );
        }
    }

    /** UPDATE hanya kolom yang dikirim (bukan null). */
    private static function patch(string $table, string $keyCol, int $key, array $fields): void
    {
        $fields = array_filter($fields, fn($x) => $x !== null);
        if (!$fields) {
            return;
        }
        $set = implode(', ', array_map(fn($c) => "$c = ?", array_keys($fields)));
        Db::run("UPDATE $table SET $set WHERE $keyCol = ?", [...array_values($fields), $key]);
    }
}
