<?php
declare(strict_types=1);

namespace Anemom\Controllers;

use Anemom\ApiException;
use Anemom\Db;
use Anemom\Domain;
use Anemom\Images;
use Anemom\Inference;
use Anemom\InferenceRejected;
use Anemom\Request;
use Anemom\Response;
use Anemom\Storage;
use Anemom\Time;
use Anemom\Validator;

/** Skrining AI Mata + PPG Band, unggahan gambar, dan perangkat band. */
final class ScreeningController
{
    /** Kode segmenter dari aplikasi -> pola ai_models.architecture dan runtime server-nya. */
    private const SEG_ARCH = ['unet' => 'unet%', 'yolo' => 'yolo%'];
    private const SEG_SERVER_RUNTIME = ['unet' => 'unet_server', 'yolo' => 'yolov8_server'];

    // ---------- Gambar ----------

    public static function imageUpload(Request $req): never
    {
        $v = new Validator($req->body);
        $purpose = $v->enum('purpose', ['eye', 'food']);
        $captured = $v->datetime('captured_at', false);
        $uuid = $v->uuid();
        $train = $v->bool('use_for_training', false);
        $v->check();
        if ($uuid !== null && ($old = Db::one('SELECT * FROM images WHERE user_id = ? AND client_uuid = ?', [$req->userId(), $uuid]))) {
            Response::data(Images::fmt($old));
        }
        Response::data(Images::fmt(Images::store($req, 'file', $purpose, $captured, $uuid, (bool)$train)), 201);
    }

    public static function imageFile(Request $req): never
    {
        $r = Db::one('SELECT * FROM images WHERE id = ? AND user_id = ? AND deleted_at IS NULL', [$req->intParam('id'), $req->userId()]);
        $r ?? throw new ApiException(404, 'not_found', 'Gambar tidak ditemukan.');
        $path = Storage::abs($r['file_path']);
        if (!is_file($path)) {
            throw new ApiException(404, 'not_found', 'Berkas gambar hilang dari penyimpanan.');
        }
        header('Content-Type: ' . $r['mime_type']);
        header('Content-Length: ' . filesize($path));
        header('Cache-Control: private, max-age=86400');
        readfile($path);
        exit;
    }

    public static function imageDelete(Request $req): never
    {
        $r = Db::one('SELECT * FROM images WHERE id = ? AND user_id = ? AND deleted_at IS NULL', [$req->intParam('id'), $req->userId()]);
        $r ?? throw new ApiException(404, 'not_found', 'Gambar tidak ditemukan.');
        Storage::delete($r['file_path']);
        Storage::delete($r['mask_path']);
        Db::run('UPDATE images SET deleted_at = UTC_TIMESTAMP() WHERE id = ?', [$r['id']]);
        Response::data(['deleted' => true]);
    }

    // ---------- Skrining ----------

    public static function list(Request $req): never
    {
        $sql = 'SELECT * FROM screenings WHERE user_id = ?';
        $p = [$req->userId()];
        $since = HealthController::since($req);
        if ($since !== null) {
            $sql .= ' AND updated_at > ?';
            $p[] = $since;
        } else {
            $sql .= ' AND deleted_at IS NULL';
        }
        if (!empty($req->query['method']) && in_array($req->query['method'], ['ai_eye', 'ppg_band'], true)) {
            $sql .= ' AND method = ?';
            $p[] = $req->query['method'];
        }
        $sql .= ' ORDER BY measured_at DESC, id DESC LIMIT ' . HealthController::limit($req, 50);
        $rows = array_map([self::class, 'fmt'], Db::all($sql, $p));
        Response::data($rows, 200, ['count' => count($rows)]);
    }

    public static function get(Request $req): never
    {
        Response::data(self::fmt(self::owned($req)));
    }

    public static function delete(Request $req): never
    {
        $r = self::owned($req);
        Db::run('UPDATE screenings SET deleted_at = UTC_TIMESTAMP() WHERE id = ?', [$r['id']]);
        Response::data(['deleted' => true]);
    }

