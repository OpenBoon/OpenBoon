#!/usr/bin/env python3
import sys
import subprocess

build_order = [
    ("containers/zmlp-py3-base", "zmlp-py3-base"),
    ("sdk/python", "zmlp-py3-sdk"),
    ("containers/zmlp-plugins-base", "zmlp-plugins-base"),
    ("containers/zmlp-plugins-core", "zmlp-plugins-core"),
    ("containers/zmlp-plugins-analysis", "zmlp-plugins-analysis")
]

images = []
if len(sys.argv) > 1:
    images = sys.argv[1:]

for directory, tag in build_order:
    if not images or tag in images:
        cmd = ['docker', 'build', "--no-cache", ".", "-t", tag]
        subprocess.check_call(cmd, shell=False, cwd=directory)
