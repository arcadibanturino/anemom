<?php
declare(strict_types=1);

// Front controller: semua request /api/v1/* masuk ke sini (lihat deploy/nginx.conf).
require __DIR__ . '/../src/Support.php';
require __DIR__ . '/../src/Services.php';
foreach (glob(__DIR__ . '/../src/Controllers/*.php') as $f) {
    require $f;
}

use Anemom\ApiException;
use Anemom\Controllers\AccountController as A;
use Anemom\Controllers\HealthController as H;
use Anemom\Controllers\NutritionController as N;
use Anemom\Controllers\ScreeningController as S;
use Anemom\Controllers\SystemController as Sys;
use Anemom\Env;
use Anemom\Request;
use Anemom\Response;
use Anemom\Router;

date_default_timezone_set('UTC');
header('X-Content-Type-Options: nosniff');
header('Cache-Control: no-store');

set_exception_handler(function (Throwable $e): never {
    if ($e instanceof ApiException) {
        Response::error($e->status, $e->errorCode, $e->getMessage(), $e->details);
    }
    error_log((string)$e);
    Response::error(500, 'server_error', Env::bool('APP_DEBUG') ? $e->getMessage() : 'Terjadi kesalahan pada server.');
});

$r = new Router();
$P = '/api/v1';

// Publik
$r->add('GET',  "$P/health", [Sys::class, 'health'], false);
$r->add('GET',  "$P/config", [Sys::class, 'config'], false);
$r->add('POST', "$P/auth/register", [A::class, 'register'], false);
$r->add('POST', "$P/auth/login", [A::class, 'login'], false);

// Akun
$r->add('POST',   "$P/auth/logout", [A::class, 'logout']);
$r->add('GET',    "$P/me", [A::class, 'me']);
$r->add('PUT',    "$P/me", [A::class, 'updateMe']);
$r->add('PUT',    "$P/me/password", [A::class, 'changePassword']);
$r->add('DELETE', "$P/me", [A::class, 'deleteMe']);
$r->add('GET',    "$P/consents", [A::class, 'consents']);
$r->add('POST',   "$P/consents", [A::class, 'setConsent']);

// Beranda
$r->add('GET', "$P/dashboard", [H::class, 'dashboard']);

// Hb laboratorium
$r->add('GET',    "$P/hb-results", [H::class, 'hbList']);
$r->add('POST',   "$P/hb-results", [H::class, 'hbCreate']);
$r->add('GET',    "$P/hb-results/{id}", [H::class, 'hbGet']);
$r->add('DELETE', "$P/hb-results/{id}", [H::class, 'hbDelete']);

// TTD
$r->add('GET',    "$P/ttd", [H::class, 'ttdMonth']);
$r->add('POST',   "$P/ttd/take", [H::class, 'ttdTake']);
$r->add('GET',    "$P/ttd/settings", [H::class, 'ttdSettings']);
$r->add('PUT',    "$P/ttd/settings", [H::class, 'ttdSettingsUpdate']);
$r->add('DELETE', "$P/ttd/{date}", [H::class, 'ttdDelete']);

// Gambar + skrining
$r->add('POST',   "$P/images", [S::class, 'imageUpload']);
$r->add('GET',    "$P/images/{id}/file", [S::class, 'imageFile']);
$r->add('DELETE', "$P/images/{id}", [S::class, 'imageDelete']);
$r->add('GET',    "$P/screenings", [S::class, 'list']);
$r->add('POST',   "$P/screenings/eye", [S::class, 'createEye']);
$r->add('POST',   "$P/screenings/ppg", [S::class, 'createPpg']);
$r->add('GET',    "$P/screenings/{id}", [S::class, 'get']);
$r->add('DELETE', "$P/screenings/{id}", [S::class, 'delete']);
$r->add('PUT',    "$P/screenings/{id}/link-hb", [S::class, 'linkHb']);

// Band
$r->add('GET',    "$P/devices", [S::class, 'deviceList']);
$r->add('POST',   "$P/devices", [S::class, 'deviceRegister']);
$r->add('PUT',    "$P/devices/{id}/status", [S::class, 'deviceStatus']);
$r->add('DELETE', "$P/devices/{id}", [S::class, 'deviceDelete']);

// Bahan makanan, rekomendasi, catatan makan, artikel
$r->add('GET',    "$P/ingredients", [N::class, 'ingredients']);
$r->add('GET',    "$P/ingredients/{id}", [N::class, 'ingredient']);
$r->add('GET',    "$P/ingredients/{id}/image", [N::class, 'ingredientImage'], false);
$r->add('GET',    "$P/recommendations", [N::class, 'recommendations']);
$r->add('GET',    "$P/food-logs", [N::class, 'logList']);
$r->add('POST',   "$P/food-logs", [N::class, 'logCreate']);
$r->add('GET',    "$P/food-logs/summary", [N::class, 'logSummary']);
$r->add('DELETE', "$P/food-logs/{id}", [N::class, 'logDelete']);
$r->add('GET',    "$P/articles", [N::class, 'articles']);
$r->add('GET',    "$P/articles/{slug}", [N::class, 'article']);

// Notifikasi
$r->add('POST',   "$P/fcm-tokens", [Sys::class, 'fcmRegister']);
$r->add('DELETE', "$P/fcm-tokens", [Sys::class, 'fcmRemove']);
$r->add('GET',    "$P/notifications", [Sys::class, 'notifications']);
$r->add('PUT',    "$P/notifications/{id}/read", [Sys::class, 'notificationRead']);

$r->dispatch(Request::capture());
