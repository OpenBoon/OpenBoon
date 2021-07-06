import boonsdk
import zipfile

from tempfile import mkdtemp
from os import walk, path

# download dataset from: https://www.kaggle.com/drgfreeman/rockpaperscissors

batch_size = 50
zipped_file_location = path.dirname(path.realpath(__file__))
zipped_file_name = 'archive.zip'
test_ratio = 0.1
images_base_path = ''
ds_name = 'RockPaperScissors-Dataset'


def import_dog_dataset():
    base_path = prepare_dataset_folder()
    label_path_dict = {
        'Rock': 'rock',
        'Paper': 'paper',
        'Scissors': 'scissors',
    }
    app = boonsdk.app_from_env()
    assets = []

    ds = app.datasets.create_dataset(ds_name, boonsdk.DatasetType.Classification)

    for key in label_path_dict:
        for (dirpath, dirnames, filenames) in walk(path.join(base_path, label_path_dict[key])):
            test_count = int(test_ratio * len(filenames)) + 1

            sanitized_label = key
            test_label = ds.make_label(sanitized_label, scope=boonsdk.LabelScope.TEST)
            assets.extend([boonsdk.FileUpload(path.join(dirpath, name),
                                              label=test_label) for name in filenames[0:test_count]])

            train_label = ds.make_label(sanitized_label, scope=boonsdk.LabelScope.TRAIN)
            assets.extend([boonsdk.FileUpload(path.join(dirpath, name),
                                              label=train_label) for name in filenames[test_count:]])

    total_file_count = len(assets)
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


import_dog_dataset()
