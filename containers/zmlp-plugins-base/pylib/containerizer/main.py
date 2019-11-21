import argparse
import logging

from .server import PixmlContainerDaemon

logger = logging.getLogger(__file__)


def main():
    parser = argparse.ArgumentParser(prog='pixmld')
    parser.add_argument("-p", "--port", default=5557, help="TCP port to listen on.")

    args = parser.parse_args()

    logging.basicConfig(level=logging.DEBUG)

    server = PixmlContainerDaemon(int(args.port))
    server.start()
