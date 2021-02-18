#!/usr/bin/env python3
import argparse
import json

from boonsdk import app_from_env, FileUpload


def main():
    parser = argparse.ArgumentParser(description='Import some files')
    parser.add_argument('paths', metavar='N', nargs='+', help='A file path to import')
    parser.add_argument('-m', '--module', action='append', help='Module to apply to the upload')

    args = parser.parse_args()

    assets = [FileUpload(path) for path in args.paths]
    modules = args.module or None

    app = app_from_env()
    print(json.dumps(app.assets.batch_upload_files(assets, modules=modules), indent=4))


if __name__ == '__main__':
    main()
