import boonsdk
import utils

from os import walk, path

# download dataset from: https://www.kaggle.com/crowww/a-large-scale-fish-dataset

# UPDATABLE
DS_NAME = 'Fishes'
BATCH_SIZE = 20
TEST_RATIO = 0.1

# DO NOT CHANGE
zipped_file_location = path.dirname(path.realpath(__file__))
zipped_file_name = 'archive.zip'
images_base_path = 'Fish_Dataset/Fish_Dataset/'


def import_fish_dataset():
    base_path = utils.prepare_dataset_folder(
        images_base_path, zipped_file_location, zipped_file_name)

    label_path_dict = {
        'Black Sea Sprat': 'Black Sea Sprat/Black Sea Sprat',
        'Hourse Mackerel': 'Hourse Mackerel/Hourse Mackerel',
        'Red Sea Bream': 'Red Sea Bream/Red Sea Bream',
        'Striped Red Mullet': 'Striped Red Mullet/Striped Red Mullet',
        'Gilt-Head Bream': 'Gilt-Head Bream/Gilt-Head Bream',
        'Red Mullet': 'Red Mullet/Red Mullet',
        'Sea Bass': 'Sea Bass/Sea Bass',
        'Shrimp': 'Shrimp/Shrimp',
        'Trout': 'Trout/Trout'
    }

    app = boonsdk.app_from_env()
    assets = []

    ds = app.datasets.create_dataset(DS_NAME, boonsdk.DatasetType.Classification)

    for key in label_path_dict:
        for (dirpath, dirnames, filenames) in walk(path.join(base_path, label_path_dict[key])):
            test_count = int(TEST_RATIO * len(filenames)) + 1

            test_label = ds.make_label(key, scope=boonsdk.LabelScope.TEST)
            assets.extend([boonsdk.FileUpload(path.join(dirpath, name), label=test_label)
                           for name in filenames[0:test_count]])

            train_label = ds.make_label(key, scope=boonsdk.LabelScope.TRAIN)
            assets.extend([boonsdk.FileUpload(path.join(dirpath, name), label=train_label)
                           for name in filenames[test_count:]])

    utils.print_dataset_info(DS_NAME, len(assets), TEST_RATIO)

    assets = utils.create_batches(assets, BATCH_SIZE)

    for batch in assets:
        app.assets.batch_upload_files(batch)


import_fish_dataset()
