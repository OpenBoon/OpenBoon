#!/usr/bin/env python3

import zmlp
import sys

app = zmlp.app_from_env()
print(app.assets.get_sim_hashes(sys.argv[1]))
