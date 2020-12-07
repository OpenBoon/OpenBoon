#!/usr/bin/env python3
import os
import sys
import traceback
import requests

port = os.environ.get("ANALYST_PORT", "5000")


def main():
    try:
        prestop()
    except Exception as e:
        print("Unexpected exception while waiting for analyst to idle: {}".format(e))
        traceback.print_exc()

    print("Exiting prestop")
    sys.exit()


def prestop():
    """
    Check to see if the local analyst is idle.

    Returns:
        bool: True if the analyst is idle.
    """
    status = requests.get(
        "http://localhost:{}/prestop".format(port)).json()
    print(status)
    # If the idle key doesn't exist for some reason then just assume true
    return status.get('exit', True)


if __name__ == '__main__':
    main()
