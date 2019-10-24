#!/usr/bin/env python

import argparse
import json
import time

ZpsHeader = "######## BEGIN ########"
ZpsFooter = "######## END ##########"


def main():
    parser = argparse.ArgumentParser(prog='faux_go')
    parser.add_argument("-z", "--script",
                        help="The location of the ZPS script to run. "
                             "If -z and -u are omitted it will be read from STDIN")
    parser.add_argument("-d", "--shared-path", help="Root path for shared data directory.")
    parser.add_argument("--error", action='store_true', help="Emit an error")
    parser.add_argument("--expand", action='store_true', help="Emit an expand")
    parser.add_argument("--sleep", help="Sleep for X seconds")
    parser.add_argument("-v", action='store_true', help="Verbose")

    args = parser.parse_args()

    if args.error:
        print_error()

    if args.expand:
        print_expand()

    if args.sleep:
        print "SLEEPING"
        time.sleep(int(args.sleep))


def print_expand():
    data = {
        "type": "expand",
        "payload": {
            "over": [
                {"id": "abc123", "document": {"name": "gandalf"}},
                {"id": "abc123", "document": {"name": "bilbo"}}
            ]
        }
    }
    print ZpsHeader
    print json.dumps(data)
    print ZpsFooter


def print_error():
    data = {
        "type": "error",
        "payload": {
            "message": "The shit was broken",
            "processor": "zplugins.core.document.PyGroupProcessor",
            "path": "/foo/bar.jpg",
            "fatal": False,
            "id": "ABC123"
        }
    }
    print ZpsHeader
    print json.dumps(data)
    print ZpsFooter


if __name__ == "__main__":
    main()
