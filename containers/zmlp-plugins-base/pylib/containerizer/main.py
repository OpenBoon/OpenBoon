import argparse
import logging

from .daemon import PixmlContainerDaemon

logger = logging.getLogger(__file__)
logging.basicConfig(level=logging.INFO)


def main():
    parser = argparse.ArgumentParser(prog='containerized')
    parser.add_argument("-p", "--port",
                        help="The TCP port to open and listen for connections on", default="5001")

    args = parser.parse_args()
    port = int(args.port)

    server = PixmlContainerDaemon(port)
    server.start()
