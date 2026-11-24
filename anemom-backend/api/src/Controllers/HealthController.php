<?php
declare(strict_types=1);

namespace Anemom\Controllers;

use Anemom\ApiException;
use Anemom\Db;
use Anemom\Domain;
use Anemom\Request;
use Anemom\Response;
use Anemom\Time;
use Anemom\Validator;

/** Hasil Hb lab, TTD, dan dashboard. */
final class HealthController
{
    // ---------- Hb ----------

    public static function hbList(Request $req): never
    {
        $uid = $req->userId();
        $sql = 'SELECT * FROM hb_results WHERE user_id = ?';
        $p = [$uid];
        $since = self::since($req);
        if ($since !== null) {
            $sql .= ' AND updated_at > ?';            // sinkronisasi: sertakan juga yang sudah dihapus
            $p[] = $since;
        } else {
            $sql .= ' AND deleted_at IS NULL';
        }
        foreach (['from' => '>=', 'to' => '<='] as $q => $op) {
            if (!empty($req->query[$q]) && preg_match('/^\d{4}-\d{2}-\d{2}$/', $req->query[$q])) {
                $sql .= " AND test_date $op ?";
                $p[] = $req->query[$q];
            }
        }
        $sql .= ' ORDER BY test_date DESC, id DESC LIMIT ' . self::limit($req);
        $rows = array_map([self::class, 'hbFmt'], Db::all($sql, $p));
        Response::data($rows, 200, ['count' => count($rows)]);
    }

    public static function hbCreate(Request $req): never
    {
        $v = new Validator($req->body);
        $value = $v->number('hb_value', 3, 20);
        $date = $v->date('test_date', true, true);
        $facility = $v->string('facility', 1, 150, false);
        $source = $v->string('source', 1, 30, false) ?? 'Laboratorium';
        $notes = $v->string('notes', 1, 255, false);
        $uuid = $v->uuid();
        $v->check();

        $uid = $req->userId();
        if ($uuid !== null && ($old = Db::one('SELECT * FROM hb_results WHERE user_id = ? AND client_uuid = ?', [$uid, $uuid]))) {
            Response::data(self::hbFmt($old), 200);          // idempoten: sudah pernah dikirim
        }
        $hpht = Domain::hphtOf($uid);
        $id = Db::insert(
            'INSERT INTO hb_results (user_id, client_uuid, test_date, hb_value, risk, facility, source, pregnancy_week, notes)
             VALUES (?,?,?,?,?,?,?,?,?)',
            [$uid, $uuid, $date, $value, Domain::classify($value), $facility, $source,
             $hpht ? Domain::weeksBetween($hpht, $date) : null, $notes]
        );
        Response::data(self::hbFmt(Db::one('SELECT * FROM hb_results WHERE id = ?', [$id])), 201);
    }

    public static function hbGet(Request $req): never
    {
        $r = Db::one('SELECT * FROM hb_results WHERE id = ? AND user_id = ? AND deleted_at IS NULL', [$req->intParam('id'), $req->userId()]);
        $r ?? throw new ApiException(404, 'not_found', 'Hasil Hb tidak ditemukan.');
        Response::data(self::hbFmt($r));
    }

    public static function hbDelete(Request $req): never
    {
        $n = Db::run('UPDATE hb_results SET deleted_at = UTC_TIMESTAMP() WHERE id = ? AND user_id = ? AND deleted_at IS NULL', [$req->intParam('id'), $req->userId()]);
        $n > 0 || throw new ApiException(404, 'not_found', 'Hasil Hb tidak ditemukan.');
        Response::data(['deleted' => true]);
    }

    public static function hbFmt(array $r): array
    {
        return [
            'id' => (int)$r['id'],
            'client_uuid' => $r['client_uuid'],
            'test_date' => $r['test_date'],
            'hb_value' => (float)$r['hb_value'],
            'risk' => $r['risk'],
            'risk_label' => Domain::riskLabel($r['risk']),
            'facility' => $r['facility'],
            'source' => $r['source'],
            'pregnancy_week' => $r['pregnancy_week'] === null ? null : (int)$r['pregnancy_week'],
            'notes' => $r['notes'],
            'updated_at' => Time::iso($r['updated_at']),
            'deleted_at' => Time::iso($r['deleted_at']),
        ];
    }

    // ---------- TTD ----------

