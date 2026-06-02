#!/usr/bin/env python3
import base64
import os
import sys
import urllib.error
import urllib.request

username = os.getenv("CENTRAL_PORTAL_USERNAME", "")
password = os.getenv("CENTRAL_PORTAL_PASSWORD", "")

if not username or not password:
    print("Missing CENTRAL_PORTAL_USERNAME or CENTRAL_PORTAL_PASSWORD")
    sys.exit(1)

token = base64.b64encode(f"{username}:{password}".encode()).decode()
url = "https://central.sonatype.com/api/v1/publisher/deployments?maxResultSize=1"
request = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})

try:
    with urllib.request.urlopen(request, timeout=15) as response:
        print(f"HTTP {response.status}: Central Portal auth OK")
except urllib.error.HTTPError as error:
    if error.code in (401, 403):
        print(f"Central Portal auth failed: HTTP {error.code} (invalid token)")
        sys.exit(1)
    print(f"HTTP {error.code}: Central Portal auth OK (server accepted credentials)")
except Exception as error:
    print(f"Central Portal auth failed: {error}")
    sys.exit(1)