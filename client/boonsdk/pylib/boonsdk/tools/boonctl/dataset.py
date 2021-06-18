import os.path

import boonsdk

app = boonsdk.app_from_env()


def add_subparser(subparsers):
    subparser = subparsers.add_parser("datasets", help='Manage Datasets')
    commands = subparser.add_subparsers()

    list_cmd = commands.add_parser('list', help='List Datasets')
    list_cmd.set_defaults(func=display_list)

    ds_types = [t.name.lower() for t in boonsdk.DatasetType]
    create_cmd = commands.add_parser('create', help='Create a Dataset')
    create_cmd.add_argument('name', metavar='NAME', help='The Dataset name')
    create_cmd.add_argument('type', metavar='TYPE', help='The Dataset type: %s' % ds_types,
                            choices=ds_types)
    create_cmd.add_argument('-d', '--descr', metavar='DESCRIPTION', help='A description')
    create_cmd.set_defaults(func=create)

    import_cmd = commands.add_parser('import', help='Import a Dataset')
    import_cmd.add_argument('type', metavar='TYPE', help='The Dataset type: %s' % ds_types,
                            choices=ds_types)
    import_cmd.add_argument('path', metavar='PATH', help='The path to the dataset files')

    import_cmd.add_argument('-d', '--descr', metavar='DESCRIPTION', help='A description')
    import_cmd.add_argument('-c', '--count', type=int, default=0,
                            help='The number of assets to import per label. Default all.')
    import_cmd.add_argument('-r', '--test-ratio', type=float,
                            default=0.1, help='The percentage of assets to label as test.')
    import_cmd.set_defaults(func=import_ds)

    info_cmd = commands.add_parser('info', help='Get Info about a Dataset')
    info_cmd.add_argument('name', metavar='NAME', help='The Dataset name or ID.')
    info_cmd.set_defaults(func=info)

    label1_cmd = commands.add_parser('keyword-label',
                                     help='Label assets for classification with given keyword')
    label1_cmd.add_argument('dataset', metavar='DATASET', help='The Dataset ID or name.')
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
    print(f'Description: {ds.description}')
    print(f'Created By: {ds.actor_created}')
    print(f'Created Date: {ds.time_created}')
    print('\nLabels: ')
    i = 1
    for k, v in labels.items():
        print('%d:%-30s            assets: %d' % (i, k, v))
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


def import_ds(args):
    if args.type == "classification":
        import_classification(args)


def import_classification(args):
    ds_name = os.path.basename(args.path)
    labels = [label for label in os.listdir(args.path) if label.isalnum()]
    print(f'Detected Name: {ds_name}')
    print(f'Detected Labels: {labels}')
    print("Ok? (ctr-c to abort)")
    _ = input()

    dataset = app.datasets.create_dataset(ds_name, args.type)

    for label in labels:
        lpath = os.path.join(args.path, label)
        files = [name for name in os.listdir(lpath) if boonsdk.FileTypes.supported(name)]

        if args.count == 0:
            total_file_count = len(files)
        else:
            total_file_count = args.count

        test_count = int(args.test_ratio * total_file_count) + 1
        train_count = total_file_count - test_count

        print(f'Importing {total_file_count} "{label}" '
              f'assets. Train: {train_count} Test: {test_count}')

        ds_label = dataset.make_label(label, scope=boonsdk.LabelScope.TRAIN)
        upload = [boonsdk.FileUpload(os.path.join(lpath, f),
                                     label=ds_label) for f in files[0:train_count]]
        app.assets.batch_upload_files(upload)

        ds_label = dataset.make_label(label, scope=boonsdk.LabelScope.TEST)
        upload = [boonsdk.FileUpload(os.path.join(lpath, f), label=ds_label)
                  for f in files[train_count:train_count+test_count]]
        app.assets.batch_upload_files(upload)
