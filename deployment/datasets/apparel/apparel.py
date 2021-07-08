import boonsdk
import utils

from os import walk, path

# download dataset from: https://www.kaggle.com/trolukovich/apparel-images-dataset

# UPDATABLE
BATCH_SIZE = 50
TEST_RATIO = 0.1
DS_NAME = 'Apparel'

# DO NOT CHANGE
zipped_file_location = path.dirname(path.realpath(__file__))
zipped_file_name = 'archive.zip'
images_base_path = ''


def import_apparel_dataset():
    base_path = utils.prepare_dataset_folder(
        images_base_path, zipped_file_location, zipped_file_name)

    label_path_dict = {
        'red_shoes': ['red_shoes'],
        'green_pants': ['green_pants'],
        'brown_shoes': ['brown_shoes'],
        'red_pants': ['red_pants'],
        'black_shoes': ['black_shoes'],
        'brown_shorts': ['brown_shorts'],
        'green_shirt': ['green_shirt'],
        'blue_shorts': ['blue_shorts'],
        'black_dress': ['black_dress'],
        'blue_shoes': ['blue_shoes'],
        'brown_pants': ['brown_pants'],
        'white_dress': ['white_dress'],
        'white_shorts': ['white_shorts'],
        'black_shirt': ['black_shirt'],
        'white_pants': ['white_pants'],
        'green_shoes': ['green_shoes'],
        'black_shorts': ['black_shorts'],
        'blue_shirt': ['blue_shirt'],
        'green_shorts': ['green_shorts'],
        'red_dress': ['red_dress'],
        'blue_pants': ['blue_pants'],
        'black_pants': ['black_pants'],
        'blue_dress': ['blue_dress'],
        'white_shoes': ['white_shoes']
    }

    app = boonsdk.app_from_env()
    assets = []

    ds = app.datasets.create_dataset(DS_NAME, boonsdk.DatasetType.Classification)

    for key in label_path_dict:
        paths = []
        for samples in label_path_dict[key]:
            paths.append(path.join(base_path, samples))

        for p in paths:
            for (dirpath, dirnames, filenames) in walk(p):
                test_count = int(TEST_RATIO * len(filenames)) + 1

                test_label = ds.make_label(key, scope=boonsdk.LabelScope.TEST)
                assets.extend([boonsdk.FileUpload(path.join(dirpath, name), label=test_label)
                               for name in filenames[0:test_count]])

                train_label = ds.make_label(key, scope=boonsdk.LabelScope.TRAIN)
                assets.extend([boonsdk.FileUpload(path.join(dirpath, name), label=train_label)
                               for name in filenames[test_count:]])

    utils.print_dataset_info(DS_NAME, len(assets), TEST_RATIO)

    assets = [assets[offs:offs + BATCH_SIZE] for offs in range(0, len(assets), BATCH_SIZE)]

    for batch in assets:
        app.assets.batch_upload_files(batch)


import_apparel_dataset()
