#!/usr/bin/env python3
import os
import sys
import traceback
import time
import requests

port = os.environ.get("ANALYST_PORT", "5000")


def main():
    try:
        while True:
            if prestop():
                break
            else:
                print("Waiting for analyst to be idle")
                time.sleep(10)
        retval = 0
    except Exception as e:
        print("Unexpected exception while waiting for analyst to idle: {}".format(e))
        traceback.print_exc()
        retval = 1

    print("Exiting prestop {}".format(retval))
    sys.exit(retval)


def prestop():
    """
    Check to see if the local analyst is idle.

    Returns:
        bool: True if the analyst is idle.
    """
    status = requests.get(
        "http://localhost:{}/prestop".format(port)).json()
    # If the idle key doesn't exist for some reason then just assume true
    return status.get('ok', True)


if __name__ == '__main__':
    main()
