#!/usr/bin/env python3
import argparse
import json

from zmlp import app_from_env, FileImport


def main():
    parser = argparse.ArgumentParser(description='Import some files remote')
    parser.add_argument('paths', metavar='N', nargs='+',
                        help='A URI to import, example https://i.imgur.com/Ly80IrC.jpg')
    parser.add_argument('-m', '--module', action='append',
                        help='Module to apply to the upload')
    parser.add_argument('-c', '--custom', action='append',
                        help='Custom metdata value to set.')

    args = parser.parse_args()

    custom = {}
    if args.custom:
       for val in args.custom:
           k,v = val.split("=")
           custom[k.strip()] = v.strip()

    assets = [FileImport(path, custom=custom) for path in args.paths]

    modules = args.module or None

    app = app_from_env()
    json.dumps(app.assets.batch_import_files(assets, modules=modules), indent=4)


if __name__ == '__main__':
    main()
