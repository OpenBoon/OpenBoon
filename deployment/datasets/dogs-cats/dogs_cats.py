import boonsdk
import utils

from os import walk, path

# download dataset from: https://www.kaggle.com/chetankv/dogs-cats-images

# UPDATABLE
DS_NAME = 'Dogs and Cats'
BATCH_SIZE = 20
TEST_RATIO = 0.1

# DO NOT CHANGE
zipped_file_location = path.dirname(path.realpath(__file__))
zipped_file_name = 'archive.zip'
images_base_path = 'dataset/'


def import_fruit_dataset():
    base_path = utils.prepare_dataset_folder(
        images_base_path, zipped_file_location, zipped_file_name)
    set_base_paths = [path.join(base_path, 'training_set'), path.join(base_path, 'test_set')]
    label_path_dict = {
        'Cat': ['cats'],
        'Dog': ['dogs']
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


import_fruit_dataset()
