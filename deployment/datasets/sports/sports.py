import boonsdk
import zipfile

from tempfile import mkdtemp
from os import walk, path

# download dataset from: https://www.kaggle.com/gpiosenka/sports-classification

batch_size = 50
zipped_file_location = path.dirname(path.realpath(__file__))
zipped_file_name = 'archive.zip'
test_ratio = 0.1
images_base_path = '/'
ds_name = 'sports'


def import_sports_dataset():
    base_path = prepare_dataset_folder()
    set_base_paths = [path.join(base_path, 'train'), path.join(base_path, 'test'), path.join(base_path, 'valid')]

    label_path_dict = {
        'frisbee': ['frisbee'],
        'jai alai': ['jai alai'],
        'uneven bars': ['uneven bars'],
        'curling': ['curling'],
        'golf': ['golf'],
        'hockey': ['hockey'],
        'rock climbing': ['rock climbing'],
        'luge': ['luge'],
        'judo': ['judo'],
        'nascar racing': ['nascar racing'],
        'bmx': ['bmx'],
        'olympic wrestling': ['olympic wrestling'],
        'fencing': ['fencing'],
        'swimming': ['swimming'],
        'bowling': ['bowling'],
        'bull riding': ['bull riding'],
        'boxing': ['boxing'],
        'archery': ['archery'],
        'basketball': ['basketball'],
        'billiards': ['billiards'],
        'rings': ['rings'],
        'speed skating': ['speed skating'],
        'table tennis': ['table tennis'],
        'hurdles': ['hurdles'],
        'figure skating pairs': ['figure skating pairs'],
        'track bicycle': ['track bicycle'],
        'figure skating women': ['figure skating women'],
        'ice climbing': ['ice climbing'],
        'lacrosse': ['lacrosse'],
        'polo': ['polo'],
        'giant slalom': ['giant slalom'],
        'horse racing': ['horse racing'],
        'ski jumping': ['ski jumping'],
        'barell racing': ['barell racing'],
        'hammer throw': ['hammer throw'],
        'sumo wrestling': ['sumo wrestling'],
        'tennis': ['tennis'],
        'sailboat racing': ['sailboat racing'],
        'javelin': ['javelin'],
        'weightlifting': ['weightlifting'],
        'parallel bar': ['parallel bar'],
        'harness racing': ['harness racing'],
        'pole vault': ['pole vault'],
        'croquet': ['croquet'],
        'snowmobile racing': ['snowmobile racing'],
        'bobsled': ['bobsled'],
        'high jump': ['high jump'],
        'surfing': ['surfing'],
        'canoe slamon': ['canoe slamon'],
        'field hockey': ['field hockey'],
        'ampute football': ['ampute football'],
        'shot put': ['shot put'],
        'skydiving': ['skydiving'],
        'wheelchair racing': ['wheelchair racing'],
        'cricket': ['cricket'],
        'arm wrestling': ['arm wrestling'],
        'air hockey': ['air hockey'],
        'formula 1 racing': ['formula 1 racing'],
        'football': ['football'],
        'water polo': ['water polo'],
        'pommel horse': ['pommel horse'],
        'volleyball': ['volleyball'],
        'rollerblade racing': ['rollerblade racing'],
        'rowing': ['rowing'],
        'motorcycle racing': ['motorcycle racing'],
        'rugby': ['rugby'],
        'snow boarding': ['snow boarding'],
        'baseball': ['baseball'],
        'tug of war': ['tug of war'],
        'figure skating men': ['figure skating men'],
        'balance beam': ['balance beam'],
        'horse jumping': ['horse jumping'],
        'wheelchair basketball': ['wheelchair basketball']
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

                sanitized_key = sanitize_key(key)
                test_label = ds.make_label(sanitized_key, scope=boonsdk.LabelScope.TEST)
                assets.extend([boonsdk.FileUpload(path.join(dirpath, name),
                                                  label=test_label) for name in filenames[0:test_count]])

                train_label = ds.make_label(sanitized_key, scope=boonsdk.LabelScope.TRAIN)
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


def sanitize_key(key):
    return key.title()


def prepare_dataset_folder():
    temp_dir = mkdtemp()
    zipped_file = path.join(zipped_file_location, zipped_file_name)
    zip_ref = zipfile.ZipFile(zipped_file)
    zip_ref.extractall(temp_dir)
    return temp_dir + images_base_path


import_sports_dataset()
