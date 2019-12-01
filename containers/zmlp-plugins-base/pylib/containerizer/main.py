import argparse
import logging
import os

from .daemon import PixmlContainerDaemon

logger = logging.getLogger(__file__)


def main():
    parser = argparse.ArgumentParser(prog='containerized')
    parser.add_argument("-a", "--analyst",
                        help="The Analyst URI to connect to in ZMQ format.")

    args = parser.parse_args()
    host = args.analyst or os.environ.get("ZMLP_EVENT_HOST")

    logging.basicConfig(level=logging.INFO)

    server = PixmlContainerDaemon(host)
    server.start()
