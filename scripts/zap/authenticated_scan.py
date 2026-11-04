"""Directed authenticated GET coverage through ZAP; bootstrap writes are separate.

Runs inside the official ZAP image. Credentials, cookies and CSRF tokens are
ephemeral and redacted from all exported ZAP reports. No active scanner/spider.
"""
import csv
import html
import json
import os
from pathlib import Path
import secrets
import subprocess
import sys
import tempfile
import time
from datetime import datetime, timezone
from urllib.parse import quote, urlsplit

import requests


TARGET = os.environ.get("ZAP_TARGET", "").rstrip("/")
parsed = urlsplit(TARGET)
if (parsed.scheme != "http" or parsed.hostname != "host.docker.internal"
        or parsed.path or parsed.query or parsed.fragment or parsed.username
        or not parsed.port or parsed.port < 1024):
    raise SystemExit("Only the isolated HTTP application on host.docker.internal is allowed.")

OUT = Path("/zap/wrk")
PROXY = "http://127.0.0.1:8090"
KEY = secrets.token_urlsafe(32)
SENSITIVE = {KEY, os.environ.get("LAB_ADMIN_PASSWORD", "")}
COVERAGE = []
SESSIONS = []
PHASE = "bootstrap"


def redact(value):
    if isinstance(value, dict):
        return {key: redact(item) for key, item in value.items()}
    if isinstance(value, list):
        return [redact(item) for item in value]
    if isinstance(value, str):
        for secret in sorted((s for s in SENSITIVE if s), key=len, reverse=True):
            value = value.replace(secret, "[REDACTED]").replace(quote(secret, safe=""), "[REDACTED]")
        return value
    return value


def zap(component, kind, operation, **params):
    # Never use the Windows/system proxy for the loopback control API.
    with requests.Session() as control:
        control.trust_env = False
        response = control.get(f"{PROXY}/{kind}/{component}/{operation}/",
                               params={"apikey": KEY, **params}, timeout=30)
        response.raise_for_status()
        if kind == "OTHER":
            return response.text
        data = response.json()
        if "code" in data:
            raise RuntimeError(f"ZAP API failed: {component}/{operation}: {data['code']}")
        return data


def api(component, operation, action=False, **params):
    return zap(component, "JSON", ("action/" if action else "view/") + operation, **params)


def session():
    result = requests.Session()
    result.trust_env = False
    result.proxies = {"http": PROXY, "https": PROXY} if PHASE == "scan" else {}
    SESSIONS.append(result)
    return result


def request(client, method, path, expected=200, **kwargs):
    if not path.startswith("/") or path.startswith("//"):
        raise RuntimeError("A request escaped the fixed laboratory target.")
    if PHASE == "scan" and method != "GET":
        raise RuntimeError("Only GET requests are allowed in the scan phase.")
    SENSITIVE.update(cookie.value for cookie in client.cookies)
    response = client.request(method, TARGET + path, allow_redirects=False, timeout=30, **kwargs)
    SENSITIVE.update(cookie.value for cookie in client.cookies)
    if expected is not None and response.status_code != expected:
        raise RuntimeError(f"{method} {path}: expected HTTP {expected}, received {response.status_code}")
    return response


def write(client, method, path, body, expected):
    token = request(client, "GET", "/api/auth/csrf").json()
    SENSITIVE.add(token["token"])
    return request(client, method, path, expected, json=body,
                   headers={token["headerName"]: token["token"]}).json()


def login(username, password, role):
    SENSITIVE.add(password)
    client = session()
    token = request(client, "GET", "/api/auth/csrf").json()
    SENSITIVE.add(token["token"])
    # Spring Security expects form parameters. The CSRF token is sent as a header.
    request(client, "POST", "/api/auth/login", data={"username": username, "password": password},
            headers={token["headerName"]: token["token"]})
    profile = request(client, "GET", "/api/auth/me").json()
    if profile.get("rol") != role or profile.get("username") != username:
        raise RuntimeError(f"The authenticated profile did not match {role}.")
    return client, profile


