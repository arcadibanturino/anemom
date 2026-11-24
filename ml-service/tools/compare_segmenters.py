"""Bandingkan model segmentasi (U-Net vs YOLO) pada data uji yang SAMA.

Struktur data mengikuti Eyes-defy-anemia: satu folder per pasien berisi foto asli dan mask buatan tangan
`palpebral.png` (folder lain seperti forniceal.png diabaikan).

Contoh:
  ENABLED_SEGMENTERS=unet,yolo python tools/compare_segmenters.py /data/eyes-defy-anemia/Italy \\
      --ids data_uji_ids.txt --out hasil_perbandingan.csv

--ids  : berkas berisi nama folder data UJI (satu per baris). Pakai split uji yang sama untuk semua model,
         dan jangan memakai folder yang dipakai melatih model mana pun.

Keluaran: CSV per foto (dice, iou, mask_area, waktu ms per segmenter) + ringkasan rata-rata di terminal.
"""
from __future__ import annotations

import argparse
import csv
import statistics
import sys
import time
from pathlib import Path

import numpy as np
from PIL import Image, ImageOps

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from anemom_ml import config, segmenters  # noqa: E402
from anemom_ml.segmenters.base import MASK_SIZE  # noqa: E402

MASK_NAMES = {"palpebral.png", "forniceal.png", "forniceal_palpebral.png"}
IMAGE_EXT = {".jpg", ".jpeg", ".png", ".bmp", ".tif", ".tiff"}


def load_gt(path: Path) -> np.ndarray:
    """Mask buatan tangan -> bool 256 x 256. Piksel tidak hitam = konjungtiva."""
    m = np.asarray(Image.open(path).convert("L").resize((MASK_SIZE, MASK_SIZE), Image.NEAREST))
    return m > 0


def dice_iou(pred: np.ndarray, gt: np.ndarray) -> tuple[float, float]:
    inter = np.logical_and(pred, gt).sum()
    total = pred.sum() + gt.sum()
    union = np.logical_or(pred, gt).sum()
    dice = 1.0 if total == 0 else 2.0 * inter / total
    iou = 1.0 if union == 0 else inter / union
    return float(dice), float(iou)


def find_samples(root: Path, ids: set[str] | None):
    for folder in sorted(p for p in root.iterdir() if p.is_dir()):
        if ids is not None and folder.name not in ids:
            continue
        gt = folder / "palpebral.png"
        photos = [p for p in folder.iterdir()
                  if p.suffix.lower() in IMAGE_EXT and p.name.lower() not in MASK_NAMES]
        if gt.exists() and len(photos) == 1:
            yield folder.name, photos[0], gt
        else:
            print(f"lewati {folder.name}: mask={gt.exists()} foto={len(photos)}", file=sys.stderr)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("root", type=Path)
    ap.add_argument("--ids", type=Path)
    ap.add_argument("--out", type=Path, default=Path("hasil_perbandingan.csv"))
    args = ap.parse_args()

    ids = None if args.ids is None else {l.strip() for l in args.ids.read_text().splitlines() if l.strip()}
    segs = {name: segmenters.get(name) for name in config.ENABLED_SEGMENTERS}
    rows, scores = [], {n: {"dice": [], "iou": [], "ms": []} for n in segs}

    for sample_id, photo, gt_path in find_samples(args.root, ids):
        image = np.asarray(ImageOps.exif_transpose(Image.open(photo)).convert("RGB"), dtype=np.uint8)
        gt = load_gt(gt_path)
        for name, seg in segs.items():
            t0 = time.perf_counter()
            res = seg.segment(image)
            ms = (time.perf_counter() - t0) * 1000
            d, i = dice_iou(res.binary, gt)
            scores[name]["dice"].append(d)
            scores[name]["iou"].append(i)
            scores[name]["ms"].append(ms)
            rows.append({"sample": sample_id, "segmenter": name, "model": seg.info.model_name,
                         "version": seg.info.model_version, "dice": round(d, 4), "iou": round(i, 4),
                         "mask_area": round(res.area_ratio, 4), "ms": round(ms, 1)})

    with args.out.open("w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0]) if rows else ["sample"])
        w.writeheader()
        w.writerows(rows)

    print(f"{len(rows)} baris -> {args.out}")
    for name, s in scores.items():
        if s["dice"]:
            print(f"{name:6s} n={len(s['dice'])}  Dice {statistics.mean(s['dice']):.3f}"
                  f"  IoU {statistics.mean(s['iou']):.3f}  waktu {statistics.median(s['ms']):.0f} ms (median)")


if __name__ == "__main__":
    main()
