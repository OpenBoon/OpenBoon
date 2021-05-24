#!/usr/bin/env python3
import argparse
import pprint

from boonsdk import app_from_env

app = app_from_env()


def callback_function(files, rsp):
    print("--processed files--")
    for path in files:
        print(path)
    print("--boonai response--")
    pprint.pprint(rsp)


def main():
    parser = argparse.ArgumentParser(description='Import some files')
    parser.add_argument('paths', metavar='N', nargs='+', help='A file path to import')
    parser.add_argument('-m', '--module', action='append', help='Module to apply to the upload')
    parser.add_argument('-b', '--batch-size', type=int,
                        default=50, help='The number of assets to upload per batch')
    parser.add_argument('-n', '--max-batches', type=int,
                        default=0, help='The number of batches to upload, default to unlimited')
    parser.add_argument('-t', '--file-types', action='append',
                        help='The file types to filter by, default all supported types')

    args = parser.parse_args()
    modules = args.module or None

    for path in args.paths:
        app.assets.batch_upload_directory(path,
                                          file_types=args.file_types,
                                          modules=modules,
                                          batch_size=args.batch_size,
                                          max_batches=args.max_batches,
                                          callback=callback_function)


if __name__ == '__main__':
    main()
