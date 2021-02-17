#!/usr/bin/env python3
"""Tool for adding additional analysts to docker-compose"""
import subprocess

cmd = [
    "docker",
    "run",
    "-d",
    "-v", "/tmp:/tmp",
    "-v", "/var/run/docker.sock:/var/run/docker.sock",
    "--env", "ANALYST_DOCKER_PULL=false",
    "--env", "ANALYST_SHAREDKEY=8248911982254469B56C849A29CE0E0F",
    "--env", "BOONAI_SERVER=http://archivist:8080",
    "--env", "OFFICER_URL=http://officer:7078",
    "--network", "zmlp_default",
    "zmlp/analyst:latest"
]

subprocess.call(cmd, shell=False)
