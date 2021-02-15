#!/usr/bin/env python3

import boonsdk
import sys

app = boonsdk.app_from_env()
print(app.assets.get_sim_hashes(sys.argv[1]))
