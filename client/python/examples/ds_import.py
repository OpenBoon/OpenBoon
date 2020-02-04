#!/usr/bin/env python3
import argparse

from zmlp import app_from_env


def main():
    parser = argparse.ArgumentParser(description='Data Source')

    parser.add_argument('-c', '--create', nargs=2, help="a DS name and url, like gs://zorroa-dev-data")
    parser.add_argument('-m', '--module', action='append',
                        help='Modules to assign to the DS')

    args = parser.parse_args()

    app = app_from_env()
    ds = app.datasource.create_datasource(args.create[0],
                                          args.create[1],
                                          args.module)
    app.datasource.import_files(ds)


if __name__ == '__main__':
    main()


