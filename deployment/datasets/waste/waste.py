import boonsdk
import utils

from os import walk, path

# download dataset from: https://www.kaggle.com/techsash/waste-classification-data

batch_size = 50
zipped_file_location = path.dirname(path.realpath(__file__))
zipped_file_name = 'archive.zip'
test_ratio = 0.1
images_base_path = 'DATASET/'
ds_name = 'waste'


def import_waste_dataset():
    base_path = utils.prepare_dataset_folder(images_base_path, zipped_file_location, zipped_file_name)

    set_base_paths = [path.join(base_path, 'TEST'), path.join(base_path, 'TRAIN')]

    label_path_dict = {
        'Organic': ['O'],
        'Recyclable': ['R'],
    }

    app = boonsdk.app_from_env()
    assets = []

    ds = app.datasets.create_dataset(ds_name, boonsdk.DatasetType.Classification)
    for key in label_path_dict:
        paths = []
        for samples in label_path_dict[key]:
            for images_set in set_base_paths:
                paths.append(path.join(images_set, samples))

        for p in paths:
            for (dirpath, dirnames, filenames) in walk(p):
                test_count = int(test_ratio * len(filenames)) + 1

                test_label = ds.make_label(key, scope=boonsdk.LabelScope.TEST)
                assets.extend([boonsdk.FileUpload(path.join(dirpath, name),
                                                  label=test_label) for name in filenames[0:test_count]])

                train_label = ds.make_label(key, scope=boonsdk.LabelScope.TRAIN)
                assets.extend([boonsdk.FileUpload(path.join(dirpath, name),
                                                  label=train_label) for name in filenames[test_count:]])

    utils.print_dataset_info(ds_name, len(assets), test_ratio)

    assets = [assets[offs:offs + batch_size] for offs in range(0, len(assets), batch_size)]

    for batch in assets:
        app.assets.batch_upload_files(batch)


import_waste_dataset()
