<?php
declare(strict_types=1);

namespace Anemom\Controllers;

use Anemom\ApiException;
use Anemom\Db;
use Anemom\Domain;
use Anemom\Env;
use Anemom\Inference;
use Anemom\Request;
use Anemom\Response;
use Anemom\Time;
use Anemom\Validator;

final class SystemController
{
    public static function health(Request $req): never
    {
        $db = true;
        try {
            Db::one('SELECT 1');
        } catch (\Throwable) {
            $db = false;
        }
        Response::data(['status' => $db ? 'ok' : 'degraded', 'database' => $db, 'time' => Time::iso(Time::now())], $db ? 200 : 503);
    }

    /** Konfigurasi yang boleh diketahui aplikasi tanpa login: kategori Hb dan model AI aktif. */
    public static function config(Request $req): never
    {
        $models = Db::all('SELECT name, task, architecture, version, runtime FROM ai_models WHERE is_active = 1');
        Response::data([
            'hb_reference' => 11.0,
            'hb_risk_levels' => array_map(
                fn($l) => ['code' => $l['code'], 'label' => $l['label'], 'min_hb' => (float)$l['min_hb']],
                Domain::levels()
            ),
            'active_models' => $models,
            'server_inference_available' => Inference::configured(),
            'max_upload_mb' => Env::int('MAX_UPLOAD_MB', 8),
        ]);
    }

    public static function fcmRegister(Request $req): never
    {
        $v = new Validator($req->body);
        $token = $v->string('token', 20, 255);
        $name = $v->string('device_name', 1, 100, false);
        $v->check();
        Db::run(
            'INSERT INTO fcm_tokens (user_id, token, device_name) VALUES (?,?,?)
             ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), device_name = VALUES(device_name), updated_at = UTC_TIMESTAMP()',
            [$req->userId(), $token, $name]
        );
        Response::data(['registered' => true], 201);
    }

    public static function fcmRemove(Request $req): never
    {
        $v = new Validator($req->body);
        $token = $v->string('token', 20, 255);
        $v->check();
        Db::run('DELETE FROM fcm_tokens WHERE token = ? AND user_id = ?', [$token, $req->userId()]);
        Response::data(['removed' => true]);
    }

    public static function notifications(Request $req): never
    {
        $rows = Db::all(
            'SELECT id, type, title, body, scheduled_at, sent_at, read_at FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ' . HealthController::limit($req, 50),
            [$req->userId()]
        );
        Response::data(array_map(fn($r) => [
            'id' => (int)$r['id'], 'type' => $r['type'], 'title' => $r['title'], 'body' => $r['body'],
            'scheduled_at' => Time::iso($r['scheduled_at']), 'sent_at' => Time::iso($r['sent_at']), 'read_at' => Time::iso($r['read_at']),
        ], $rows));
    }

    public static function notificationRead(Request $req): never
    {
        $n = Db::run('UPDATE notifications SET read_at = UTC_TIMESTAMP() WHERE id = ? AND user_id = ? AND read_at IS NULL', [$req->intParam('id'), $req->userId()]);
        $n > 0 || throw new ApiException(404, 'not_found', 'Notifikasi tidak ditemukan atau sudah dibaca.');
        Response::data(['read' => true]);
    }
}
