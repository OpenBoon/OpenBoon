#!/usr/bin/env python3
import argparse
import pprint

from boonsdk import app_from_env


def main():
    parser = argparse.ArgumentParser(description='Import some files')
    parser.add_argument('name', metavar='PROJECT_NAME', help='The project name')
    parser.add_argument('--id', '-i', metavar='ID', help='Override the randomly generated Id.')

    args = parser.parse_args()

    app = app_from_env()
    pprint.pprint(app.client.post('/api/v1/projects', {'name': args.name, 'projectId': args.id}))


if __name__ == '__main__':
    main()
