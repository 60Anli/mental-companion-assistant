import imaplib
import json
import os
import ssl
import sys
import time
import uuid
from datetime import datetime
from urllib import error, request


def env(name, default=None):
    value = os.environ.get(name)
    return default if value is None or value == "" else value


BASE_URL = env("BACKEND_BASE_URL", "http://localhost:8080").rstrip("/")
ADMIN_USERNAME = env("ADMIN_USERNAME", "admin")
ADMIN_PASSWORD = env("ADMIN_PASSWORD", "admin123")
RECEIVER = env("EMAIL_RECEIVER")
COUNT = int(env("EMAIL_TEST_COUNT", "5"))
INTERVAL_SECONDS = float(env("EMAIL_TEST_INTERVAL_SECONDS", "1"))
DELIVERY_WAIT_SECONDS = int(env("EMAIL_DELIVERY_WAIT_SECONDS", "20"))

IMAP_HOST = env("EMAIL_IMAP_HOST")
IMAP_PORT = int(env("EMAIL_IMAP_PORT", "993"))
IMAP_USERNAME = env("EMAIL_IMAP_USERNAME")
IMAP_PASSWORD = env("EMAIL_IMAP_PASSWORD")
IMAP_MAILBOX = env("EMAIL_IMAP_MAILBOX", "INBOX")


def post_json(path, payload, token=None):
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = request.Request(f"{BASE_URL}{path}", data=body, headers=headers, method="POST")
    with request.urlopen(req, timeout=20) as resp:
        return json.loads(resp.read().decode("utf-8"))


def get_json(path, token):
    req = request.Request(
        f"{BASE_URL}{path}",
        headers={"Authorization": f"Bearer {token}"},
        method="GET",
    )
    with request.urlopen(req, timeout=20) as resp:
        return json.loads(resp.read().decode("utf-8"))


def login():
    data = post_json("/api/auth/login", {
        "username": ADMIN_USERNAME,
        "password": ADMIN_PASSWORD,
    })
    if not data.get("success"):
        raise RuntimeError(f"login failed: {data}")
    return data["data"]["token"]


def send_probe_email(token, receiver, subject, content):
    data = post_json("/api/admin/email/test", {
        "receiver": receiver,
        "subject": subject,
        "content": content,
    }, token)
    if not data.get("success"):
        return False
    return bool(data.get("data"))


def fetch_app_statuses(token, subjects):
    data = get_json("/api/admin/email/logs", token)
    if not data.get("success"):
        return {}
    wanted = set(subjects)
    statuses = {}
    for item in data.get("data", []):
        subject = item.get("subject")
        if subject in wanted and subject not in statuses:
            statuses[subject] = item.get("sendStatus")
    return statuses


def imap_enabled():
    return all([IMAP_HOST, IMAP_USERNAME, IMAP_PASSWORD])


def find_subjects_in_mailbox(subjects):
    found = set()
    context = ssl.create_default_context()
    with imaplib.IMAP4_SSL(IMAP_HOST, IMAP_PORT, ssl_context=context) as client:
        client.login(IMAP_USERNAME, IMAP_PASSWORD)
        client.select(IMAP_MAILBOX)
        for subject in subjects:
            token = subject.split()[-1]
            status, payload = client.search(None, "TEXT", f'"{token}"')
            if status == "OK" and payload and payload[0].split():
                found.add(subject)
        client.logout()
    return found


def percent(numerator, denominator):
    if denominator == 0:
        return "0.00%"
    return f"{numerator / denominator * 100:.2f}%"


def main():
    if not RECEIVER:
        print("Missing EMAIL_RECEIVER. Example: $env:EMAIL_RECEIVER='receiver@example.com'", file=sys.stderr)
        return 2

    run_id = datetime.now().strftime("%Y%m%d%H%M%S") + "-" + uuid.uuid4().hex[:8]
    token = login()
    subjects = []
    accepted = []

    print(f"Backend: {BASE_URL}")
    print(f"Receiver: {RECEIVER}")
    print(f"Probe count: {COUNT}")
    print(f"Run id: {run_id}")

    for index in range(1, COUNT + 1):
        subject = f"[MindCare Delivery Probe] {run_id}-{index:03d}"
        content = (
            "This is an automated delivery probe for mental-companion-assistant.\n"
            f"RunId: {run_id}\n"
            f"Index: {index}\n"
        )
        ok = send_probe_email(token, RECEIVER, subject, content)
        subjects.append(subject)
        accepted.append(subject if ok else None)
        print(f"send {index}/{COUNT}: {'ACCEPTED' if ok else 'FAILED'} | {subject}")
        time.sleep(INTERVAL_SECONDS)

    app_statuses = fetch_app_statuses(token, subjects)
    app_success = sum(1 for subject in subjects if app_statuses.get(subject) == "SUCCESS")
    accepted_success = sum(1 for item in accepted if item)

    mailbox_found = set()
    if imap_enabled():
        print(f"Waiting {DELIVERY_WAIT_SECONDS}s before checking mailbox...")
        time.sleep(DELIVERY_WAIT_SECONDS)
        mailbox_found = find_subjects_in_mailbox(subjects)
    else:
        print("IMAP check skipped. Set EMAIL_IMAP_HOST, EMAIL_IMAP_USERNAME and EMAIL_IMAP_PASSWORD to verify inbox delivery.")

    print("\nSummary")
    print(f"API accepted: {accepted_success}/{COUNT} ({percent(accepted_success, COUNT)})")
    print(f"Log SUCCESS:  {app_success}/{COUNT} ({percent(app_success, COUNT)})")
    if imap_enabled():
        print(f"Inbox found:  {len(mailbox_found)}/{COUNT} ({percent(len(mailbox_found), COUNT)})")

    print("\nDetails")
    for subject in subjects:
        print(json.dumps({
            "subject": subject,
            "appStatus": app_statuses.get(subject, "MISSING_LOG"),
            "inboxFound": subject in mailbox_found if imap_enabled() else None,
        }, ensure_ascii=False))

    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except error.HTTPError as ex:
        print(f"HTTP error: {ex.code} {ex.read().decode('utf-8', errors='ignore')}", file=sys.stderr)
        raise SystemExit(1)
    except Exception as ex:
        print(f"Probe failed: {ex}", file=sys.stderr)
        raise SystemExit(1)
