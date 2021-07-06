import boonsdk
import zipfile

from tempfile import mkdtemp
from os import walk, path

# download dataset from: https://www.kaggle.com/chetankv/dogs-cats-images

batch_size = 50
zipped_file_location = path.dirname(path.realpath(__file__))
zipped_file_name = 'archive.zip'
test_ratio = 0.1
images_base_path = '/dataset/'
ds_name = 'dogs-cats'


def import_fruit_dataset():
    base_path = prepare_dataset_folder()
    set_base_paths = [path.join(base_path, 'training_set'), path.join(base_path, 'test_set')]
    label_path_dict = {
        'Cat': ['cats'],
        'Dog': ['dogs']
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

    total_file_count = len(assets)
    test_count = int(test_ratio * total_file_count) + 1
    print(f'Importing {total_file_count} files to {ds_name} dataset '
          f'Using {test_ratio} as test ratio '
          f'{test_count} images were reserved to test and '
          f'{total_file_count - test_count} to train')

    assets = [assets[offs:offs + batch_size] for offs in range(0, len(assets), batch_size)]

    for batch in assets:
        app.assets.batch_upload_files(batch)


def prepare_dataset_folder():
    temp_dir = mkdtemp()
    zipped_file = path.join(zipped_file_location, zipped_file_name)
    zip_ref = zipfile.ZipFile(zipped_file)
    zip_ref.extractall(temp_dir)
    return temp_dir + images_base_path


import_fruit_dataset()
