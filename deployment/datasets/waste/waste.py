import boonsdk
import utils

from os import walk, path

# download dataset from: https://www.kaggle.com/techsash/waste-classification-data

# UPDATABLE
DS_NAME = 'Waste'
BATCH_SIZE = 20
TEST_RATIO = 0.1

# DO NOT CHANGE
zipped_file_location = path.dirname(path.realpath(__file__))
zipped_file_name = 'archive.zip'
images_base_path = 'DATASET/'


def import_waste_dataset():
    base_path = utils.prepare_dataset_folder(
        images_base_path, zipped_file_location, zipped_file_name)

    set_base_paths = [path.join(base_path, 'TEST'), path.join(base_path, 'TRAIN')]

    label_path_dict = {
        'Organic': ['O'],
        'Recyclable': ['R'],
    }

    app = boonsdk.app_from_env()
    assets = []

    ds = app.datasets.create_dataset(DS_NAME, boonsdk.DatasetType.Classification)
    for key in label_path_dict:
        paths = []
        for samples in label_path_dict[key]:
            for images_set in set_base_paths:
                paths.append(path.join(images_set, samples))

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

    assets = utils.create_batches(assets, BATCH_SIZE)

    for batch in assets:
        app.assets.batch_upload_files(batch)


import_waste_dataset()
