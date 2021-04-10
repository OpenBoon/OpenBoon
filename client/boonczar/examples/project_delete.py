#!/usr/bin/env python3
import argparse

import boonsdk
import boonczar

app = boonsdk.app_from_env()
admin = boonczar.from_app(app)


def main():
    parser = argparse.ArgumentParser(description='Delete Project')
    parser.add_argument('id', metavar='PROJECT_ID', help='The project id')
    args = parser.parse_args()

    admin.projects.delete_project(args.id)


if __name__ == '__main__':
    main()
