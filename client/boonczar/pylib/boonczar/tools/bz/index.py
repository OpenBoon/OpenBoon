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

    close_cmd = commands.add_parser('close', help='Close an index.')
    close_cmd.add_argument('index', metavar='INDEX', help='The Index id')
    close_cmd.set_defaults(func=close_index)

    open_cmd = commands.add_parser('open', help='Open an index.')
    open_cmd.add_argument('index', metavar='INDEX', help='The Index id')
    open_cmd.set_defaults(func=open_index)

    del_cmd = commands.add_parser('delete', help='Delete an index.')
    del_cmd.add_argument('index', metavar='INDEX', help='The Index id')
    del_cmd.set_defaults(func=delete_index)

    subparser.set_defaults(func=handle_args)


def handle_args(args):
    args.func(args)


def close_index(args):
    print(czar.indexes.close_index(args.index))


def open_index(args):
    print(czar.indexes.open_index(args.index))


def delete_index(args):
    print(czar.indexes.delete_index(args.index))


def display_list(args):
    fmt = '%-36s %-6s %-16s %-20s %-6s'
    print((fmt % ('ID', 'State', 'Name', 'Project', 'Ver')))
    for idx in czar.indexes.find_indexes(project=args.p, project_name=args.n):
        print(fmt % (idx.id,
                     idx.state.name,
                     idx.index_name,
                     idx.project_name,
                     idx.version))


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
