"""U-Net `anemom_palpebral_unet` di server. Milik tim U-Net.

Memakai berkas .tflite yang SAMA dengan aplikasi Android, sehingga mask di HP dan di server identik.

TODO(tim AI) konfirmasi praproses (harus sama dengan saat pelatihan DAN dengan UnetTfliteSegmenter.kt):
  - resize langsung ke 256 x 256 (rasio tidak dipertahankan), interpolasi bilinear
  - RGB dibagi 255 -> 0..1
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

from .base import MASK_SIZE, SegmenterInfo, SegResult

PIXEL_SCALE = 255.0
THRESHOLD = 0.5


def _load_interpreter(path: Path):
    try:
        from ai_edge_litert.interpreter import Interpreter
    except ImportError:
        try:
            from tflite_runtime.interpreter import Interpreter
        except ImportError:
            from tensorflow.lite import Interpreter  # type: ignore
    return Interpreter(model_path=str(path), num_threads=2)


class UnetSegmenter:
    info = SegmenterInfo(
        segmenter="unet",
        architecture="unet",
        model_name="conjunctiva-unet-server",
        model_version="2026.11.18",
    )

    def __init__(self, model_path: Path):
        self._it = _load_interpreter(model_path)
        self._it.allocate_tensors()
        self._in = self._it.get_input_details()[0]
        self._out = self._it.get_output_details()[0]
        shape = tuple(self._in["shape"])
        if shape[1:] != (MASK_SIZE, MASK_SIZE, 3):
            raise ValueError(f"Input U-Net tidak terduga: {shape}")

    def segment(self, image_rgb: np.ndarray) -> SegResult:
        img = Image.fromarray(image_rgb).resize((MASK_SIZE, MASK_SIZE), Image.BILINEAR)
        x = (np.asarray(img, dtype=np.float32) / PIXEL_SCALE)[None, ...]
        self._it.set_tensor(self._in["index"], x)
        self._it.invoke()
        mask = self._it.get_tensor(self._out["index"])[0, :, :, 0].astype(np.float32)
        return SegResult(mask=mask, threshold=THRESHOLD)
