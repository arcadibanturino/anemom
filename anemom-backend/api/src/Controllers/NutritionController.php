<?php
declare(strict_types=1);

namespace Anemom\Controllers;

use Anemom\ApiException;
use Anemom\Db;
use Anemom\Request;
use Anemom\Response;
use Anemom\Time;
use Anemom\Validator;

/** Katalog bahan makanan, rekomendasi, catatan makan (termasuk hasil scan makanan), dan artikel panduan. */
final class NutritionController
{
    public static function ingredients(Request $req): never
    {
        $sql = 'SELECT * FROM ingredients WHERE 1=1';
        $p = [];
        if (!empty($req->query['q'])) {
            $sql .= ' AND name LIKE ?';
            $p[] = '%' . str_replace(['%', '_'], ['\\%', '\\_'], (string)$req->query['q']) . '%';
        }
        if (!empty($req->query['category'])) {
            $sql .= ' AND category = ?';
            $p[] = $req->query['category'];
        }
        $sql .= ' ORDER BY is_iron_booster DESC, iron_mg DESC, name LIMIT ' . HealthController::limit($req, 50, 200);
        $rows = array_map([self::class, 'ingredientFmt'], Db::all($sql, $p));
        Response::data($rows, 200, ['count' => count($rows)]);
    }

    public static function ingredient(Request $req): never
    {
        $r = Db::one('SELECT * FROM ingredients WHERE id = ?', [$req->intParam('id')]);
        $r ?? throw new ApiException(404, 'not_found', 'Bahan makanan tidak ditemukan.');
        Response::data(self::ingredientFmt($r));
    }

    /**
     * Rekomendasi bahan makanan untuk Beranda dan layar "Lihat Semua".
     * Hanya bahan dengan is_recommended = 1. Kosong sampai kelompok mengisi data berdasarkan rujukan gizi.
     * Bahan dengan pregnancy_warning tetap dikirim (app yang memutuskan ditampilkan sebagai peringatan).
     * Bahan yang dicentang ibu sebagai alergi (user_allergies) TIDAK dikirim.
     */
    public static function recommendations(Request $req): never
    {
        $rows = Db::all(
            'SELECT i.* FROM ingredients i
             WHERE i.is_recommended = 1
               AND NOT EXISTS (SELECT 1 FROM user_allergies a WHERE a.user_id = ? AND a.ingredient_id = i.id)
             ORDER BY i.recommend_order, i.name LIMIT 100',
            [$req->userId()]
        );
        Response::data(array_map([self::class, 'ingredientFmt'], $rows), 200, ['count' => count($rows)]);
    }

    /** Gambar bahan (publik, bukan data pribadi) supaya bisa dimuat oleh pemuat gambar tanpa token. */
    public static function ingredientImage(Request $req): never
    {
        $r = Db::one('SELECT image_path FROM ingredients WHERE id = ?', [$req->intParam('id')]);
        $path = $r && $r['image_path'] ? \Anemom\Storage::abs($r['image_path']) : null;
        if ($path === null || str_contains((string)$r['image_path'], '..') || !is_file($path)) {
            throw new ApiException(404, 'not_found', 'Gambar bahan tidak ada.');
        }
        $mime = (new \finfo(FILEINFO_MIME_TYPE))->file($path);
        header('Content-Type: ' . $mime);
        header('Content-Length: ' . filesize($path));
        header('Cache-Control: public, max-age=86400');
        readfile($path);
        exit;
    }

    public static function logList(Request $req): never
    {
        $sql = 'SELECT * FROM food_logs WHERE user_id = ?';
        $p = [$req->userId()];
        $since = HealthController::since($req);
        if ($since !== null) {
            $sql .= ' AND updated_at > ?';
        } else {
            $sql .= ' AND deleted_at IS NULL';
        }
        if ($since !== null) {
            $p[] = $since;
        }
        if (!empty($req->query['date'])) {
            [$a, $b] = self::dayRange($req->query['date']);
            $sql .= ' AND eaten_at >= ? AND eaten_at < ?';
            array_push($p, $a, $b);
        }
        $sql .= ' ORDER BY eaten_at DESC, id DESC LIMIT ' . HealthController::limit($req);
        $rows = array_map([self::class, 'logFmt'], Db::all($sql, $p));
        Response::data($rows, 200, ['count' => count($rows)]);
    }

