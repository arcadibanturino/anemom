#!/usr/bin/env python3
"""Uji asap seluruh endpoint API Anemom. Pemakaian: python3 smoke_test.py [BASE_URL]
Butuh: pip install requests pillow. Membuat 1 akun uji baru tiap dijalankan."""
import io, json, sys, time, uuid
import requests
from PIL import Image

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080") + "/api/v1"
fails = 0

def check(name, cond, extra=""):
    global fails
    print(("PASS " if cond else "FAIL ") + name + ("" if cond else f"  -> {extra}"))
    fails += 0 if cond else 1

def call(method, path, token=None, **kw):
    h = {"Authorization": f"Bearer {token}"} if token else {}
    return requests.request(method, BASE + path, headers=h, timeout=30, **kw)

def jpeg():
    b = io.BytesIO(); Image.new("RGB", (64, 48), (200, 80, 90)).save(b, "JPEG"); return b.getvalue()

email = f"uji{int(time.time())}@email.com"
hpht = time.strftime("%Y-%m-%d", time.gmtime(time.time() - 24 * 7 * 86400))   # ~24 minggu
birth = time.strftime("%Y-%m-%d", time.gmtime(time.time() - 28 * 365.25 * 86400))   # ~28 tahun

# ---- Akun
r = call("POST", "/auth/register", json={"name": "Rani Pratiwi", "email": email, "password": "sandi12345", "hpht": hpht, "birth_date": birth})
check("register 201", r.status_code == 201, r.text)
tok = r.json()["data"]["token"]
check("register: trimester 2 & minggu >= 23", r.json()["data"]["user"]["pregnancy"]["trimester"] == 2, r.text)
check("register email ganda 409", call("POST", "/auth/register", json={"name": "Rani", "email": email, "password": "sandi12345", "hpht": hpht, "birth_date": birth}).status_code == 409)
check("register validasi 422 (nama 1 huruf)", call("POST", "/auth/register", json={"name": "R", "email": "x", "password": "1"}).status_code == 422)
check("register tanpa birth_date 422", call("POST", "/auth/register", json={"name": "Tanpa Lahir", "email": "tl" + email, "password": "sandi12345", "hpht": hpht}).status_code == 422)
check("register usia 5 tahun 422", call("POST", "/auth/register", json={"name": "Anak Kecil", "email": "ak" + email, "password": "sandi12345", "hpht": hpht, "birth_date": time.strftime("%Y-%m-%d", time.gmtime(time.time() - 5 * 365.25 * 86400))}).status_code == 422)
check("register tanpa telepon 201", call("POST", "/auth/register", json={"name": "Bu Ani", "email": "ani" + email, "password": "sandi12345", "hpht": hpht, "birth_date": birth}).status_code == 201)
check("login salah 401", call("POST", "/auth/login", json={"email": email, "password": "salah-salah"}).status_code == 401)
r = call("POST", "/auth/login", json={"email": email, "password": "sandi12345", "device_name": "Pixel"})
check("login 200", r.status_code == 200, r.text)
tok = r.json()["data"]["token"]
check("tanpa token 401", call("GET", "/me").status_code == 401)
check("GET /me", call("GET", "/me", tok).json()["data"]["email"] == email)
r = call("PUT", "/me", tok, json={"health_facility": "Puskesmas Tamalanrea", "birth_date": "1998-05-10"})
check("PUT /me", r.json()["data"]["profile"]["health_facility"] == "Puskesmas Tamalanrea", r.text)
r = call("PUT", "/me", tok, json={"name": "Rani", "email": "baru" + email, "birth_date": "1998-05-10", "hpht": hpht, "health_facility": ""})
check("PUT /me ubah email + kosongkan fasilitas", r.status_code == 200 and r.json()["data"]["email"] == "baru" + email and r.json()["data"]["profile"]["health_facility"] is None, r.text)
check("PUT /me email sudah dipakai 409", call("PUT", "/me", tok, json={"email": "ani" + email}).status_code == 409)
check("PUT /me usia 70 tahun 422", call("PUT", "/me", tok, json={"birth_date": "1950-01-01"}).status_code == 422)
r = call("PUT", "/me", tok, json={"email": email})   # kembalikan agar tes berikutnya yang memakai email tetap jalan
check("PUT /me kembalikan email", r.status_code == 200 and r.json()["data"]["email"] == email, r.text)

