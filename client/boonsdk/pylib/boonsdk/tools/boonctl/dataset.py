import boonsdk

app = boonsdk.app_from_env()


def add_subparser(subparsers):
    subparser = subparsers.add_parser("dataset", help='Manage DataSets')
    commands = subparser.add_subparsers()

    list_cmd = commands.add_parser('list', help='List DataSets')
    list_cmd.set_defaults(func=display_list)

    create_cmd = commands.add_parser('create', help='Create a DataSet')
    create_cmd.add_argument('name', metavar='NAME', help='The DataSet name')
    create_cmd.add_argument('type', metavar='TYPE', help='The DataSet type',
                            choices=['classify', 'detect', 'recognize'])
    create_cmd.set_defaults(func=create)

    info_cmd = commands.add_parser('info', help='Get Info about a DataSet')
    info_cmd.add_argument('name', metavar='NAME', help='The DataSet name or ID.')
    info_cmd.set_defaults(func=info)

    label1_cmd = commands.add_parser('keyword-label',
                                     help='Label assets for classification with given keyword')
    label1_cmd.add_argument('dataset', metavar='DATASET', help='The DataSet ID or name.')
    label1_cmd.add_argument('keyword', metavar='KEYWORD', help='The keyword to search for.')
    label1_cmd.add_argument('-l', '--label', metavar='LABEL',
                            help='Override the label to use, defaults to the keyword.')
    label1_cmd.add_argument('-f', '--field', metavar='FIELD',
                            help='The field to match for the keyword',
                            default='source.path.fulltext')
    label1_cmd.set_defaults(func=keyword_label)


def show_info(ds):
    labels = app.datasets.get_label_counts(ds)
    print(f'ID:   {ds.id}')
    print(f'Name: {ds.name}')
    print(f'Created By: {ds.actor_created}')
    print(f'Created Date: {ds.time_created}')
    print('\nLabels: ')
    i = 1
    for k, v in labels.items():
        print(f'{i}:{k}            assets: {v}')
        i += 1


def info(args):
    ds = app.datasets.get_dataset(args.name)
    show_info(ds)


def create(args):
    type_map = {'c': 'classification', 'd': 'detection', 'r': 'facerecognition'}
    ds = app.datasets.create_dataset(args.name, type_map[args.type[0]])
    show_info(ds)


def keyword_label(args):
    ds = app.datasets.get_dataset(args.dataset)
    q = {
        "size": 50,
        "query": {
            "match": {
                args.field: {
                    "query": args.keyword
                }
            }
        }
    }

    label = ds.make_label(args.label or args.keyword)
    search = app.assets.search(q).batches_of(50)
    for batch in search:
        print("labeling {} as {}".format(len(batch), label.label))
        app.assets.update_labels(batch, label)


def display_list(args):
    fmt = '%-36s %-24s %-24s'
    print((fmt % ('ID', 'Name', 'Type')))
    print("-" * (36+24+24))
    for item in app.datasets.find_datasets():
        print(fmt % (item.id,
                     item.name,
                     item.type.name))
