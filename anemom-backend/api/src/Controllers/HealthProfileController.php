<?php
declare(strict_types=1);

namespace Anemom\Controllers;

use Anemom\ApiException;
use Anemom\Db;
use Anemom\Request;
use Anemom\Response;
use Anemom\Validator;

/**
 * Profil Kesehatan Awal (langkah 2 pendaftaran): tinggi badan, berat sebelum hamil, riwayat anemia,
 * alergi makanan (dipilih dari tabel ingredients) dan LiLA (opsional).
 *
 * Akun dibuat di langkah 1 (POST /auth/register). Selama data wajib langkah 2 belum lengkap,
 * `profile_completed` pada GET /me bernilai false dan aplikasi mengarahkan ibu ke layar ini.
 */
final class HealthProfileController
{
    private const MAX_ALLERGENS = 200;

    /** GET /me/health-profile: isi form (untuk melanjutkan pengisian atau mengubah nanti). */
    public static function show(Request $req): never
    {
        Response::data(self::payload($req->userId()));
    }

    /**
     * PUT /me/health-profile: simpan lengkap (bukan sebagian). Field yang tidak dikirim dianggap kosong,
     * jadi lila_cm yang tidak dikirim menghapus LiLA lama. Mengembalikan user seperti GET /me.
     */
    public static function update(Request $req): never
    {
        $v = new Validator($req->body);
        $height = $v->number('height_cm', 100, 220);
        $weight = $v->number('pre_pregnancy_weight_kg', 30, 200);
        $anemia = $v->enum('anemia_history', ['ya', 'tidak', 'tidak_tahu']);
        $allergy = $v->enum('allergy_status', ['ada', 'tidak', 'tidak_tahu']);
        $lila = $v->number('lila_cm', 15, 50, false);
        $v->check();

        // Daftar bahan hanya dipakai bila status "ada"; untuk "tidak"/"tidak_tahu" daftar lama dikosongkan.
        $ids = $allergy === 'ada' ? self::allergenIds($req->body['allergen_ids'] ?? null) : [];
        if ($allergy === 'ada' && !$ids) {
            throw new ApiException(422, 'validation_failed', 'Data yang dikirim tidak valid.', [
                'allergen_ids' => 'Pilih minimal satu bahan yang menyebabkan alergi.',
            ]);
        }
        if ($ids) {
            $marks = implode(',', array_fill(0, count($ids), '?'));
            $found = Db::all("SELECT id FROM ingredients WHERE id IN ($marks)", $ids);
            if (count($found) !== count($ids)) {
                throw new ApiException(422, 'validation_failed', 'Data yang dikirim tidak valid.', [
                    'allergen_ids' => 'Ada bahan makanan yang tidak dikenal.',
                ]);
            }
        }

        $uid = $req->userId();
        $pdo = Db::pdo();
        $pdo->beginTransaction();
        try {
            Db::run(
                'UPDATE mother_profiles
                 SET height_cm = ?, pre_pregnancy_weight_kg = ?, anemia_history = ?, allergy_status = ?, lila_cm = ?
                 WHERE user_id = ?',
                [$height, $weight, $anemia, $allergy, $lila, $uid]
            );
            Db::run('DELETE FROM user_allergies WHERE user_id = ?', [$uid]);
            foreach ($ids as $id) {
                Db::run('INSERT INTO user_allergies (user_id, ingredient_id) VALUES (?,?)', [$uid, $id]);
            }
            $pdo->commit();
        } catch (\Throwable $e) {
            $pdo->rollBack();
            throw $e;
        }
        Response::data(AccountController::userPayload($uid));
    }

    /** Profil lengkap = keempat kolom wajib terisi. Dipakai GET /me sebagai `profile_completed`. */
    public static function isComplete(array $profileRow): bool
    {
        foreach (['height_cm', 'pre_pregnancy_weight_kg', 'anemia_history', 'allergy_status'] as $col) {
            if (($profileRow[$col] ?? null) === null) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------

    private static function payload(int $uid): array
    {
        $p = Db::one(
            'SELECT height_cm, pre_pregnancy_weight_kg, anemia_history, allergy_status, lila_cm
             FROM mother_profiles WHERE user_id = ?',
            [$uid]
        ) ?? throw new ApiException(404, 'not_found', 'Profil tidak ditemukan.');
        $ids = array_map(
            fn($r) => (int)$r['ingredient_id'],
            Db::all('SELECT ingredient_id FROM user_allergies WHERE user_id = ? ORDER BY ingredient_id', [$uid])
        );
        $n = fn($x) => $x === null ? null : (float)$x;
        return [
            'height_cm' => $n($p['height_cm']),
            'pre_pregnancy_weight_kg' => $n($p['pre_pregnancy_weight_kg']),
            'anemia_history' => $p['anemia_history'],
            'allergy_status' => $p['allergy_status'],
            'lila_cm' => $n($p['lila_cm']),
            'allergen_ids' => $ids,
            'profile_completed' => self::isComplete($p),
        ];
    }

    /** @return int[] id unik, berurutan; null/kosong = []. Bukan daftar angka positif = 422. */
    private static function allergenIds(mixed $raw): array
    {
        $bad = fn() => new ApiException(422, 'validation_failed', 'Data yang dikirim tidak valid.', [
            'allergen_ids' => 'Harus berupa daftar id bahan makanan (angka).',
        ]);
        if ($raw === null || $raw === '') {
            return [];
        }
        if (!is_array($raw) || count($raw) > self::MAX_ALLERGENS) {
            throw $bad();
        }
        $ids = [];
        foreach ($raw as $x) {
            if (is_int($x) ? $x < 1 : !(is_string($x) && ctype_digit($x) && (int)$x >= 1)) {
                throw $bad();
            }
            $ids[(int)$x] = (int)$x;
        }
        ksort($ids);
        return array_values($ids);
    }
}
