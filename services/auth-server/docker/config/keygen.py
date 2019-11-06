#!/usr/bin/env python3

import json
import time
import uuid
import hashlib
import sys

data = str(uuid.uuid4()) + str(time.time())
m = hashlib.sha512()
m.update(data.encode("utf-8"))
shared_key = m.hexdigest()

# This isn't the actual API key, its a key spec for creating
# the API key.
key_spec = {
    "name": "admin-key",
    "projectId": str(uuid.uuid4()),
    "keyId": str(uuid.uuid4()),
    "sharedKey": shared_key,
    "permissions": ["SuperAdmin"]
}

with open(sys.argv[1], "w") as fp:
    json.dump(key_spec, fp, indent=4)
