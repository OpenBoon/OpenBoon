#!/usr/bin/env python3

import zmlp
import sys

app = zmlp.app_from_env()
print(app.client.upload_files("/ml/v1/sim-hash", [sys.argv[1]], body=None))