# ---- Hb (+ idempotensi + klasifikasi)
u1 = str(uuid.uuid4())
today = time.strftime("%Y-%m-%d")
r = call("POST", "/hb-results", tok, json={"hb_value": "10,8", "test_date": today, "facility": "Puskesmas", "client_uuid": u1})
check("Hb 10,8 -> moderate 201", r.status_code == 201 and r.json()["data"]["risk"] == "moderate", r.text)
r2 = call("POST", "/hb-results", tok, json={"hb_value": 10.8, "test_date": today, "client_uuid": u1})
check("Hb kirim ulang idempoten (200, id sama)", r2.status_code == 200 and r2.json()["data"]["id"] == r.json()["data"]["id"], r2.text)
hb_id = r.json()["data"]["id"]
check("Hb 9.5 -> high", call("POST", "/hb-results", tok, json={"hb_value": 9.5, "test_date": "2026-10-01"}).json()["data"]["risk"] == "high")
check("Hb 12 -> normal", call("POST", "/hb-results", tok, json={"hb_value": 12, "test_date": "2026-09-01"}).json()["data"]["risk"] == "normal")
check("Hb 25 -> 422", call("POST", "/hb-results", tok, json={"hb_value": 25, "test_date": today}).status_code == 422)
check("Hb tanggal masa depan 422", call("POST", "/hb-results", tok, json={"hb_value": 11, "test_date": "2099-01-01"}).status_code == 422)
lst = call("GET", "/hb-results", tok).json()
check("list Hb 3 data urut terbaru", lst["meta"]["count"] == 3 and lst["data"][0]["test_date"] == today, json.dumps(lst)[:200])

# ---- TTD
r = call("POST", "/ttd/take", tok, json={})
check("TTD take 201", r.status_code == 201 and r.json()["data"]["taken_today"] is True, r.text)
r = call("POST", "/ttd/take", tok, json={})
check("TTD take dua kali -> 200 created=false", r.status_code == 200 and r.json()["data"]["created"] is False, r.text)
call("POST", "/ttd/take", tok, json={"date": time.strftime("%Y-%m-%d", time.gmtime(time.time() - 86400)), "time": "20:05"})
r = call("GET", "/ttd", tok).json()["data"]
check("TTD bulan ini + ringkasan 7 hari = 2/7 (29%)", r["taken_last_7_days"] == 2 and r["percent_last_7_days"] == 29, json.dumps(r))
r = call("PUT", "/ttd/settings", tok, json={"reminder_on": False, "reminder_time": "19:30"}).json()["data"]
check("TTD settings", r == {"reminder_on": False, "reminder_time": "19:30"}, str(r))

# ---- Perangkat band
r = call("POST", "/devices", tok, json={"device_uid": "AA:BB:CC:DD:EE:01", "firmware_version": "0.1.0"})
check("device register 201", r.status_code == 201, r.text)
dev = r.json()["data"]["id"]
check("device register ulang 200", call("POST", "/devices", tok, json={"device_uid": "AA:BB:CC:DD:EE:01"}).status_code == 200)
check("device status baterai", call("PUT", f"/devices/{dev}/status", tok, json={"battery_pct": 80}).json()["data"]["battery_pct"] == 80)

# ---- Gambar + AI Scan (on-device)
r = call("POST", "/images", tok, data={"purpose": "food"}, files={"file": ("a.jpg", jpeg(), "image/jpeg")})
check("upload gambar 201", r.status_code == 201, r.text)
img = r.json()["data"]["id"]
check("unduh gambar milik sendiri", call("GET", f"/images/{img}/file", tok).content[:2] == b"\xff\xd8")
check("upload bukan gambar 415", call("POST", "/images", tok, data={"purpose": "eye"}, files={"file": ("a.jpg", b"bukan gambar", "image/jpeg")}).status_code == 415)
check("research_use tanpa consent 403", call("POST", "/images", tok, data={"purpose": "eye", "use_for_training": "true"}, files={"file": ("a.jpg", jpeg(), "image/jpeg")}).status_code == 403)
check("consent research_use", call("POST", "/consents", tok, json={"type": "research_use", "version": "1.0"}).status_code == 200)

