import json

import boonsdk

app = boonsdk.app_from_env()


def add_subparser(subparsers):
    subparser = subparsers.add_parser("assets", help='Manage Assets')
    commands = subparser.add_subparsers()

    set_value = commands.add_parser('set-field-value', help='Set the value of a custom field.')
    set_value.add_argument('asset', metavar='ASSET', help='The asset ID')
    set_value.add_argument('name', metavar='NAME', help='The field name')
    set_value.add_argument('value', metavar='VALUE', help='The field type')
    set_value.set_defaults(func=set_field_value)

    app_module = commands.add_parser('apply-module', help='Apply a module to an Asset.')
    app_module.add_argument('asset', metavar='ASSET', help='The asset ID')
    app_module.add_argument('-m', '--module', metavar='NAME',
                            help='The module name', action='append')
    app_module.set_defaults(func=apply_module)

    add_label = commands.add_parser('add-label', help='Add label to an Asset')
    add_label.add_argument('asset', metavar='ASSET', help='The asset ID')
    add_label.add_argument('dataset', metavar='DATASET', help='The Dataset name o ID')
    add_label.add_argument('label', metavar='ASSET', help='The label text')

    add_label.set_defaults(func=label_asset)

    subparser.set_defaults(func=handle_default)


def handle_default(args):
    pass


def set_field_value(args):
    j_value = json.loads(args.value)
    print(app.assets.set_field_values(args.asset, {args.name: j_value}))


def apply_module(args):
    if not args.module:
        print("You must specify at least 1 module to apply.")
        return
    app.assets.apply_modules(args.asset, args.module)


def label_asset(args):
    dataset = app.datasets.get_dataset(args.dataset)
    label = dataset.make_label(args.label)

    print(app.assets.batch_add_labels({args.asset: label}))
