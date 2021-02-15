#!/usr/bin/env python3
import argparse

from boonsdk import app_from_env


def main():
    parser = argparse.ArgumentParser(description='Data Source')

    parser.add_argument('-c', '--create', nargs=2,
                        help="a DS name and url, like gs://zorroa-dev-data")
    parser.add_argument('-m', '--module', action='append',
                        help='Modules to assign to the DS')
    parser.add_argument('-t', '--types', action='append',
                        choices=['images', 'videos', 'documents'],
                        help='File extensions to import')

    parser.add_argument('-a', '--creds', action='append', help='Creds blob name')
    parser.add_argument('-b', '--batch-size', help='The batch size', default=25, type=int)

    args = parser.parse_args()

    app = app_from_env()
    ds = app.datasource.create_datasource(args.create[0],
                                          args.create[1],
                                          args.module,
                                          file_types=args.types,
                                          credentials=args.creds or None)
    app.datasource.import_files(ds, int(args.batch_size))


if __name__ == '__main__':
    main()
