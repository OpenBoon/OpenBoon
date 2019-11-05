import argparse
import json
import logging

from .runner import ZpsRunner
from .runner import ZpsTestRunner

logger = logging.getLogger(__file__)


def main():
    parser = argparse.ArgumentParser(prog='zpsdebug')
    parser.add_argument("processor", help="The processor to execute.")
    parser.add_argument("-d", "--test-dir",
                        help="Run unit tests in this container directory")
    parser.add_argument("-i", "--image",
                        help="A docker image to execute in, otherwise run locally")
    parser.add_argument("-a", "--args", help="Json formatting arg string")
    parser.add_argument("-o", "--data-obj", help="A data object to process")

    args = parser.parse_args()

    # Usage when running tests in a container directory:
    # zpsdebug pytest -d /zps/pylib/zplugins -i plugins-py3-analysis
    if args.test_dir and args.image:
        runner = ZpsTestRunner(args.processor, args.test_dir, args.image)
        runner.run_in_container()

    else:
        if args.data_obj:
            if args.data_obj.startswith("@"):
                with open(args.data_obj[1:]) as fp:
                    data = json.load(fp)
            else:
                data = json.loads(args.data_obj)
        else:
            data = None

        if not args.image:
            logging.basicConfig(level=logging.DEBUG)

        print(args.processor)
        print(args.args)

        runner = ZpsRunner(args.processor, args.args, args.image, data)
        runner.run()
