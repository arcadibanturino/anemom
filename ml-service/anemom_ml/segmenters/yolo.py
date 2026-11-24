"""YOLOv8-seg di server. Milik tim YOLO. BELUM DIBUAT.

Yang perlu dikerjakan tim YOLO (tanpa mengubah base.py, unet.py, atau app.py):
  1. Simpan bobot hasil latih di models/segmentation/yolo/best.pt (atau set env YOLO_MODEL).
  2. Tambahkan `ultralytics` ke requirements-yolo.txt.
  3. Isi segment():
        from ultralytics import YOLO
        r = self._model.predict(image_rgb[..., ::-1], imgsz=..., verbose=False)[0]   # ultralytics memakai BGR
        - tidak ada deteksi -> mask nol (MaskQuality akan menolak)
        - ambil mask deteksi skor tertinggi: r.masks.data[i] (sudah sigmoid, ukuran imgsz)
        - ubah ukuran ke 256 x 256 (base.MASK_SIZE), float32 0..1
  4. Isi `info.model_version` sesuai baris ai_models `conjunctiva-yolov8-server`.
"""
from __future__ import annotations

from pathlib import Path

import numpy as np

from .base import SegmenterInfo, SegResult


class YoloSegmenter:
    info = SegmenterInfo(
        segmenter="yolo",
        architecture="yolov8n-seg",
        model_name="conjunctiva-yolov8-server",
        model_version="0.0.1",
    )

    def __init__(self, model_path: Path):
        raise NotImplementedError(
            "YoloSegmenter belum dibuat (tugas tim YOLO). Jangan aktifkan 'yolo' di ENABLED_SEGMENTERS dulu.")

    def segment(self, image_rgb: np.ndarray) -> SegResult:  # pragma: no cover
        raise NotImplementedError
