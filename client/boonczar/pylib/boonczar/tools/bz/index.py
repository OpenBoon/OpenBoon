import pprint

import boonczar
import boonsdk

app = boonsdk.app_from_env()
czar = boonczar.from_app(app)


def add_subparser(subparsers):
    subparser = subparsers.add_parser("index", help='Manage elasticsearch indices.')
    commands = subparser.add_subparsers()

    # List Command
    list_command = commands.add_parser('list', help='List all indices.')
    list_command.add_argument('-p', metavar='PROJECT_ID', help='The project id')
    list_command.add_argument('-n', metavar='PROJECT_NAME', help='The project name')
    list_command.set_defaults(func=display_list)

    # List Command
    mig_cmd = commands.add_parser('migrate-project', help='Migrate a project index')
    mig_cmd.add_argument('id', metavar='PROJECT_ID', help='The project id')
    mig_cmd.add_argument('version', metavar='VERSION', help='The mapping version', type=int)
    mig_cmd.add_argument('-s', '--size', metavar='SIZE',
                         help='The predicted index size, defaults to same size')
    mig_cmd.set_defaults(func=migrate_project)

    subparser.set_defaults(func=handle_args)


def handle_args(args):
    args.func(args)


def display_list(args):
    fmt = '%-36s %-32s %-32s %-6s'
    print((fmt % ('ID', 'URL', 'Project', 'State')))
    for idx in czar.indexes.find_indexes(project=args.p, project_name=args.n):
        print(fmt % (idx.id,
                     idx.index_url,
                     idx.project_name,
                     idx.state.name))


def migrate_project(args):
    project = czar.projects.get_project(args.id)
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

    task = czar.indexes.migrate_project_index(project,
                                              "english_strict", args.version,
                                              size=size)
    pprint.pprint(task._data)
