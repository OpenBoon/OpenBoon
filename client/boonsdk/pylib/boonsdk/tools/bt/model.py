from argparse import Namespace
import boonsdk

app = boonsdk.app_from_env()


def add_subparser(subparsers):
    subparser = subparsers.add_parser("model", help='Manage custom models')
    commands = subparser.add_subparsers()

    list_cmd = commands.add_parser('list', help='List models')
    list_cmd.set_defaults(func=display_list)

    upload_cmd = commands.add_parser('upload', help='Upload a model directory')
    upload_cmd.add_argument('id', metavar='ID', help='The model ID')
    upload_cmd.add_argument('path', metavar='PATH', help='A model directory path')
    upload_cmd.set_defaults(func=upload_model)

    subparser.set_defaults(func=default_list)


def default_list(args):
    display_list(Namespace())


def display_list(args):
    fmt = '%-36s %24s %-24s %-24s'
    print((fmt % ('ID', 'Name', 'Mo', 'Type')))
    for item in app.models.find_models():
        print(fmt % (item.id,
                     item.name,
                     item.module_name,
                     item.type))


def upload_model(args):
    model = app.models.get_model(args.id)
    print(app.models.upload_trained_model(model, args.path, labels=None))


def create_model(args):
    model = app.models.get_model(args.id)
    print(app.models.upload_trained_model(model, args.path, labels=None))
