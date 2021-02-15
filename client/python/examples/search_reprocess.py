#!/usr/bin/env python3
import argparse

from boonsdk import app_from_env


def main():
    parser = argparse.ArgumentParser(description='Reprocess all assets of types')
    parser.add_argument('-t', '--type', action='append',
                        help='File extensions')
    parser.add_argument('modules', action='append',
                        help='Module to apply to the upload')

    args = parser.parse_args()
    modules = args.modules or None
    types = args.type or None

    if not types:
        print("Reprocessing all assets")
        query = {}
    else:
        query = {"query": {"terms": {"source.extension": types}}}

    app = app_from_env()
    print(app.assets.reprocess_search(query, modules))


if __name__ == '__main__':
    main()
