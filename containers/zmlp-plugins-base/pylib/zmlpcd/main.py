import argparse
import logging
import os

from .daemon import ZmlpContainerDaemon

logger = logging.getLogger(__file__)


def main():
    parser = argparse.ArgumentParser(prog='containerized')
    parser.add_argument("-p", "--port",
                        help="The TCP port to open and listen for connections on", default="5001")

    args = parser.parse_args()
    port = int(args.port)

    if os.environ.get("ZMLP_DEBUG"):
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    server = ZmlpContainerDaemon(port)
    server.start()
