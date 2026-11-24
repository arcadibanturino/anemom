"""Fitur warna konjungtiva `G1-color-features-exact-v1` (tahap 2). Dipakai BERSAMA oleh semua segmenter.

Perhitungan fitur ada di `g1_feature_extraction.py`, disalin APA ADANYA dari tim AI
(Anemom_G1_Untuk_Backend.zip). Jangan ubah berkas itu; ubah hanya pembungkus di bawah ini.

Pembungkus ini meniru `load_pair()` milik tim AI, tetapi mask berasal dari segmenter (U-Net/YOLO),
bukan dari berkas mask buatan tangan:
  - foto asli dikecilkan ke 256 x 256 dengan cv2.INTER_AREA, RGB uint8
  - mask 256 x 256 boolean; hanya komponen terhubung TERBESAR (8-arah) yang dipakai
  - fraksi mask harus 0,002 - 0,70 dan minimal 80 piksel
"""
from __future__ import annotations

import cv2
import numpy as np

from . import g1_feature_extraction as g1

FEATURE_VERSION = "G1-color-features-exact-v1"
SIZE = g1.SIZE  # 256


class FeaturesNotReady(RuntimeError):
    """Dipertahankan agar kompatibel dengan app.py lama; tidak dilempar lagi."""


class MaskRejected(ValueError):
    """Mask tidak layak untuk dihitung fiturnya (sama dengan aturan load_pair tim AI)."""


def prepare(image_rgb: np.ndarray, mask_256: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    """image_rgb: uint8 [H, W, 3] RGB ukuran ASLI. mask_256: bool [256, 256] dari segmenter.
    -> (rgb_256 uint8, mask_256 bool) siap untuk g1.get_features()."""
    if image_rgb.ndim != 3 or image_rgb.shape[2] != 3:
        raise ValueError("Foto harus RGB [H, W, 3]")
    if mask_256.shape != (SIZE, SIZE):
        raise ValueError(f"Mask harus {SIZE}x{SIZE}, bukan {mask_256.shape}")
    m = mask_256.astype(bool)
    nc, cc, stats, _ = cv2.connectedComponentsWithStats(m.astype("uint8"), 8)
    if nc > 1:
        m = cc == (1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA])))
    fraction = float(m.mean())
    if not (0.002 < fraction < 0.70):
        raise MaskRejected(f"Fraksi mask tak wajar {fraction:.4f}")
    if m.sum() < 80:
        raise MaskRejected("Mask terlalu kecil (< 80 piksel)")
    rgb = cv2.resize(np.ascontiguousarray(image_rgb, dtype=np.uint8), (SIZE, SIZE), interpolation=cv2.INTER_AREA)
    return rgb, m


def extract(image_rgb: np.ndarray, mask_256: np.ndarray) -> dict[str, float]:
    """Semua fitur G1 (27 legacy + rob_* + QC). image_rgb = foto ASLI (RGB uint8), mask_256 = bool 256x256."""
    rgb, m = prepare(image_rgb, mask_256)
    return g1.get_features(rgb, m)
