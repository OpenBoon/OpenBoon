import argparse

from .logs import setup_logging
from .daemon import ZmlpContainerDaemon

setup_logging()


def main():
    parser = argparse.ArgumentParser(prog='containerized')
    parser.add_argument("-p", "--port",
                        help="The TCP port to open and listen for connections on", default="5001")

    args = parser.parse_args()
    port = int(args.port)

    server = ZmlpContainerDaemon(port)
    server.start()


