from argparse import Namespace
import boonsdk
from boonsdk.util import is_valid_uuid

app = boonsdk.app_from_env()


def add_subparser(subparsers):
    subparser = subparsers.add_parser("model", help='Manage custom models')
    commands = subparser.add_subparsers()

    list_cmd = commands.add_parser('list', help='List models')
    list_cmd.set_defaults(func=display_list)

    train_cmd = commands.add_parser('train', help='Train a model')
    train_cmd.add_argument('id', metavar='ID', help='The model ID')
    train_cmd.add_argument('-a', '--action', metavar='ACTION', default='none',
                           choices=['test', 'apply', 'none'],
                           help='An action to take after training is complete.')
    train_cmd.set_defaults(func=train)

    upload_cmd = commands.add_parser('upload', help='Upload a model directory')
    upload_cmd.add_argument('id', metavar='ID', help='The model ID')
    upload_cmd.add_argument('path', metavar='PATH', help='A model directory path')
    upload_cmd.set_defaults(func=upload_model)

    subparser.set_defaults(func=default_list)


def get_model(name):
    if is_valid_uuid(name):
        return app.models.get_model(name)
    else:
        return app.models.find_one_model(name=[name])


def default_list(args):
    display_list(Namespace())


def train(args):
    app.models.train_model(get_model(args.id), args.action)


def display_list(args):
    fmt = '%-36s %24s %-24s %-24s'
    print((fmt % ('ID', 'Name', 'Mod', 'Type')))
    for item in app.models.find_models():
        print(fmt % (item.id,
                     item.name,
                     item.module_name,
                     item.type))


def upload_model(args):
    model = get_model(args.id)
    print(app.models.upload_trained_model(model, args.path, labels=None))
