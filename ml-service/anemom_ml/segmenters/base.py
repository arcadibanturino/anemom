"""Kontrak BERSAMA semua model segmentasi (pasangan ConjunctivaSegmenter.kt di Android).

Ubah berkas ini hanya bila tim U-Net dan tim YOLO sepakat.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol

import numpy as np

# Ukuran mask keluaran SEMUA segmenter, supaya Dice/IoU dan fitur warna sebanding.
MASK_SIZE = 256


@dataclass(frozen=True)
class SegmenterInfo:
    segmenter: str      # kode singkat: "unet" / "yolo" (dikirim aplikasi di field `segmenter`)
    architecture: str   # sama dengan ai_models.architecture
    model_name: str     # sama dengan ai_models.name (runtime server)
    model_version: str  # sama dengan ai_models.version


@dataclass
class SegResult:
    mask: np.ndarray        # float32 [MASK_SIZE, MASK_SIZE], 0..1
    threshold: float = 0.5

    @property
    def binary(self) -> np.ndarray:
        return self.mask >= self.threshold

    @property
    def area_ratio(self) -> float:
        return float(self.binary.mean())


class Segmenter(Protocol):
    info: SegmenterInfo

    def segment(self, image_rgb: np.ndarray) -> SegResult:
        """image_rgb: uint8 [H, W, 3] urutan RGB, ukuran asli foto. Mengembalikan mask 256 x 256."""
        ...
