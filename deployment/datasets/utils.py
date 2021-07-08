from tempfile import mkdtemp
import zipfile
from os import path, environ


def prepare_dataset_folder(images_base_path, zipped_file_location, zipped_file_name):

    temp_dir = mkdtemp()
    zipped_file = path.join(zipped_file_location, zipped_file_name)
    zip_ref = zipfile.ZipFile(zipped_file)
    zip_ref.extractall(temp_dir)
    base_path = path.join(temp_dir, images_base_path)
    return base_path


def sanitize_label(key):
    for ch in ['-', '_']:
        key = key.replace(ch, " ")
    return key.title()


def print_dataset_info(ds_name, total_file_count, test_ratio):
    test_count = int(total_file_count * test_ratio) + 1
    print(f'Importing {total_file_count} files to {ds_name} dataset '
          f'Using {test_ratio} as test ratio '
          f'{test_count} images were reserved to test and '
          f'{total_file_count - test_count} to train')


def create_batches(assets, batch_size=20):
    return [assets[offs:offs + batch_size] for offs in range(0, len(assets), batch_size)]