    public static function logCreate(Request $req): never
    {
        $v = new Validator($req->body);
        $eaten = $v->datetime('eaten_at');
        $meal = $v->enum('meal_type', ['sarapan', 'makan_siang', 'makan_malam', 'camilan']);
        $ingredientId = $v->int('ingredient_id', 1, PHP_INT_MAX, false);
        $custom = $v->string('custom_name', 1, 120, false);
        $portions = $v->number('portions', 0.1, 20, false, 2) ?? 1.0;
        $imageId = $v->int('image_id', 1, PHP_INT_MAX, false);
        $aiLabel = $v->string('ai_label', 1, 80, false);
        $aiConf = $v->number('ai_confidence', 0, 1, false, 3);
        $source = $v->enum('source', ['manual', 'scan'], false) ?? 'manual';
        $uuid = $v->uuid();
        $v->check();
        if ($ingredientId === null && $custom === null) {
            throw new ApiException(422, 'validation_failed', 'Data yang dikirim tidak valid.', ['ingredient_id' => 'Isi ingredient_id atau custom_name.']);
        }
        $uid = $req->userId();
        if ($uuid !== null && ($old = Db::one('SELECT * FROM food_logs WHERE user_id = ? AND client_uuid = ?', [$uid, $uuid]))) {
            Response::data(self::logFmt($old));
        }
        $kcal = $iron = null;
        if ($ingredientId !== null) {
            $f = Db::one('SELECT * FROM ingredients WHERE id = ?', [$ingredientId]);
            $f ?? throw new ApiException(404, 'not_found', 'Bahan makanan tidak ditemukan.');
            $kcal = $f['calories_kcal'] === null ? null : round((float)$f['calories_kcal'] * $portions, 1);
            $iron = $f['iron_mg'] === null ? null : round((float)$f['iron_mg'] * $portions, 2);
        }
        if ($imageId !== null && !Db::one('SELECT 1 FROM images WHERE id = ? AND user_id = ? AND deleted_at IS NULL', [$imageId, $uid])) {
            throw new ApiException(404, 'not_found', 'Gambar tidak ditemukan.');
        }
        $id = Db::insert(
            'INSERT INTO food_logs (user_id, client_uuid, eaten_at, meal_type, ingredient_id, custom_name, portions, calories_kcal, iron_mg, source, image_id, ai_label, ai_confidence)
             VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)',
            [$uid, $uuid, $eaten, $meal, $ingredientId, $custom, $portions, $kcal, $iron, $source, $imageId, $aiLabel, $aiConf]
        );
        Response::data(self::logFmt(Db::one('SELECT * FROM food_logs WHERE id = ?', [$id])), 201);
    }

    public static function logDelete(Request $req): never
    {
        $n = Db::run('UPDATE food_logs SET deleted_at = UTC_TIMESTAMP() WHERE id = ? AND user_id = ? AND deleted_at IS NULL', [$req->intParam('id'), $req->userId()]);
        $n > 0 || throw new ApiException(404, 'not_found', 'Catatan makan tidak ditemukan.');
        Response::data(['deleted' => true]);
    }

    /** Ringkasan sehari: total kalori dan zat besi, plus penanda penghambat/pendukung serapan zat besi. */
    public static function logSummary(Request $req): never
    {
        $date = $req->query['date'] ?? Time::today();
        [$a, $b] = self::dayRange($date);
        $rows = Db::all(
            'SELECT l.calories_kcal, l.iron_mg, f.is_iron_booster, f.is_iron_inhibitor
             FROM food_logs l LEFT JOIN ingredients f ON f.id = l.ingredient_id
             WHERE l.user_id = ? AND l.deleted_at IS NULL AND l.eaten_at >= ? AND l.eaten_at < ?',
            [$req->userId(), $a, $b]
        );
        $kcal = $iron = 0.0;
        $booster = $inhibitor = false;
        foreach ($rows as $r) {
            $kcal += (float)$r['calories_kcal'];
            $iron += (float)$r['iron_mg'];
            $booster = $booster || (bool)$r['is_iron_booster'];
            $inhibitor = $inhibitor || (bool)$r['is_iron_inhibitor'];
        }
        Response::data([
            'date' => $date, 'entries' => count($rows),
            'calories_kcal' => round($kcal, 1), 'iron_mg' => round($iron, 2),
            'has_iron_booster' => $booster, 'has_iron_inhibitor' => $inhibitor,
        ]);
    }

    public static function articles(Request $req): never
    {
        $sql = 'SELECT slug, category, title, summary, published_at FROM articles WHERE is_published = 1';
        $p = [];
        if (!empty($req->query['category'])) {
            $sql .= ' AND category = ?';
            $p[] = $req->query['category'];
        }
        $rows = Db::all($sql . ' ORDER BY published_at DESC LIMIT 100', $p);
        Response::data(array_map(
            fn($r) => ['slug' => $r['slug'], 'category' => $r['category'], 'title' => $r['title'], 'summary' => $r['summary'], 'published_at' => Time::iso($r['published_at'])],
            $rows
        ));
    }

    public static function article(Request $req): never
    {
        $r = Db::one('SELECT slug, category, title, summary, body, published_at FROM articles WHERE slug = ? AND is_published = 1', [$req->param('slug')]);
        $r ?? throw new ApiException(404, 'not_found', 'Artikel tidak ditemukan.');
        $r['published_at'] = Time::iso($r['published_at']);
        Response::data($r);
    }

    private static function dayRange(string $date): array
    {
        if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $date) || !checkdate((int)substr($date, 5, 2), (int)substr($date, 8, 2), (int)substr($date, 0, 4))) {
            throw new ApiException(422, 'validation_failed', 'Parameter date harus YYYY-MM-DD.');
        }
        return Time::dayRangeUtc($date);
    }

    /**
     * Kelompok tampilan di aplikasi (tab Sayur / Buah / Protein): satu-satunya tempat pemetaannya.
     * Kolom `category` di database tidak diubah; hewani dan nabati sama-sama Protein,
     * minuman dan lainnya masuk "lainnya".
     */
    public static function groupOf(string $category): string
    {
        return match ($category) {
            'sayur' => 'sayur',
            'buah' => 'buah',
            'hewani', 'nabati' => 'protein',
            default => 'lainnya',
        };
    }

    private static function ingredientFmt(array $r): array
    {
        $n = fn($x) => $x === null ? null : (float)$x;
        return [
            'id' => (int)$r['id'], 'name' => $r['name'], 'category' => $r['category'],
            'group' => self::groupOf($r['category']),
            'description' => $r['description'], 'iron_type' => $r['iron_type'],
            'serving_desc' => $r['serving_desc'], 'serving_grams' => (float)$r['serving_grams'],
            'calories_kcal' => $n($r['calories_kcal']), 'protein_g' => $n($r['protein_g']), 'iron_mg' => $n($r['iron_mg']),
            'vitamin_c_mg' => $n($r['vitamin_c_mg']), 'folate_mcg' => $n($r['folate_mcg']),
            'is_iron_booster' => (bool)$r['is_iron_booster'], 'is_iron_inhibitor' => (bool)$r['is_iron_inhibitor'],
            'is_recommended' => (bool)$r['is_recommended'], 'recommend_order' => (int)$r['recommend_order'],
            'pregnancy_warning' => $r['pregnancy_warning'], 'reference' => $r['reference'],
            'image_url' => $r['image_path'] ? '/api/v1/ingredients/' . $r['id'] . '/image' : null,
        ];
    }

    private static function logFmt(array $r): array
    {
        $n = fn($x) => $x === null ? null : (float)$x;
        return [
            'id' => (int)$r['id'], 'client_uuid' => $r['client_uuid'], 'eaten_at' => Time::iso($r['eaten_at']),
            'meal_type' => $r['meal_type'], 'ingredient_id' => $r['ingredient_id'] === null ? null : (int)$r['ingredient_id'],
            'custom_name' => $r['custom_name'], 'portions' => (float)$r['portions'],
            'calories_kcal' => $n($r['calories_kcal']), 'iron_mg' => $n($r['iron_mg']),
            'source' => $r['source'], 'image_id' => $r['image_id'] === null ? null : (int)$r['image_id'],
            'ai_label' => $r['ai_label'], 'ai_confidence' => $n($r['ai_confidence']),
            'updated_at' => Time::iso($r['updated_at']), 'deleted_at' => Time::iso($r['deleted_at']),
        ];
    }
}
