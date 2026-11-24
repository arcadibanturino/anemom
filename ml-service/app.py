"""Layanan AI mata Anemom (hanya localhost; dipanggil API PHP, bukan langsung oleh aplikasi).

Jalankan:  uvicorn app:app --host 127.0.0.1 --port 8001

Endpoint:
  GET  /health        status model
  POST /segment/eye   multipart `image`, [`segmenter`]  -> luas mask + kualitas (tahap 1 saja)
  POST /predict/eye   multipart `image`, [`segmenter`]  -> label anemia (tahap 1 + 2)

`segmenter` = "unet" / "yolo"; kosong = DEFAULT_SEGMENTER. Kontrak respons /predict/eye tetap
{label, confidence, est_hb, model_version} seperti di docs/API.md, ditambah info model segmentasi.
"""
from __future__ import annotations

import base64
import io

import numpy as np
from PIL import Image, ImageOps, UnidentifiedImageError
from starlette.applications import Starlette
from starlette.requests import Request
from starlette.responses import JSONResponse
from starlette.routing import Route

from anemom_ml import config, features, segmenters
from anemom_ml.classifier import ColorClassifier

MAX_PIXELS = 4000 * 4000

_seg_status = segmenters.load_enabled()
try:
    _classifier: ColorClassifier | None = ColorClassifier(config.CLASSIFIER_BUNDLE, config.CLASSIFIER_CANDIDATE)
    _cls_status = "ok"
except Exception as e:  # noqa: BLE001 - dilaporkan di /health
    _classifier, _cls_status = None, f"{type(e).__name__}: {e}"


def _error(status: int, code: str, message: str) -> JSONResponse:
    return JSONResponse({"error": {"code": code, "message": message}}, status_code=status)


async def _read_request(request: Request):
    """-> (image_rgb uint8 HxWx3, segmenter) atau JSONResponse galat."""
    form = await request.form()
    upload = form.get("image")
    if upload is None or not hasattr(upload, "read"):
        return _error(400, "image_required", "Field multipart `image` wajib diisi.")
    raw = await upload.read()
    try:
        img = Image.open(io.BytesIO(raw))
        if img.width * img.height > MAX_PIXELS:
            return _error(413, "image_too_large", "Resolusi foto terlalu besar.")
        img = ImageOps.exif_transpose(img).convert("RGB")   # foto HP sering berotasi lewat EXIF
    except (UnidentifiedImageError, OSError):
        return _error(415, "unsupported_media", "Berkas bukan gambar yang valid.")

    name = (form.get("segmenter") or config.DEFAULT_SEGMENTER).strip().lower()
    try:
        seg = segmenters.get(name)
    except KeyError:
        return _error(400, "unknown_segmenter", f"Segmenter {name!r} tidak dikenal; pilihan: {segmenters.known()}.")
    except LookupError:
        return _error(503, "segmenter_disabled", f"Segmenter {name!r} tidak aktif di server ini.")
    except Exception as e:  # noqa: BLE001
        return _error(503, "segmenter_failed", f"Segmenter {name!r} gagal dimuat: {e}")
    return np.asarray(img, dtype=np.uint8), seg


def _quality(area: float) -> str:
    if area < config.MIN_MASK_AREA:
        return "too_small"
    if area > config.MAX_MASK_AREA:
        return "too_large"
    return "ok"


def _seg_payload(seg, result) -> dict:
    return {
        "segmenter": seg.info.segmenter,
        "seg_model_name": seg.info.model_name,
        "seg_model_version": seg.info.model_version,
        "mask_area": round(result.area_ratio, 4),
        "quality": _quality(result.area_ratio),
    }


async def health(_: Request) -> JSONResponse:
    feat = f"ok ({features.FEATURE_VERSION})"
    return JSONResponse({
        "segmenters": _seg_status,
        "default_segmenter": config.DEFAULT_SEGMENTER,
        "classifier": _cls_status if _classifier is None else {
            "candidate": _classifier.candidate, "model_name": _classifier.db_name,
            "model_version": _classifier.db_version, "research_only": _classifier.research_only,
            "sklearn_warning": _classifier.version_warning},
        "features": feat,
    })


async def segment_eye(request: Request) -> JSONResponse:
    parsed = await _read_request(request)
    if isinstance(parsed, JSONResponse):
        return parsed
    image, seg = parsed
    result = seg.segment(image)
    body = _seg_payload(seg, result)
    if request.query_params.get("mask") == "1":
        png = io.BytesIO()
        Image.fromarray((result.binary * 255).astype(np.uint8)).save(png, format="PNG")
        body["mask_png_base64"] = base64.b64encode(png.getvalue()).decode()
    return JSONResponse(body)


async def predict_eye(request: Request) -> JSONResponse:
    if _classifier is None:
        return _error(503, "classifier_not_ready", f"Model klasifikasi gagal dimuat: {_cls_status}")
    parsed = await _read_request(request)
    if isinstance(parsed, JSONResponse):
        return parsed
    image, seg = parsed
    result = seg.segment(image)
    seg_info = _seg_payload(seg, result)
    if seg_info["quality"] != "ok":
        return JSONResponse({"error": {"code": "poor_quality",
                                       "message": "Konjungtiva tidak terlihat jelas. Ulangi foto.",
                                       "details": seg_info}}, status_code=422)

    try:
        # Foto ASLI: pembungkus G1 mengecilkannya sendiri dengan cv2.INTER_AREA seperti saat pelatihan.
        feats = features.extract(image, result.binary)
    except features.MaskRejected as e:
        return JSONResponse({"error": {"code": "poor_quality",
                                       "message": "Konjungtiva tidak terlihat jelas. Ulangi foto.",
                                       "details": {**seg_info, "reason": str(e)}}}, status_code=422)

    try:
        pred = _classifier.predict(feats)
    except Exception as e:  # noqa: BLE001 - mis. versi scikit-learn tidak cocok
        hint = f" {_classifier.version_warning}" if _classifier.version_warning else ""
        return _error(500, "inference_failed", f"Klasifikasi gagal: {type(e).__name__}.{hint}")
    return JSONResponse({
        "label": pred.label,
        "confidence": pred.confidence,
        "est_hb": None,                          # model sekarang hanya klasifikasi
        "model_version": _classifier.db_version,  # versi model KLASIFIKASI (kontrak lama)
        "model_name": _classifier.db_name,
        **seg_info,
        # Skor mentah model (peluang kelas anemia) dan ambang skrining yang dipakai untuk `label`.
        # Aplikasi menampilkan skor + ambang, bukan "keyakinan", agar tidak menyesatkan.
        "p_anemia": pred.p_anemia,
        "threshold": round(pred.threshold, 4),
        "research_only": _classifier.research_only,
    })


app = Starlette(routes=[
    Route("/health", health, methods=["GET"]),
    Route("/segment/eye", segment_eye, methods=["POST"]),
    Route("/predict/eye", predict_eye, methods=["POST"]),
])
