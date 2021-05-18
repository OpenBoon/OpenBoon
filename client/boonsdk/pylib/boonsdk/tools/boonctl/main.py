import argparse
import logging

from . import field, model, asset, dataset


def main():
    # create the top-level parser
    parser = argparse.ArgumentParser(prog='bt')
    parser.add_argument('-v', '--verbose', action='store_true', help='verbose logging')
    parser.set_defaults(func=lambda args: parser.print_help())
    subparsers = parser.add_subparsers(title="management domain", dest='command')

    # Add sub commands to the parser.
    # See README for instructions on creating new subcommands.
    field.add_subparser(subparsers)
    model.add_subparser(subparsers)
    asset.add_subparser(subparsers)
    dataset.add_subparser(subparsers)

    # Parse the args and set up the archivist client.
    args = parser.parse_args()
    if args.verbose:
        logging.basicConfig(level=logging.DEBUG)

    args.func(args)
