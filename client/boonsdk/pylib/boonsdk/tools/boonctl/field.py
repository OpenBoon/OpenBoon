from argparse import Namespace
import boonsdk

app = boonsdk.app_from_env()


def add_subparser(subparsers):
    subparser = subparsers.add_parser("fields", help='Manage Custom Fields')
    commands = subparser.add_subparsers()

    list_cmd = commands.add_parser('list', help='List all.')
    list_cmd.add_argument('-n', '--name', help='Filter fields by name')
    list_cmd.add_argument('-t', '--type', help='Filter fields by type')
    list_cmd.set_defaults(func=display_list)

    create_cmd = commands.add_parser('create', help='Create a new custom field.')
    create_cmd.add_argument('name', metavar='NAME', help='The field name')
    create_cmd.add_argument('type', metavar='TYPE', help='The field type')
    create_cmd.set_defaults(func=create_field)

    subparser.set_defaults(func=default_list)


def default_list(args):
    display_list(Namespace(name=None, type=None))


def display_list(args):
    fmt = '%-36s %-24s %-24s %-24s'
    print((fmt % ('ID', 'Name', 'Path', 'Type')))
    for item in app.fields.find_fields(name=args.name, type=args.type):
        print(fmt % (item.id, item.name, item.path, item.type))


def create_field(args):
    field = app.fields.create_field(args.name, args.type)
    print(field.as_json())
