import argparse
import logging

from . import project, index


def main():
    # create the top-level parser
    parser = argparse.ArgumentParser(prog='bz')
    parser.add_argument('-v', '--verbose', action='store_true', help='verbose logging')
    parser.set_defaults(func=lambda args: parser.print_help())
    subparsers = parser.add_subparsers(title="management domain", dest='command')

    # Add sub commands to the parser.
    # See README for instructions on creating new subcommands.
    project.add_subparser(subparsers)
    index.add_subparser(subparsers)

    # Parse the args and set up the archivist client.
    args = parser.parse_args()
    if args.verbose:
        logging.basicConfig(level=logging.DEBUG)

    args.func(args)
