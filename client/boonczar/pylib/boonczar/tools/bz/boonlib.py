import json

import boonczar
import boonsdk

app = boonsdk.app_from_env()
czar = boonczar.from_app(app)


def add_subparser(subparsers):
    subparser = subparsers.add_parser("boonlibs", help='Manage BoonLibs')
    commands = subparser.add_subparsers()

    # List Command
    create_cmd = commands.add_parser('create', help='Create a new BoonLib from an Entity.')
    create_cmd.set_defaults(func=create_boonlib)

    subparser.set_defaults(func=handle_args)


def handle_args(args):
    args.func(args)


def create_boonlib(args):
    entity = input("Type of BoonLib (Dataset): ")
    entity_id = input("Unique ID of the Entity: ")
    name = input("Name for the new BoonLib: ")
    desc = input("Description: ")

    if not entity:
        entity = "Dataset"

    print(json.dumps(czar.boonlibs.create_boonlib(entity, entity_id, name, desc)._data, indent=2))
