import argparse
import json
import logging

from .runner import ZpsRunner
from .runner import ZpsTestRunner

logger = logging.getLogger(__file__)


def main():
    parser = argparse.ArgumentParser(prog='zpsdebug')
    parser.add_argument("processor", help="The processor to execute.")
    parser.add_argument("-t", "--testing_directory", help="Run unit tests in this container directory")
    parser.add_argument("-i", "--image",
                        help="A docker image to execute in, otherwise run locally")
    parser.add_argument("-a", "--args", help="Json formatting arg string")
    parser.add_argument("-d", "--data", help="A data object to process")

    args = parser.parse_args()

    # Usage when running tests in a container directory:
    # zpsdebug pytest -t /zps/pylib/zplugins -i plugins-py3-analysis
    if args.testing_directory and args.image:
        runner = ZpsTestRunner(args.processor, args.testing_directory, args.image)
        runner.run_in_container()

    else:
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

        print(args.processor)
        print(args.args)


        runner = ZpsRunner(args.processor, args.args, args.image, data)
        runner.run()