    public static function ttdMonth(Request $req): never
    {
        $month = $req->query['month'] ?? substr(Time::today(), 0, 7);
        if (!preg_match('/^\d{4}-(0[1-9]|1[0-2])$/', $month)) {
            throw new ApiException(422, 'validation_failed', 'Parameter month harus YYYY-MM.');
        }
        $uid = $req->userId();
        $rows = Db::all(
            'SELECT taken_date, taken_time, status FROM ttd_logs WHERE user_id = ? AND taken_date BETWEEN ? AND LAST_DAY(?) ORDER BY taken_date',
            [$uid, "$month-01", "$month-01"]
        );
        $fmt = fn($r) => ['date' => $r['taken_date'], 'time' => substr($r['taken_time'], 0, 5)];
        // days = hari "Sudah" (diminum); skipped = hari yang dilaporkan "Belum diminum" dari alarm.
        // Hari tanpa catatan = "Belum dikonfirmasi" (dihitung aplikasi).
        Response::data([
            'month' => $month,
            'days' => array_values(array_map($fmt, array_filter($rows, fn($r) => $r['status'] === 'sudah'))),
            'skipped' => array_values(array_map($fmt, array_filter($rows, fn($r) => $r['status'] === 'belum'))),
        ] + self::ttdSummary($uid));
    }

    public static function ttdTake(Request $req): never
    {
        $v = new Validator($req->body);
        $date = $v->date('date', false, true) ?? Time::today();
        $time = $v->time('time') ?? (new \DateTimeImmutable('now', Time::tz()))->format('H:i:s');
        $v->check();
        $uid = $req->userId();
        // Laporan "Belum" pada hari yang sama diganti "Sudah"; catatan "Sudah" yang ada tidak diubah.
        $created = Db::run(
            "INSERT INTO ttd_logs (user_id, taken_date, taken_time, status) VALUES (?,?,?,'sudah')
             ON DUPLICATE KEY UPDATE taken_time = IF(status = 'belum', VALUES(taken_time), taken_time), status = 'sudah'",
            [$uid, $date, $time]
        ) > 0;
        $row = Db::one('SELECT taken_date, taken_time FROM ttd_logs WHERE user_id = ? AND taken_date = ?', [$uid, $date]);
        Response::data(
            ['date' => $row['taken_date'], 'time' => substr($row['taken_time'], 0, 5), 'created' => $created] + self::ttdSummary($uid),
            $created ? 201 : 200
        );
    }

    /**
     * Laporan "Belum diminum" dari alarm. Tidak menambah kepatuhan; tidak menimpa catatan "Sudah".
     * Body opsional: date (YYYY-MM-DD), time (HH:mm). Default: sekarang.
     */
    public static function ttdSkip(Request $req): never
    {
        $v = new Validator($req->body);
        $date = $v->date('date', false, true) ?? Time::today();
        $time = $v->time('time') ?? (new \DateTimeImmutable('now', Time::tz()))->format('H:i:s');
        $v->check();
        $uid = $req->userId();
        Db::run("INSERT IGNORE INTO ttd_logs (user_id, taken_date, taken_time, status) VALUES (?,?,?,'belum')", [$uid, $date, $time]);
        $row = Db::one('SELECT taken_date, taken_time, status FROM ttd_logs WHERE user_id = ? AND taken_date = ?', [$uid, $date]);
        Response::data(
            ['date' => $row['taken_date'], 'time' => substr($row['taken_time'], 0, 5), 'status' => $row['status']] + self::ttdSummary($uid)
        );
    }

    public static function ttdDelete(Request $req): never
    {
        $n = Db::run('DELETE FROM ttd_logs WHERE user_id = ? AND taken_date = ?', [$req->userId(), $req->param('date')]);
        $n > 0 || throw new ApiException(404, 'not_found', 'Catatan TTD tidak ditemukan.');
        Response::data(['deleted' => true]);
    }

    public static function ttdSettings(Request $req): never
    {
        Response::data(self::settingsFmt($req->userId()));
    }

    public static function ttdSettingsUpdate(Request $req): never
    {
        $v = new Validator($req->body);
        $on = $v->bool('reminder_on');
        $time = $v->time('reminder_time');
        $v->check();
        $uid = $req->userId();
        if ($on === false && self::reminderLocked($uid)) {
            throw new ApiException(
                422, 'reminder_locked',
                'Hb terakhir Bunda di bawah normal, jadi pengingat TTD wajib aktif.',
                ['reminder_on' => 'Pengingat TTD tidak bisa dimatikan selama Hb terakhir di bawah normal.']
            );
        }
        if ($on !== null) {
            Db::run('UPDATE ttd_settings SET reminder_on = ? WHERE user_id = ?', [$on ? 1 : 0, $uid]);
        }
        if ($time !== null) {
            Db::run('UPDATE ttd_settings SET reminder_time = ? WHERE user_id = ?', [$time, $uid]);
        }
        Response::data(self::settingsFmt($uid));
    }

