<?php
declare(strict_types=1);

// Ulangi job inferensi mata (layanan AI ml-service: U-Net / YOLO) yang tertunda. Jadwalkan lewat cron, mis. tiap menit:
//   * * * * * php /var/www/anemom/api/bin/process_jobs.php >> /var/log/anemom-jobs.log 2>&1
require __DIR__ . '/../src/Support.php';
require __DIR__ . '/../src/Services.php';
require __DIR__ . '/../src/Controllers/ScreeningController.php';

use Anemom\Db;
use Anemom\Controllers\ScreeningController;

$jobs = Db::all("SELECT id FROM inference_jobs WHERE status = 'queued' AND attempts < 3 ORDER BY created_at LIMIT 20");
foreach ($jobs as $j) {
    $ok = ScreeningController::runJob((int)$j['id']);
    echo gmdate('c'), " job {$j['id']} ", $ok ? 'done' : 'retry/failed', PHP_EOL;
}
