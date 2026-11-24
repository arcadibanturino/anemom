"""Klasifikasi anemia dari fitur warna (tahap 2), memakai bundle joblib `g2_two_candidates`.

Berkas joblib adalah pickle: hanya muat berkas dari tim sendiri, jangan dari sumber lain.
"""
from __future__ import annotations

import math
import warnings
from dataclasses import dataclass
from pathlib import Path

import joblib
import numpy as np
import sklearn

# Nama dan versi di tabel ai_models untuk tiap kandidat.
DB_MODELS = {
    "RF_legacy27": ("conjunctiva-color-rf", "g2-rf27"),
    "LR_robust_extended": ("conjunctiva-color-lr", "g2-lr35"),
}

# TODO(tim AI): pastikan kelas 1 = anemia (label_rule: F Hb<12; M Hb<13).
ANEMIA_CLASS = 1


@dataclass
class Prediction:
    label: str            # "anemia" / "non_anemia"
    confidence: float     # keyakinan terhadap label yang dipilih, 0..1
    p_anemia: float
    threshold: float


class ColorClassifier:
    def __init__(self, bundle_path: Path, candidate: str):
        # sklearn membuang _sklearn_version saat memuat; versi asal hanya terlihat dari peringatannya.
        with warnings.catch_warnings(record=True) as caught:
            warnings.simplefilter("always")
            bundle = joblib.load(bundle_path)
        trained = {getattr(w.message, "original_sklearn_version", None) for w in caught} - {None}
        if candidate not in bundle["classifiers"]:
            raise KeyError(f"Kandidat {candidate!r} tidak ada; pilihan: {list(bundle['classifiers'])}")
        c = bundle["classifiers"][candidate]
        self.candidate = candidate
        self.model = c["model"]
        self.feature_names: list[str] = list(c["feature_names"])
        self.threshold = float(c["threshold_exploratory"])
        self.feature_version = bundle.get("feature_extraction")
        self.research_only = bool(bundle.get("research_only", True))
        self.warning = bundle.get("warning")
        self.db_name, self.db_version = DB_MODELS.get(candidate, (candidate, "unknown"))
        classes = list(self.model.classes_)
        if ANEMIA_CLASS not in classes:
            raise ValueError(f"Kelas {ANEMIA_CLASS} tidak ada di model: {classes}")
        self._idx = classes.index(ANEMIA_CLASS)
        # Pickle sklearn hanya aman dipakai dengan versi yang sama saat model dibuat.
        self.trained_sklearn = next(iter(trained), sklearn.__version__)
        self.version_warning = None
        if self.trained_sklearn and self.trained_sklearn != sklearn.__version__:
            self.version_warning = (f"Model dibuat dengan scikit-learn {self.trained_sklearn}, server memakai "
                                    f"{sklearn.__version__}. Pasang scikit-learn=={self.trained_sklearn}.")

    def predict(self, features: dict[str, float]) -> Prediction:
        # Fitur yang tidak ada -> NaN; SimpleImputer di pipeline mengisinya dengan median data latih.
        x = np.array([[features.get(n, math.nan) for n in self.feature_names]], dtype=np.float64)
        p = float(self.model.predict_proba(x)[0, self._idx])
        anemia = p >= self.threshold
        return Prediction(
            label="anemia" if anemia else "non_anemia",
            confidence=round(p if anemia else 1.0 - p, 3),
            p_anemia=round(p, 4),
            threshold=self.threshold,
        )