    private static function settingsFmt(int $uid): array
    {
        $r = Db::one('SELECT reminder_on, reminder_time FROM ttd_settings WHERE user_id = ?', [$uid])
            ?? ['reminder_on' => 1, 'reminder_time' => '19:00:00'];
        $locked = self::reminderLocked($uid);
        // Hb turun saat pengingat mati -> pengingat dinyalakan lagi (wajib).
        if ($locked && (int)$r['reminder_on'] === 0) {
            Db::run('UPDATE ttd_settings SET reminder_on = 1 WHERE user_id = ?', [$uid]);
            $r['reminder_on'] = 1;
        }
        return [
            'reminder_on' => (bool)$r['reminder_on'],
            'reminder_time' => substr($r['reminder_time'], 0, 5),
            // false = Hb lab terakhir di bawah normal: pengingat wajib aktif.
            'can_disable' => !$locked,
        ];
    }

    /** Pengingat TTD wajib aktif bila hasil Hb lab terakhir bukan 'normal' (risiko sedang/tinggi). */
    public static function reminderLocked(int $uid): bool
    {
        $hb = Db::one(
            'SELECT risk FROM hb_results WHERE user_id = ? AND deleted_at IS NULL ORDER BY test_date DESC, id DESC LIMIT 1',
            [$uid]
        );
        return $hb !== null && $hb['risk'] !== 'normal';
    }

    /** Kepatuhan 7 hari terakhir (hari ini + 6 hari sebelumnya), sama dengan aplikasi. */
    public static function ttdSummary(int $uid): array
    {
        $today = Time::today();
        $from = (new \DateTimeImmutable($today))->modify('-6 days')->format('Y-m-d');
        $n = (int)Db::one(
            "SELECT COUNT(*) c FROM ttd_logs WHERE user_id = ? AND status = 'sudah' AND taken_date BETWEEN ? AND ?",
            [$uid, $from, $today]
        )['c'];
        $takenToday = Db::one("SELECT 1 FROM ttd_logs WHERE user_id = ? AND taken_date = ? AND status = 'sudah'", [$uid, $today]) !== null;
        return ['taken_today' => $takenToday, 'taken_last_7_days' => $n, 'percent_last_7_days' => (int)round($n * 100 / 7)];
    }

    // ---------- Dashboard ----------

    public static function dashboard(Request $req): never
    {
        $uid = $req->userId();
        $user = AccountController::userPayload($uid);

        $trend = array_reverse(Db::all(
            'SELECT * FROM hb_results WHERE user_id = ? AND deleted_at IS NULL ORDER BY test_date DESC, id DESC LIMIT 5',
            [$uid]
        ));
        $last = $trend ? self::hbFmt($trend[count($trend) - 1]) : null;
        $note = null;
        if (count($trend) >= 2) {
            $diff = (float)$trend[count($trend) - 1]['hb_value'] - (float)$trend[count($trend) - 2]['hb_value'];
            $note = abs($diff) < 0.05 ? 'stabil' : ($diff > 0 ? 'naik' : 'turun');
        }
        $screenings = array_map(
            [ScreeningController::class, 'fmt'],
            Db::all('SELECT * FROM screenings WHERE user_id = ? AND deleted_at IS NULL ORDER BY measured_at DESC LIMIT 3', [$uid])
        );
        Response::data([
            'mother_name' => $user['name'],
            'today' => Time::today(),
            'pregnancy' => $user['pregnancy'],
            'last_hb' => $last,
            'hb_trend' => array_map(fn($r) => ['date' => $r['test_date'], 'hb_value' => (float)$r['hb_value']], $trend),
            'hb_trend_direction' => $note,
            'ttd' => self::ttdSummary($uid) + self::settingsFmt($uid),
            'recent_screenings' => $screenings,
        ]);
    }

    // ---------- util ----------

    public static function limit(Request $req, int $default = 100, int $max = 500): int
    {
        return max(1, min($max, (int)($req->query['limit'] ?? $default)));
    }

    /** Parameter updated_since (ISO 8601) -> UTC, atau null. */
    public static function since(Request $req): ?string
    {
        if (empty($req->query['updated_since'])) {
            return null;
        }
        $utc = Time::parseToUtc((string)$req->query['updated_since']);
        $utc ?? throw new ApiException(422, 'validation_failed', 'updated_since harus ISO 8601.');
        return $utc;
    }
}
