import json
import boonsdk

app = boonsdk.app_from_env()


def add_subparser(subparsers):
    subparser = subparsers.add_parser("asset", help='Manage Assets')
    commands = subparser.add_subparsers()

    set_value = commands.add_parser('set-field-value', help='Set the value of a custom field.')
    set_value.add_argument('asset', metavar='ASSET', help='The asset ID')
    set_value.add_argument('name', metavar='NAME', help='The field name')
    set_value.add_argument('value', metavar='VALUE', help='The field type')
    set_value.set_defaults(func=set_field_value)

    subparser.set_defaults(func=handle_default)

def handle_default(args):
    pass

def set_field_value(args):
    j_value = json.loads(args.value)
    print(app.assets.set_field_values(args.asset, {args.name: j_value}))
