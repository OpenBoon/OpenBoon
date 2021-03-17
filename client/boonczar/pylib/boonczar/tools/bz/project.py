import boonczar
import boonsdk

app = boonsdk.app_from_env()
czar = boonczar.from_app(app)


def add_subparser(subparsers):
    subparser = subparsers.add_parser("project", help='Manage projects.')
    commands = subparser.add_subparsers()

    # List Command
    list_cmd = commands.add_parser('list', help='List all.')
    list_cmd.set_defaults(func=display_list)

    # List Command
    setidx_cmd = commands.add_parser('set-index', help='Set the index for the project')
    setidx_cmd.add_argument('project', metavar='PROJECT_ID', help='The project id')
    setidx_cmd.add_argument('index', metavar='INDEX_ID', help='The index id')
    setidx_cmd.set_defaults(func=set_index)

    subparser.set_defaults(func=handle_args)


def handle_args(args):
    args.func(args)


def set_index(args):
    czar.projects.set_project_index(args.project, args.index)


def display_list(args):
    fmt = '%-36s %-32s %-5s'
    print((fmt % ('ID', 'Name', 'Enabled')))
    for item in czar.projects.find_projects():
        print(fmt % (item.id, item.name, item.enabled))
