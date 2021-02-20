#!/usr/bin/env python3
import argparse
import pprint

import boonsdk
import boonczar

app = boonsdk.app_from_env()
admin = boonczar.from_app(app)


def main():
    parser = argparse.ArgumentParser(description='Migrate project index')
    parser.add_argument('id', metavar='PROJECT_ID', help='The project id')
    parser.add_argument('version', metavar='VERSION', help='The mapping ver', type=int)
    parser.add_argument('-s', '--size', metavar='SIZE', help='The predicted index size')
    args = parser.parse_args()

    project = admin.projects.get_project(args.id)
    print("project: {}".format(project.name))
    print("server: {}".format(app.client.server))
    print("size: {}".format(args.size))
    print("version: {}".format(args.version))
    print("Continue?")
    _ = input()

    if args.size:
        size = boonczar.IndexSize[args.size.upper()]
    else:
        size = None

    task = admin.indexes.migrate_project_index(project,
                                               "english_strict", args.version,
                                               size=size)
    pprint.pprint(task._data)


if __name__ == '__main__':
    main()
