import boonsdk
import utils

from os import walk, path

# download dataset from: https://www.kaggle.com/slothkong/10-monkey-species

# UPDATABLE
ds_name = 'Monkeys'
batch_size = 50
test_ratio = 0.1

# DO NOT CHANGE
zipped_file_location = path.dirname(path.realpath(__file__))
zipped_file_name = 'archive.zip'
images_base_path = ''


def import_monkeys_dataset():
    base_path = utils.prepare_dataset_folder(images_base_path, zipped_file_location, zipped_file_name)
    set_base_paths = [path.join(base_path, 'training/training'), path.join(base_path, 'validation/validation')]

    lines = []
    with open(path.join(base_path, 'monkey_labels.txt'), 'r') as f_in:
        lines = [l for l in (line.strip() for line in f_in) if l]

    label_path_dict = {}

    for i in range(1, len(lines)):
        splitted_line = lines[i].split(',')
        label_path_dict[splitted_line[0].strip()] = [splitted_line[2].strip()]

    app = boonsdk.app_from_env()
    assets = []

    ds = app.datasets.create_dataset(ds_name, boonsdk.DatasetType.Classification)

    paths = []
    for samples in label_path_dict.keys():
        for images_set in set_base_paths:
            paths.append(path.join(images_set, samples))

    for p in paths:
        for (dirpath, dirnames, filenames) in walk(p):
            filenames = [file for file in filenames if file.endswith(".jpg")]

            test_count = int(test_ratio * len(filenames)) + 1

            sanitized_label = utils.sanitize_label(label_path_dict[path.basename(p)][0])
            test_label = ds.make_label(sanitized_label, scope=boonsdk.LabelScope.TEST)
            assets.extend([boonsdk.FileUpload(path.join(dirpath, name),
                                              label=test_label) for name in filenames[0:test_count]])

            train_label = ds.make_label(sanitized_label, scope=boonsdk.LabelScope.TRAIN)
            assets.extend([boonsdk.FileUpload(path.join(dirpath, name),
                                              label=train_label) for name in filenames[test_count:]])

    utils.print_dataset_info(ds_name, len(assets), test_ratio)

    assets = [assets[offs:offs + batch_size] for offs in range(0, len(assets), batch_size)]

    for batch in assets:
        app.assets.batch_upload_files(batch)


import_monkeys_dataset()
