import json

from argparse import Namespace
import boonsdk
from boonsdk.util import is_valid_uuid

app = boonsdk.app_from_env()

model_types = [t.name.lower() for t in boonsdk.ModelType]


def add_subparser(subparsers):
    subparser = subparsers.add_parser("models", help='Manage custom models')
    commands = subparser.add_subparsers()

    create_cmd = commands.add_parser('create', help='CreateModel')
    create_cmd.add_argument('name', metavar='NAME', help='The model name')
    create_cmd.add_argument('type', metavar='TYPE', help='The model type', choices=model_types)
    create_cmd.add_argument('-d', '--dataset', metavar='DATASET', help='An optional dataset')
    create_cmd.set_defaults(func=create_model)

    list_cmd = commands.add_parser('list', help='List models')
    list_cmd.set_defaults(func=display_list)

    list_cmd = commands.add_parser('list-types', help='List models types')
    list_cmd.set_defaults(func=display_types)

    train_args_cmd = commands.add_parser('train-args', help='Show model training args')
    train_args_cmd.add_argument('type', metavar='MODEL TYPE', help='The model ID, name, or type')
    train_args_cmd.set_defaults(func=show_train_args)

    train_cmd = commands.add_parser('train', help='Train a model')
    train_cmd.add_argument('id', metavar='ID', help='The model ID')
    train_cmd.add_argument('-p', '--post-action', metavar='ACTION', default='none',
                           choices=['test', 'apply', 'none'],
                           help='An action to take after training is complete.')
    train_cmd.add_argument('-a', '--arg', metavar='ARG', nargs=2, action='append',
                           help='An argument to set for this pass')

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


def display_types(args):
    all = [t._data for t in app.models.get_all_model_type_info()]
    print(json.dumps(all, indent=4))


def train(args):
    train_args = {}
    if args.arg:
        for pair in args.arg:
            train_args[pair[0]] = pair[1]

    app.models.train_model(get_model(args.id), args.post_action, train_args=train_args)


def show_info(model):
    print(f'ID:   {model.id}')
    print(f'Name: {model.name}')
    print(f'Created By: {model.actor_created}')
    print(f'Created Date: {model.time_created}')


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


def create_model(args):
    ds = None
    if args.dataset:
        ds = app.datasets.get_dataset(args.dataset)
    mdl = app.models.create_model(args.name, args.type, dataset=ds)
    show_info(mdl)


def show_train_args(args):
    mt = args.type
    if mt in model_types:
        targs = app.models.get_model_type_training_args(mt)
    else:
        model = get_model(mt)
        targs = app.models.get_model_type_training_args(model.type)
    print(json.dumps(targs, indent=4))