u2 = str(uuid.uuid4())
res = json.dumps({"label": "anemia", "confidence": 0.87, "est_hb": 10.2, "model_version": "0.0.1"})
r = call("POST", "/screenings/eye", tok, data={"client_uuid": u2, "captured_at": time.strftime("%Y-%m-%dT%H:%M:%S+00:00"), "on_device_result": res}, files={"image": ("eye.jpg", jpeg(), "image/jpeg")})
check("AI scan on-device 201 (risk moderate)", r.status_code == 201 and r.json()["data"]["risk"] == "moderate" and r.json()["data"]["inference_source"] == "on_device", r.text)
scr = r.json()["data"]["id"]
check("AI scan idempoten", call("POST", "/screenings/eye", tok, data={"client_uuid": u2, "on_device_result": res}, files={"image": ("eye.jpg", jpeg(), "image/jpeg")}).json()["data"]["id"] == scr)
# server-side: YOLO mati -> 202 queued; YOLO hidup -> 201 done
r = call("POST", "/screenings/eye", tok, files={"image": ("eye.jpg", jpeg(), "image/jpeg")})
check("AI scan di server (202 queued / 201 done)", (r.status_code, r.json()["data"]["status"]) in [(202, "queued"), (201, "done")], r.text)
check("on_device_result tidak valid 422", call("POST", "/screenings/eye", tok, data={"on_device_result": json.dumps({"label": "x"})}, files={"image": ("eye.jpg", jpeg(), "image/jpeg")}).status_code == 422)
check("link skrining ke hasil lab", call("PUT", f"/screenings/{scr}/link-hb", tok, json={"hb_result_id": hb_id}).json()["data"]["matched_hb_result_id"] == hb_id)

# ---- PPG
u3 = str(uuid.uuid4())
r = call("POST", "/screenings/ppg", tok, data={"client_uuid": u3, "device_uid": "AA:BB:CC:DD:EE:01", "started_at": time.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "duration_s": 60, "sample_rate_hz": 100, "hr_bpm": 84, "spo2_pct": 97.5, "signal_quality": "baik", "quality_score": 0.91,
        "features": json.dumps({"ac_dc_red": 0.021, "ac_dc_ir": 0.034})}, files={"raw": ("ppg.csv", b"red,ir\n1,2\n3,4\n", "text/csv")})
check("PPG 201", r.status_code == 201 and r.json()["data"]["method"] == "ppg_band", r.text)
check("PPG JSON tanpa raw + hasil model", call("POST", "/screenings/ppg", tok, json={"started_at": "2026-11-17T04:00:00+08:00", "duration_s": 60, "sample_rate_hz": 100, "result": {"est_hb": 11.4, "confidence": 0.6, "inference_source": "server"}}).json()["data"]["risk"] == "normal")
lst = call("GET", "/screenings", tok).json()
check("list skrining (4)", lst["meta"]["count"] == 4, json.dumps(lst["meta"]))
check("filter method=ppg_band (2)", call("GET", "/screenings?method=ppg_band", tok).json()["meta"]["count"] == 2)

# ---- Bahan makanan + rekomendasi + catatan makan
check("rekomendasi: 200 (kosong sampai ada bahan is_recommended=1)", call("GET", "/recommendations", tok).status_code == 200)
now = time.strftime("%Y-%m-%dT%H:%M:%SZ")
items = call("GET", "/ingredients?q=bayam", tok).json()["data"]
if not items:
    print("SKIP katalog bahan kosong (muat database/dev_sample_ingredients.sql untuk menguji bagian ini)")
    check("catat makan kustom (tanpa katalog)", call("POST", "/food-logs", tok, json={"eaten_at": now, "meal_type": "makan_siang", "custom_name": "Nasi campur"}).status_code == 201)