    /** Tautkan skrining ke hasil lab (untuk mengukur akurasi model dan sebagai label pelatihan). */
    public static function linkHb(Request $req): never
    {
        $v = new Validator($req->body);
        $hbId = $v->int('hb_result_id', 1, PHP_INT_MAX);
        $v->check();
        $r = self::owned($req);
        $hb = Db::one('SELECT id FROM hb_results WHERE id = ? AND user_id = ? AND deleted_at IS NULL', [$hbId, $req->userId()]);
        $hb ?? throw new ApiException(404, 'not_found', 'Hasil Hb tidak ditemukan.');
        Db::run('UPDATE screenings SET matched_hb_result_id = ? WHERE id = ?', [$hbId, $r['id']]);
        Response::data(self::fmt(Db::one('SELECT * FROM screenings WHERE id = ?', [$r['id']])));
    }

    /**
     * AI Scan Mata (multipart: image, captured_at, client_uuid, [segmenter], [on_device_result], [use_for_training]).
     *  - Jika on_device_result dikirim: model di HP yang menghitung, server hanya menyimpan.
     *  - Jika tidak: layanan AI mata di VPS dijalankan (segmentasi U-Net/YOLO + klasifikasi).
     *  - segmenter (unet|yolo): model segmentasi yang dipakai; dicatat di screenings.seg_model_id.
     *  - Foto ditolak layanan AI (konjungtiva tidak terlihat) -> 422 poor_quality, tidak ada yang tersimpan.
     */
    public static function createEye(Request $req): never
    {
        $v = new Validator($req->body);
        $captured = $v->datetime('captured_at', false);
        $uuid = $v->uuid();
        $onDevice = $v->object('on_device_result');
        $train = (bool)$v->bool('use_for_training', false);
        $segmenter = $v->enum('segmenter', array_keys(self::SEG_ARCH), false);
        $v->check();

        $uid = $req->userId();
        if ($uuid !== null && ($old = Db::one('SELECT * FROM screenings WHERE user_id = ? AND client_uuid = ?', [$uid, $uuid]))) {
            Response::data(self::fmt($old));
        }
        $rv = null;
        if ($onDevice !== null) {
            $rv = new Validator($onDevice);
            $rv->enum('label', ['anemia', 'non_anemia']);
            $rv->number('confidence', 0, 1, true, 3);
            $rv->number('est_hb', 3, 20, false);
            $rv->string('model_version', 1, 20, false);
            $rv->enum('segmenter', array_keys(self::SEG_ARCH), false);
            $rv->string('seg_model_version', 1, 20, false);
            try {
                $rv->check();
            } catch (ApiException $e) {
                throw new ApiException(422, 'validation_failed', 'on_device_result tidak valid.', $e->details);
            }
        }

        $image = Images::store($req, 'image', 'eye', $captured, $uuid, $train);
        $measured = $captured ?? Time::now();
        $weeks = ($h = Domain::hphtOf($uid)) ? Domain::weeksBetween($h, substr($measured, 0, 10)) : null;

        if ($onDevice !== null) {
            $estHb = isset($onDevice['est_hb']) ? round((float)$onDevice['est_hb'], 1) : null;
            $deviceSeg = $onDevice['segmenter'] ?? $segmenter;
            $id = Db::insert(
                'INSERT INTO screenings (user_id, client_uuid, method, measured_at, pregnancy_week, label, est_hb, risk, confidence, image_id, model_id, seg_model_id, inference_source)
                 VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)',
                [$uid, $uuid, 'ai_eye', $measured, $weeks, $onDevice['label'], $estHb,
                 $estHb === null ? null : Domain::classify($estHb), round((float)$onDevice['confidence'], 3),
                 $image['id'], self::modelId('eye_anemia_cls', $onDevice['model_version'] ?? null),
                 $deviceSeg === null ? null : self::segModelId($deviceSeg, true, $onDevice['seg_model_version'] ?? null),
                 'on_device']
            );
            Response::data(self::fmt(Db::one('SELECT * FROM screenings WHERE id = ?', [$id])), 201);
        }