def bootstrap():
    global PHASE
    # Bootstrap is intentionally not proxied through ZAP: writes and credentials
    # are setup traffic, not read-only coverage or report evidence.
    for client in SESSIONS:
        client.proxies = {}
    admin, _ = login(os.environ["LAB_ADMIN_USERNAME"], os.environ["LAB_ADMIN_PASSWORD"], "ADMIN")
    admin.proxies = {}
    services = []
    for suffix in ("A", "B"):
        services.append(write(admin, "POST", "/servicios", {
            "nombre": f"Servicio ZAP {suffix}", "descripcion": "Dato ficticio de laboratorio",
            "estado": "ACTIVO"}, 201))
    actors = {}
    turns = []
    staff = []
    for index, suffix in enumerate(("a", "b")):
        password = secrets.token_urlsafe(24)
        SENSITIVE.add(password)
        # Registration creates both the user record and its login account.
        token = request(admin, "GET", "/api/auth/csrf").json()
        SENSITIVE.add(token["token"])
        request(admin, "POST", "/api/auth/registro", 201,
                json={"username": f"zapuser-{suffix}", "password": password,
                      "nombre": f"Persona ficticia ZAP {suffix}", "documento": f"ZAP-{suffix}"},
                headers={token["headerName"]: token["token"]})
        client, profile = login(f"zapuser-{suffix}", password, "USUARIO")
        client.proxies = {}
        turn = write(client, "POST", "/api/mi/turnos", {"servicioId": services[index]["id"]}, 201)
        turns.append(turn)
        actors[f"USUARIO_{suffix.upper()}"] = (client, profile)
        worker = write(admin, "POST", "/funcionarios", {
            "nombre": f"Funcionario ficticio ZAP {suffix}", "servicioId": services[index]["id"]}, 201)
        staff.append(worker)
        worker_password = secrets.token_urlsafe(24)
        SENSITIVE.add(worker_password)
        # This endpoint returns an empty 201 response.
        token = request(admin, "GET", "/api/auth/csrf").json()
        SENSITIVE.add(token["token"])
        request(admin, "POST", "/api/admin/cuentas-funcionarios", 201,
                json={"username": f"zapstaff-{suffix}", "password": worker_password,
                      "funcionarioId": worker["id"]},
                headers={token["headerName"]: token["token"]})
        worker_client, worker_profile = login(f"zapstaff-{suffix}", worker_password, "FUNCIONARIO")
        worker_client.proxies = {}
        actors[f"FUNCIONARIO_{suffix.upper()}"] = (worker_client, worker_profile)
        write(admin, "PATCH", f"/turnos/{turn['id']}/llamar", {"funcionarioId": worker["id"]}, 200)
    actors["ADMIN"] = (admin, request(admin, "GET", "/api/auth/me").json())
    # Add an unauthenticated client to compare access to protected resources.
    actors["ANONIMO"] = (session(), None)
    for client, _ in actors.values():
        client.proxies = {"http": PROXY, "https": PROXY}
    PHASE = "scan"
    return actors, services, turns, staff


def check(client, role, path, expected=200, predicate=None, note="Acceso esperado"):
    response = request(client, "GET", path, None)
    passed = response.status_code == expected and (predicate is None or bool(predicate(response.json())))
    COVERAGE.append({"role": role, "method": "GET", "path": path,
                     "expected": expected, "actual": response.status_code,
                     "verified": passed, "check": note})
    if not passed:
        raise RuntimeError(f"{role} GET {path}: expected HTTP {expected} and seeded data, received {response.status_code}.")
    return response


def scan(actors, services, turns, staff):
    admin = actors["ADMIN"][0]
    service_id, turn_id, other_turn = services[0]["id"], turns[0]["id"], turns[1]["id"]
    worker_id, other_worker = staff[0]["id"], staff[1]["id"]
    user_id = actors["USUARIO_A"][1]["usuarioId"]
    for role in ("ADMIN", "USUARIO_A", "FUNCIONARIO_A"):
        client, profile = actors[role]
        check(client, role, "/api/auth/me", predicate=lambda data, p=profile: data == p,
              note="Identidad y rol confirmados")
        for path in ("/", "/login.html", "/app.js", "/auth.js", "/login.js", "/styles.css", "/auth.css"):
            check(client, role, path)
        check(client, role, "/servicios", predicate=lambda rows: service_id in [row["id"] for row in rows])
        check(client, role, f"/servicios/{service_id}", predicate=lambda row: row["id"] == service_id)
    for path in ("/usuarios", f"/usuarios/{user_id}", "/funcionarios", f"/funcionarios/{worker_id}",
                 "/turnos", f"/turnos?servicioId={service_id}", f"/turnos/{turn_id}",
                 f"/turnos/numero/{turns[0]['numero']}", "/turnos/pendientes",
                 f"/turnos/funcionario/{worker_id}", "/api/admin/cuentas"):
        check(admin, "ADMIN", path)
    user = actors["USUARIO_A"][0]
    check(user, "USUARIO_A", "/api/mi/turnos",
          predicate=lambda rows: [row["id"] for row in rows] == [turn_id],
          note="Solo devuelve el turno del usuario A, no el del usuario B")
    for path in ("/usuarios", "/funcionarios", "/turnos", "/api/admin/cuentas"):
        check(user, "USUARIO_A", path, 403, note="Deniega rutas de otro rol")
    worker = actors["FUNCIONARIO_A"][0]
    for path in ("/turnos", "/turnos/pendientes", f"/turnos/funcionario/{worker_id}"):
        check(worker, "FUNCIONARIO_A", path,
              predicate=lambda rows: [row["id"] for row in rows] == [turn_id],
              note="Solo turnos del servicio/funcionario A")
    for path in (f"/turnos/{turn_id}", f"/turnos/numero/{turns[0]['numero']}",
                 "/funcionarios", f"/funcionarios/{worker_id}"):
        check(worker, "FUNCIONARIO_A", path)
    for path in (f"/turnos/{other_turn}", f"/turnos/numero/{turns[1]['numero']}",
                 f"/funcionarios/{other_worker}", f"/turnos/funcionario/{other_worker}",
                 "/usuarios", "/api/admin/cuentas"):
        check(worker, "FUNCIONARIO_A", path, 403, note="Deniega datos de otro servicio/rol")
    check(admin, "ADMIN", "/api/mi/turnos", 403, note="Ruta reservada al rol USUARIO")
    anonymous = actors["ANONIMO"][0]
    for path in ("/api/auth/me", "/api/mi/turnos", "/servicios", "/usuarios", "/funcionarios", "/turnos", "/api/admin/cuentas"):
        check(anonymous, "ANONIMO", path, 401, note="Requiere autenticacion")


