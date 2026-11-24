"""Daftar segmenter. Menambah model baru = menambah satu baris di _FACTORIES (tanpa mengubah app.py)."""
from __future__ import annotations

from .. import config
from .base import Segmenter


def _unet() -> Segmenter:
    from .unet import UnetSegmenter
    return UnetSegmenter(config.UNET_MODEL)


def _yolo() -> Segmenter:
    from .yolo import YoloSegmenter
    return YoloSegmenter(config.YOLO_MODEL)


_FACTORIES = {"unet": _unet, "yolo": _yolo}
_loaded: dict[str, Segmenter] = {}


def known() -> list[str]:
    return list(_FACTORIES)


def load_enabled() -> dict[str, str]:
    """Muat semua segmenter di ENABLED_SEGMENTERS. Mengembalikan {nama: "ok" atau pesan galat}."""
    status = {}
    for name in config.ENABLED_SEGMENTERS:
        try:
            get(name)
            status[name] = "ok"
        except Exception as e:  # noqa: BLE001 - dilaporkan di /health
            status[name] = f"{type(e).__name__}: {e}"
    return status


def get(name: str) -> Segmenter:
    if name not in _FACTORIES:
        raise KeyError(name)
    if name not in config.ENABLED_SEGMENTERS:
        raise LookupError(name)
    if name not in _loaded:
        _loaded[name] = _FACTORIES[name]()
    return _loaded[name]
