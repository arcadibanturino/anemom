"""Konfigurasi dari environment. Semua jalur relatif terhadap folder ml-service/."""
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MODELS = ROOT / "models"

# Model segmentasi yang dipakai bila permintaan tidak menyebut `segmenter`.
DEFAULT_SEGMENTER = os.environ.get("DEFAULT_SEGMENTER", "unet")

# Segmenter yang dimuat saat server jalan. Contoh: "unet" atau "unet,yolo".
ENABLED_SEGMENTERS = [s.strip() for s in os.environ.get("ENABLED_SEGMENTERS", "unet").split(",") if s.strip()]

UNET_MODEL = Path(os.environ.get("UNET_MODEL", MODELS / "segmentation/unet/anemom_unet_fp32.tflite"))
YOLO_MODEL = Path(os.environ.get("YOLO_MODEL", MODELS / "segmentation/yolo/best.pt"))

CLASSIFIER_BUNDLE = Path(os.environ.get(
    "CLASSIFIER_BUNDLE", MODELS / "classifier/g2_two_candidates_RESEARCH_ONLY.joblib"))
# Kandidat di dalam bundle: RF_legacy27 atau LR_robust_extended.
CLASSIFIER_CANDIDATE = os.environ.get("CLASSIFIER_CANDIDATE", "RF_legacy27")

# Pemeriksaan kualitas mask: SAMA dengan MaskQuality.kt di Android.
MIN_MASK_AREA = float(os.environ.get("MIN_MASK_AREA", "0.03"))
MAX_MASK_AREA = float(os.environ.get("MAX_MASK_AREA", "0.60"))