def save_coverage():
    with (OUT / "cobertura.csv").open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["role", "method", "path", "expected", "actual", "verified", "check"])
        writer.writeheader()
        writer.writerows(COVERAGE)


def reports(version):
    deadline = time.monotonic() + 180
    while int(api("pscan", "recordsToScan")["recordsToScan"]):
        if time.monotonic() > deadline:
            raise RuntimeError("The passive scan queue did not finish within 180 seconds.")
        time.sleep(2)
    report = json.loads(zap("core", "OTHER", "other/jsonreport"))
    if any(site.get("@name", "").rstrip("/") != TARGET for site in report.get("site", [])):
        raise RuntimeError("ZAP included a site outside the fixed laboratory target.")
    report["laboratorio"] = {
        "buildId": os.environ.get("BUILD_ID", "local"), "commit": os.environ.get("BUILD_COMMIT", "local"),
        "pipelineUrl": os.environ.get("BUILD_URL", ""), "zapVersion": version,
        "generatedUtc": datetime.now(timezone.utc).isoformat(),
        "method": "Consultas GET dirigidas por rol a traves del proxy ZAP con analisis pasivo",
        "activeScan": False, "bootstrapWrites": "Solo preparacion de H2 antes del analisis",
        "scope": "Rutas previstas en el codigo; no garantiza ausencia de vulnerabilidades ni cobertura exhaustiva",
        "coverage": COVERAGE}
    report = redact(report)
    (OUT / "reporte-zap-autenticado.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    # Use ZAP's HTML renderer; redact secrets, including HTML-escaped forms.
    rendered = zap("core", "OTHER", "other/htmlreport")
    for secret in sorted((s for s in SENSITIVE if s), key=len, reverse=True):
        rendered = rendered.replace(secret, "[REDACTED]").replace(html.escape(secret), "[REDACTED]")
        rendered = rendered.replace(quote(secret, safe=""), "[REDACTED]")
    (OUT / "reporte-zap-autenticado.html").write_text(rendered, encoding="utf-8")
    save_coverage()
    (OUT / "alcance.json").write_text(json.dumps(report["laboratorio"], ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"ZAP {version}: {len(COVERAGE)} verificaciones GET completadas; reportes HTML/JSON generados.", flush=True)


def main():
    global PHASE
    OUT.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="zap-auth-") as tmp:
        log = open(Path(tmp) / "zap.log", "w", encoding="utf-8")
        daemon = subprocess.Popen(["zap.sh", "-daemon", "-host", "127.0.0.1", "-port", "8090",
                                   "-dir", str(Path(tmp) / "home"), "-config", f"api.key={KEY}",
                                   "-config", "autoupdate.checkOnStart=false"], stdout=log, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic() + 180
            while True:
                try:
                    version = api("core", "version")["version"]
                    break
                except requests.RequestException:
                    if daemon.poll() is not None or time.monotonic() > deadline:
                        raise RuntimeError("ZAP daemon could not start.")
                    time.sleep(2)
            # Prevent active scans; directed read-only requests still passively analyzed.
            api("core", "setMode", action=True, mode="safe")
            api("pscan", "setEnabled", action=True, enabled="false")
            actors, services, turns, staff = bootstrap()
            api("pscan", "setEnabled", action=True, enabled="true")
            scan(actors, services, turns, staff)
            reports(version)
        except Exception as error:
            save_coverage()
            message = redact(str(error))
            (OUT / "error.json").write_text(json.dumps({"error": message, "phase": PHASE}, ensure_ascii=False), encoding="utf-8")
            print(f"ERROR: {message}", file=sys.stderr, flush=True)
            return 1
        finally:
            for client in SESSIONS:
                client.close()
            daemon.terminate()
            try:
                daemon.wait(timeout=20)
            except subprocess.TimeoutExpired:
                daemon.kill()
                daemon.wait()
            log.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