        // Inferensi di VPS. Model klasifikasi aktif; model segmentasi sesuai permintaan aplikasi
        // (kosong = bawaan layanan AI, dicatat setelah layanan menjawab).
        $model = Db::one("SELECT id FROM ai_models WHERE task = 'eye_anemia_cls' AND runtime = 'sklearn_server' ORDER BY is_active DESC, id DESC LIMIT 1");
        $segModelId = $segmenter === null ? null : self::segModelId($segmenter, false, null);
        $jobId = Db::insert('INSERT INTO inference_jobs (user_id, image_id, model_id) VALUES (?,?,?)', [$uid, $image['id'], $model['id'] ?? null]);
        $id = Db::insert(
            'INSERT INTO screenings (user_id, client_uuid, method, measured_at, pregnancy_week, image_id, model_id, seg_model_id, inference_source, inference_job_id)
             VALUES (?,?,?,?,?,?,?,?,?,?)',
            [$uid, $uuid, 'ai_eye', $measured, $weeks, $image['id'], $model['id'] ?? null, $segModelId, 'server', $jobId]
        );
        try {
            self::runJob($jobId, true);
        } catch (InferenceRejected $e) {
            // Foto tidak layak: hapus semuanya supaya tidak muncul di riwayat, lalu minta foto ulang.
            Db::run('DELETE FROM screenings WHERE id = ?', [$id]);
            Db::run('DELETE FROM inference_jobs WHERE id = ?', [$jobId]);
            Db::run('DELETE FROM images WHERE id = ?', [$image['id']]);
            Storage::delete($image['file_path']);
            throw new ApiException(422, $e->errorCode, $e->getMessage(), $e->details);
        }
        $row = Db::one('SELECT * FROM screenings WHERE id = ?', [$id]);
        $out = self::fmt($row);
        Response::data($out, $out['status'] === 'done' ? 201 : 202);
    }

    /** Skrining PPG dari band (JSON atau multipart; raw = berkas sinyal mentah opsional). */
    public static function createPpg(Request $req): never
    {
        $v = new Validator($req->body);
        $uuid = $v->uuid();
        $deviceUid = $v->string('device_uid', 1, 64, false);
        $started = $v->datetime('started_at');
        $duration = $v->int('duration_s', 1, 600);
        $rate = $v->int('sample_rate_hz', 10, 1000);
        $samples = $v->int('sample_count', 1, 10000000, false);
        $hr = $v->int('hr_bpm', 20, 250, false);
        $spo2 = $v->number('spo2_pct', 50, 100, false);
        $quality = $v->enum('signal_quality', ['buruk', 'cukup', 'baik'], false);
        $qscore = $v->number('quality_score', 0, 1, false, 3);
        $features = $v->object('features');
        $result = $v->object('result');
        $v->check();

        $uid = $req->userId();
        if ($uuid !== null && ($old = Db::one('SELECT * FROM screenings WHERE user_id = ? AND client_uuid = ?', [$uid, $uuid]))) {
            Response::data(self::fmt($old));
        }
        $rv = new Validator($result ?? []);
        $label = $rv->enum('label', ['anemia', 'non_anemia'], false);
        $estHb = $rv->number('est_hb', 3, 20, false);
        $conf = $rv->number('confidence', 0, 1, false, 3);
        $modelVer = $rv->string('model_version', 1, 20, false);
        $source = $rv->enum('inference_source', ['on_device', 'server'], false);
        $rv->check();

        $deviceId = null;
        if ($deviceUid !== null) {
            $d = Db::one('SELECT id FROM devices WHERE user_id = ? AND device_uid = ?', [$uid, $deviceUid]);
            $deviceId = $d['id'] ?? null;
        }
        $rawPath = null;
        $f = $req->files['raw'] ?? null;
        if ($f && $f['error'] === UPLOAD_ERR_OK) {
            if ($f['size'] > \Anemom\Env::int('MAX_UPLOAD_MB', 8) * 1048576) {
                throw new ApiException(413, 'file_too_large', 'Berkas PPG terlalu besar.');
            }
            $ext = strtolower(pathinfo($f['name'], PATHINFO_EXTENSION));
            $rawPath = Storage::put("ppg/$uid/" . gmdate('Y'), in_array($ext, ['csv', 'gz', 'bin', 'txt'], true) ? $ext : 'bin', $f['tmp_name']);
        }
        $weeks = ($h = Domain::hphtOf($uid)) ? Domain::weeksBetween($h, substr($started, 0, 10)) : null;

        $pdo = Db::pdo();
        $pdo->beginTransaction();
        try {
            $ppgId = Db::insert(
                'INSERT INTO ppg_sessions (user_id, device_id, client_uuid, started_at, duration_s, sample_rate_hz, sample_count, raw_path, hr_bpm, spo2_pct, signal_quality, quality_score, features)
                 VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)',
                [$uid, $deviceId, $uuid, $started, $duration, $rate, $samples, $rawPath, $hr, $spo2, $quality, $qscore,
                 $features === null ? null : json_encode($features)]
            );
            $id = Db::insert(
                'INSERT INTO screenings (user_id, client_uuid, method, measured_at, pregnancy_week, label, est_hb, risk, confidence, ppg_session_id, model_id, inference_source)
                 VALUES (?,?,?,?,?,?,?,?,?,?,?,?)',
                [$uid, $uuid, 'ppg_band', $started, $weeks, $label, $estHb, $estHb === null ? null : Domain::classify($estHb),
                 $conf, $ppgId, self::modelId('ppg_hb', $modelVer), $result === null ? null : ($source ?? 'on_device')]
            );
            if ($deviceId !== null) {
                Db::run('UPDATE devices SET last_seen_at = UTC_TIMESTAMP() WHERE id = ?', [$deviceId]);
            }
            $pdo->commit();
        } catch (\Throwable $e) {
            $pdo->rollBack();
            Storage::delete($rawPath);
            throw $e;
        }
        Response::data(self::fmt(Db::one('SELECT * FROM screenings WHERE id = ?', [$id])), 201);
    }

    /**
     * Jalankan satu job inferensi mata; dipakai request langsung dan bin/process_jobs.php.
     * Foto yang ditolak (poor_quality) langsung ditandai failed tanpa diulang; bila [$rethrowRejected]
     * true, InferenceRejected diteruskan ke pemanggil (createEye) agar aplikasi diminta memotret ulang.
     */
    public static function runJob(int $jobId, bool $rethrowRejected = false): bool
    {
        $job = Db::one(
            'SELECT j.*, i.file_path, i.mime_type, sm.runtime AS seg_runtime
             FROM inference_jobs j
             JOIN images i ON i.id = j.image_id
             LEFT JOIN screenings s ON s.inference_job_id = j.id
             LEFT JOIN ai_models sm ON sm.id = s.seg_model_id
             WHERE j.id = ?',
            [$jobId]
        );
        if (!$job || !in_array($job['status'], ['queued', 'processing'], true)) {
            return false;
        }
        Db::run("UPDATE inference_jobs SET status = 'processing', attempts = attempts + 1 WHERE id = ?", [$jobId]);
        $t0 = microtime(true);
        try {
            $segmenter = array_search($job['seg_runtime'], self::SEG_SERVER_RUNTIME, true);
            $r = Inference::predictEye(Storage::abs($job['file_path']), $job['mime_type'], $segmenter === false ? null : $segmenter);
            $estHb = isset($r['est_hb']) && is_numeric($r['est_hb']) ? round((float)$r['est_hb'], 1) : null;
            Db::run(
                "UPDATE inference_jobs SET status = 'done', result = ?, latency_ms = ?, finished_at = UTC_TIMESTAMP(), error_message = NULL WHERE id = ?",
                [json_encode($r), (int)((microtime(true) - $t0) * 1000), $jobId]
            );
            $clsId = self::modelIdByName($r['model_name'] ?? null, $r['model_version'] ?? null)
                ?? self::modelId('eye_anemia_cls', $r['model_version'] ?? null);
            $segId = self::modelIdByName($r['seg_model_name'] ?? null, $r['seg_model_version'] ?? null);
            Db::run(
                'UPDATE screenings SET label = ?, confidence = ?, est_hb = ?, risk = ?, model_id = COALESCE(?, model_id),
                        seg_model_id = COALESCE(?, seg_model_id) WHERE inference_job_id = ?',
                [in_array($r['label'], ['anemia', 'non_anemia'], true) ? $r['label'] : null, round((float)$r['confidence'], 3), $estHb,
                 $estHb === null ? null : Domain::classify($estHb), $clsId, $segId, $jobId]
            );
            return true;
        } catch (InferenceRejected $e) {
            Db::run(
                "UPDATE inference_jobs SET status = 'failed', error_message = ?, finished_at = UTC_TIMESTAMP() WHERE id = ?",
                [mb_substr($e->errorCode . ': ' . $e->getMessage(), 0, 250), $jobId]
            );
            if ($rethrowRejected) {
                throw $e;
            }
            return false;
        } catch (\Throwable $e) {
            $failed = (int)$job['attempts'] + 1 >= 3;
            Db::run(
                'UPDATE inference_jobs SET status = ?, error_message = ?, finished_at = IF(?, UTC_TIMESTAMP(), NULL) WHERE id = ?',
                [$failed ? 'failed' : 'queued', mb_substr($e->getMessage(), 0, 250), $failed ? 1 : 0, $jobId]
            );
            return false;
        }
    }

    // ---------- Perangkat band ----------

    public static function deviceList(Request $req): never
    {
        Response::data(array_map([self::class, 'deviceFmt'], Db::all('SELECT * FROM devices WHERE user_id = ? ORDER BY paired_at DESC', [$req->userId()])));
    }

    public static function deviceRegister(Request $req): never
    {
        $v = new Validator($req->body);
        $uid = $v->string('device_uid', 3, 64);
        $name = $v->string('name', 1, 100, false);
        $fw = $v->string('firmware_version', 1, 20, false);
        $v->check();
        $u = $req->userId();
        $existing = Db::one('SELECT id FROM devices WHERE user_id = ? AND device_uid = ?', [$u, $uid]);
        if ($existing) {
            Db::run('UPDATE devices SET name = COALESCE(?, name), firmware_version = COALESCE(?, firmware_version), last_seen_at = UTC_TIMESTAMP() WHERE id = ?', [$name, $fw, $existing['id']]);
            $id = (int)$existing['id'];
            $status = 200;
        } else {
            $id = Db::insert('INSERT INTO devices (user_id, device_uid, name, firmware_version, paired_at, last_seen_at) VALUES (?,?,?,?,UTC_TIMESTAMP(),UTC_TIMESTAMP())', [$u, $uid, $name ?? 'Anemom Band', $fw]);
            $status = 201;
        }
        Response::data(self::deviceFmt(Db::one('SELECT * FROM devices WHERE id = ?', [$id])), $status);
    }

    public static function deviceStatus(Request $req): never
    {
        $v = new Validator($req->body);
        $battery = $v->int('battery_pct', 0, 100, false);
        $fw = $v->string('firmware_version', 1, 20, false);
        $v->check();
        $d = Db::one('SELECT id FROM devices WHERE id = ? AND user_id = ?', [$req->intParam('id'), $req->userId()]);
        $d ?? throw new ApiException(404, 'not_found', 'Perangkat tidak ditemukan.');
        Db::run('UPDATE devices SET battery_pct = COALESCE(?, battery_pct), firmware_version = COALESCE(?, firmware_version), last_seen_at = UTC_TIMESTAMP() WHERE id = ?', [$battery, $fw, $d['id']]);
        Response::data(self::deviceFmt(Db::one('SELECT * FROM devices WHERE id = ?', [$d['id']])));
    }

    public static function deviceDelete(Request $req): never
    {
        $n = Db::run('DELETE FROM devices WHERE id = ? AND user_id = ?', [$req->intParam('id'), $req->userId()]);
        $n > 0 || throw new ApiException(404, 'not_found', 'Perangkat tidak ditemukan.');
        Response::data(['deleted' => true]);
    }

    // ---------- format ----------

    public static function fmt(array $r): array
    {
        $status = 'done';
        $jobResult = null;
        if ($r['inference_job_id'] !== null) {
            $job = Db::one('SELECT status, result FROM inference_jobs WHERE id = ?', [$r['inference_job_id']]);
            $status = $job['status'] ?? 'done';
            $jobResult = isset($job['result']) ? json_decode((string)$job['result'], true) : null;
        }
        // Skor mentah = peluang kelas anemia menurut model (0..1). Ambang = batas skrining untuk `label`.
        // Hasil lama tanpa p_anemia: skor diturunkan dari label + confidence, ambang tidak diketahui.
        $score = is_array($jobResult) && is_numeric($jobResult['p_anemia'] ?? null) ? (float)$jobResult['p_anemia'] : null;
        if ($score === null && $r['confidence'] !== null && $r['label'] !== null) {
            $score = $r['label'] === 'anemia' ? (float)$r['confidence'] : 1.0 - (float)$r['confidence'];
        }
        $threshold = is_array($jobResult) && is_numeric($jobResult['threshold'] ?? null) ? (float)$jobResult['threshold'] : null;
        $model = $r['model_id'] ? Db::one('SELECT name, version FROM ai_models WHERE id = ?', [$r['model_id']]) : null;
        $segModel = ($r['seg_model_id'] ?? null) ? Db::one('SELECT name, version, architecture FROM ai_models WHERE id = ?', [$r['seg_model_id']]) : null;
        return [
            'id' => (int)$r['id'],
            'client_uuid' => $r['client_uuid'],
            'method' => $r['method'],
            'status' => $status,                                   // done | queued | processing | failed
            'measured_at' => Time::iso($r['measured_at']),
            'pregnancy_week' => $r['pregnancy_week'] === null ? null : (int)$r['pregnancy_week'],
            'label' => $r['label'],
            'est_hb' => $r['est_hb'] === null ? null : (float)$r['est_hb'],
            'risk' => $r['risk'],
            'risk_label' => Domain::riskLabel($r['risk']),
            'confidence' => $r['confidence'] === null ? null : (float)$r['confidence'],
            'score' => $score === null ? null : round($score, 3),             // peluang anemia menurut model
            'score_threshold' => $threshold === null ? null : round($threshold, 4), // batas skrining
            'research_only' => is_array($jobResult) ? (bool)($jobResult['research_only'] ?? true) : null,
            'image_id' => $r['image_id'] === null ? null : (int)$r['image_id'],
            'ppg_session_id' => $r['ppg_session_id'] === null ? null : (int)$r['ppg_session_id'],
            'inference_source' => $r['inference_source'],
            'model' => $model,                                     // model klasifikasi
            'seg_model' => $segModel,                              // model segmentasi (U-Net / YOLO); null untuk PPG
            'matched_hb_result_id' => $r['matched_hb_result_id'] === null ? null : (int)$r['matched_hb_result_id'],
            'updated_at' => Time::iso($r['updated_at']),
            'deleted_at' => Time::iso($r['deleted_at']),
        ];
    }

    private static function deviceFmt(array $d): array
    {
        return [
            'id' => (int)$d['id'], 'device_uid' => $d['device_uid'], 'name' => $d['name'], 'model' => $d['model'],
            'firmware_version' => $d['firmware_version'],
            'battery_pct' => $d['battery_pct'] === null ? null : (int)$d['battery_pct'],
            'paired_at' => Time::iso($d['paired_at']), 'last_seen_at' => Time::iso($d['last_seen_at']),
        ];
    }

    private static function owned(Request $req): array
    {
        $r = Db::one('SELECT * FROM screenings WHERE id = ? AND user_id = ? AND deleted_at IS NULL', [$req->intParam('id'), $req->userId()]);
        return $r ?? throw new ApiException(404, 'not_found', 'Skrining tidak ditemukan.');
    }

    /** Model segmentasi untuk kode segmenter dari aplikasi, di HP (tflite) atau di server. */
    private static function segModelId(string $segmenter, bool $onDevice, ?string $version): ?int
    {
        if (!isset(self::SEG_ARCH[$segmenter])) {
            return null;
        }
        $sql = "SELECT id FROM ai_models WHERE task = 'eye_segmentation' AND runtime = ? AND architecture LIKE ?";
        $p = [$onDevice ? 'tflite' : self::SEG_SERVER_RUNTIME[$segmenter], self::SEG_ARCH[$segmenter]];
        if ($version !== null) {
            $sql .= ' AND version = ?';
            $p[] = $version;
        }
        $m = Db::one($sql . ' ORDER BY is_active DESC, id DESC LIMIT 1', $p);
        return $m ? (int)$m['id'] : null;
    }

    /** ai_models.id dari pasangan (name, version) yang dilaporkan layanan AI. */
    private static function modelIdByName(?string $name, ?string $version): ?int
    {
        if ($name === null || $version === null) {
            return null;
        }
        $m = Db::one('SELECT id FROM ai_models WHERE name = ? AND version = ?', [$name, $version]);
        return $m ? (int)$m['id'] : null;
    }

    private static function modelId(string $task, ?string $version): ?int
    {
        if ($version === null) {
            return null;
        }
        $m = Db::one('SELECT id FROM ai_models WHERE task = ? AND version = ? ORDER BY is_active DESC LIMIT 1', [$task, $version]);
        return $m ? (int)$m['id'] : null;
    }
}
