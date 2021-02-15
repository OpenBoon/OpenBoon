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
    parser.add_argument('size', metavar='SIZE', help='The predicted index size')
    args = parser.parse_args()

    project = admin.projects.get_project(args.id)
    print(project.name)
    print(app.client.server)
    print(zmlp_admin.IndexSize[args.size.upper()])
    print("Continue?")
    _ = input()

    task = admin.indexes.migrate_project_index(project,
                                               "english_strict", args.version,
                                               size=zmlp_admin.IndexSize[args.size.upper()])
    pprint.pprint(task._data)


if __name__ == '__main__':
    main()