else:
    check("cari bahan bayam", len(items) == 1 and items[0]["is_iron_booster"], str(items))
    r = call("POST", "/food-logs", tok, json={"eaten_at": now, "meal_type": "sarapan", "ingredient_id": items[0]["id"], "portions": 2})
    check("food-log iron = 2 x 2.71", r.status_code == 201 and abs(r.json()["data"]["iron_mg"] - 5.42) < 0.01, r.text)
    tea = call("GET", "/ingredients?q=teh", tok).json()["data"][0]["id"]
    call("POST", "/food-logs", tok, json={"eaten_at": now, "meal_type": "camilan", "ingredient_id": tea, "source": "scan", "image_id": img, "ai_label": "teh", "ai_confidence": 0.8})
    call("POST", "/food-logs", tok, json={"eaten_at": now, "meal_type": "makan_siang", "custom_name": "Nasi campur"})
    s = call("GET", f"/food-logs/summary?date={time.strftime('%Y-%m-%d')}", tok).json()["data"]
    check("ringkasan makan: 3 entri, ada penghambat & pendukung", s["entries"] == 3 and s["has_iron_inhibitor"] and s["has_iron_booster"], str(s))
    hati = call("GET", "/ingredients?q=hati", tok).json()["data"]
    check("bahan dengan peringatan kehamilan terkirim", hati and hati[0]["pregnancy_warning"] and hati[0]["is_recommended"] is False, str(hati))
check("food-log tanpa ingredient_id/custom 422", call("POST", "/food-logs", tok, json={"eaten_at": "2026-11-17T04:00:00Z", "meal_type": "sarapan"}).status_code == 422)
check("gambar bahan tanpa gambar 404 (publik)", call("GET", "/ingredients/999999/image").status_code == 404)
check("artikel draf tidak tampil", call("GET", "/articles", tok).json()["data"] == [])

# ---- Dashboard, notifikasi, isolasi data
d = call("GET", "/dashboard", tok).json()["data"]
check("dashboard: last_hb hari ini 10.8, 3 titik tren", d["last_hb"]["hb_value"] == 10.8 and len(d["hb_trend"]) == 3, json.dumps(d)[:300])
check("dashboard: arah tren (turun/naik)", d["hb_trend_direction"] in ("naik", "turun", "stabil"))
check("fcm register", call("POST", "/fcm-tokens", tok, json={"token": "x" * 40}).status_code == 201)

r = call("POST", "/auth/register", json={"name": "Dewi Lain", "email": "lain" + email, "password": "sandi12345", "hpht": hpht, "birth_date": birth})
tok2 = r.json()["data"]["token"]
check("isolasi: user lain tak bisa unduh gambar", call("GET", f"/images/{img}/file", tok2).status_code == 404)
check("isolasi: user lain tak bisa lihat Hb", call("GET", f"/hb-results/{hb_id}", tok2).status_code == 404)
check("isolasi: list Hb user lain kosong", call("GET", "/hb-results", tok2).json()["meta"]["count"] == 0)

# ---- Hapus + sinkronisasi
check("hapus Hb", call("DELETE", f"/hb-results/{hb_id}", tok).status_code == 200)
check("Hb terhapus tidak muncul", call("GET", f"/hb-results/{hb_id}", tok).status_code == 404)
sync = call("GET", "/hb-results?updated_since=2000-01-01T00:00:00Z", tok).json()["data"]
check("updated_since memuat tombstone (deleted_at terisi)", any(x["deleted_at"] for x in sync), json.dumps(sync)[:200])

# ---- Logout, ganti sandi, hapus akun
check("ganti sandi", call("PUT", "/me/password", tok, json={"current_password": "sandi12345", "new_password": "sandi-baru-99"}).status_code == 200)
check("logout", call("POST", "/auth/logout", tok).status_code == 200)
check("token setelah logout 401", call("GET", "/me", tok).status_code == 401)
tok = call("POST", "/auth/login", json={"email": email, "password": "sandi-baru-99"}).json()["data"]["token"]
check("hapus akun sandi salah 403", call("DELETE", "/me", tok, json={"password": "salah-salah"}).status_code == 403)
check("hapus akun 200", call("DELETE", "/me", tok, json={"password": "sandi-baru-99"}).status_code == 200)
check("login setelah dihapus 401", call("POST", "/auth/login", json={"email": email, "password": "sandi-baru-99"}).status_code == 401)
check("404 endpoint", call("GET", "/tidak-ada", tok2).status_code == 404)
check("405 metode", call("PATCH", "/me", tok2).status_code == 405)

print(f"\n{'SEMUA LULUS' if fails == 0 else str(fails) + ' GAGAL'}")
sys.exit(1 if fails else 0)
