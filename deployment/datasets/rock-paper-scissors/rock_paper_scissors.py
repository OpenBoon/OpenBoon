import boonsdk
import utils
from os import walk, path

# download dataset from: https://www.kaggle.com/drgfreeman/rockpaperscissors

# UPDATABLE
DS_NAME = 'Rock,Paper and Scissors'
TEST_RATIO = 0.1
BATCH_SIZE = 20

# DO NOT CHANGE
zipped_file_location = path.dirname(path.realpath(__file__))
zipped_file_name = 'archive.zip'
images_base_path = ''


def import_dog_dataset():
    base_path = utils.prepare_dataset_folder(
        images_base_path, zipped_file_location, zipped_file_name)

    label_path_dict = {
        'Rock': 'rock',
        'Paper': 'paper',
        'Scissors': 'scissors',
    }

    app = boonsdk.app_from_env()
    assets = []

    ds = app.datasets.create_dataset(DS_NAME, boonsdk.DatasetType.Classification)

    for key in label_path_dict:
        for (dirpath, dirnames, filenames) in walk(path.join(base_path, label_path_dict[key])):
            test_count = int(TEST_RATIO * len(filenames)) + 1

            sanitized_label = key
            test_label = ds.make_label(sanitized_label, scope=boonsdk.LabelScope.TEST)
            assets.extend([boonsdk.FileUpload(path.join(dirpath, name), label=test_label)
                           for name in filenames[0:test_count]])

            train_label = ds.make_label(sanitized_label, scope=boonsdk.LabelScope.TRAIN)
            assets.extend([boonsdk.FileUpload(path.join(dirpath, name), label=train_label)
                           for name in filenames[test_count:]])

    utils.print_dataset_info(DS_NAME, len(assets), TEST_RATIO)

    assets = utils.create_batches(assets, BATCH_SIZE)

    for batch in assets:
        app.assets.batch_upload_files(batch)


import_dog_dataset()
