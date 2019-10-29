import argparse
import logging
import json

from .runner import ZpsRunner

logger = logging.getLogger(__file__)

def main():

    parser = argparse.ArgumentParser(prog='zpsrun')
    parser.add_argument("processor", help="The processor to execute.")
    parser.add_argument("-i", "--image",
                        help="A docker image to execute in, otherwise run locally")
    parser.add_argument("-a", "--args", help="Json formatting arg string")
    parser.add_argument("-d", "--data", help="A data object to process")

    args = parser.parse_args()
    if args.data:
        if args.data.startswith("@"):
            with open(args.data[1:]) as fp:
                data = json.load(fp)
        else:
            data = json.loads(args.data)
    else:
        data = None

    if not args.image:
        logging.basicConfig(level=logging.DEBUG)

    runner = ZpsRunner(args.processor, args.args, args.image, data)
    runner.run()
