# flake8: noqa
import argparse

from .logs import setup_logging
# Have to do this before anything.
setup_logging()

from .daemon import BoonDockDaemon


def main():
    parser = argparse.ArgumentParser(prog='containerized')
    parser.add_argument("-p", "--port",
                        help="The TCP port to open and listen for connections on", default="5001")

    args = parser.parse_args()
    port = int(args.port)

    server = BoonDockDaemon(port)
    server.start()
