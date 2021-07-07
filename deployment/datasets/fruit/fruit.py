import boonsdk
import utils

from os import walk, path

# download dataset from: https://www.kaggle.com/moltean/fruits

# UPDATABLE
ds_name = 'Fruits'
batch_size = 50
test_ratio = 0.1

# DO NOT CHANGE
zipped_file_location = path.dirname(path.realpath(__file__))
zipped_file_name = 'archive.zip'
images_base_path = 'fruits-360/'


def import_fruit_dataset():
    base_path = utils.prepare_dataset_folder(images_base_path, zipped_file_location, zipped_file_name)

    set_base_paths = [path.join(base_path, 'Training'), path.join(base_path, 'Test')]

    label_path_dict = {
        'Apple raeburn': ['Apple Braeburn'],
        'Apple Crimson Snow': ['Apple Crimson Snow'],
        'Apple Golden': ['Apple Golden 1', 'Apple Golden 2', 'Apple Golden 3'],
        'Apple Granny Smith': ['Apple Granny Smith'],
        'Apple Pink Laddy': ['Apple Pink Laddy'],
        'Apple Red': ['Apple Red 1', 'Apple Red 2', 'Apple Red 3'],
        'Apple Red Delicious': ['Apple Red Delicious'],
        'Apple Red Yellow': ['Apple Red Yellow 1', 'Apple Red Yellow 2'],
        'Apricot': ['Apricot'],
        'Avocado': ['Avocado'],
        'Avocado Ripe': ['Avocado Ripe'],
        'Banana': ['Banana'],
        'Banana Lady Finger': ['Banana Lady Finger'],
        'Banana Red': ['Banana Red'],
        'Beetroot': ['Beetroot'],
        'Blueberry': ['Blueberry'],
        'Cactus fruit': ['Cactus fruit'],
        'Cantaloupe': ['Cantaloupe 1', 'Cantaloupe 2'],
        'Carambula': ['Carambula'],
        'Cauliflower': ['Cauliflower'],
        'Cherry': ['Cherry 1', 'Cherry 2'],
        'Cherry Rainier': ['Cherry Rainier'],
        'Cherry Wax Black': ['Cherry Wax Black'],
        'Cherry Wax Red': ['Cherry Wax Red'],
        'Cherry Wax Yellow': ['Cherry Wax Yellow'],
        'Chestnut': ['Chestnut'],
        'Clementine': ['Clementine'],
        'Cocos': ['Cocos'],
        'Corn': ['Corn'],
        'Corn Husk': ['Corn Husk'],
        'Cucumber Ripe': ['Cucumber Ripe', 'Cucumber Ripe 2'],
        'Dates': ['Dates'],
        'Eggplant': ['Eggplant'],
        'Fig': ['Fig'],
        'Ginger Root': ['Ginger Root'],
        'Granadilla': ['Granadilla'],
        'Grape Blue': ['Grape Blue'],
        'Grapefruit Pink': ['Grapefruit Pink'],
        'Grapefruit White': ['Grapefruit White'],
        'Grape Pink': ['Grape Pink'],
        'Grape White': ['Grape White', 'Grape White 2', 'Grape White 3', 'Grape White 4'],
        'Guava': ['Guava'],
        'Hazelnut': ['Hazelnut'],
        'Huckleberry': ['Huckleberry'],
        'Kaki': ['Kaki'],
        'Kiwi': ['Kiwi'],
        'Kohlrabi': ['Kohlrabi'],
        'Kumquats': ['Kumquats'],
        'Lemon': ['Lemon'],
        'Lemon Meyer': ['Lemon Meyer'],
        'Limes': ['Limes'],
        'Lychee': ['Lychee'],
        'Mandarine': ['Mandarine'],
        'Mango': ['Mango'],
        'Mango Red': ['Mango Red'],
        'Mangostan': ['Mangostan'],
        'Maracuja': ['Maracuja'],
        'Melon Piel de Sapo': ['Melon Piel de Sapo'],
        'Mulberry': ['Mulberry'],
        'Nectarine': ['Nectarine'],
        'Nectarine Flat': ['Nectarine Flat'],
        'Nut Forest': ['Nut Forest'],
        'Nut Pecan': ['Nut Pecan'],
        'Onion Red': ['Onion Red'],
        'Onion Red Peeled': ['Onion Red Peeled'],
        'Onion White': ['Onion White'],
        'Orange': ['Orange'],
        'Papaya': ['Papaya'],
        'Passion Fruit': ['Passion Fruit'],
        'Peach': ['Peach', 'Peach 2'],
        'Peach Flat': ['Peach Flat'],
        'Pear': ['Pear', 'Pear 2'],
        'Pear Abate': ['Pear Abate'],
        'Pear Forelle': ['Pear Forelle'],
        'Pear Kaiser': ['Pear Kaiser'],
        'Pear Monster': ['Pear Monster'],
        'Pear Red': ['Pear Red'],
        'Pear Stone': ['Pear Stone'],
        'Pear Williams': ['Pear Williams'],
        'Pepino': ['Pepino'],
        'Pepper Green': ['Pepper Green'],
        'Pepper Orange': ['Pepper Orange'],
        'Pepper Red': ['Pepper Red'],
        'Pepper Yellow': ['Pepper Yellow'],
        'Physalis': ['Physalis'],
        'Physalis with Husk': ['Physalis with Husk'],
        'Pineapple': ['Pineapple'],
        'Pineapple Mini': ['Pineapple Mini'],
        'Pitahaya Red': ['Pitahaya Red'],
        'Plum': ['Plum', 'Plum 2', 'Plum 3'],
        'Pomegranate': ['Pomegranate'],
        'Pomelo Sweetie': ['Pomelo Sweetie'],
        'Potato Red': ['Potato Red'],
        'Potato Red Washed': ['Potato Red Washed'],
        'Potato Sweet': ['Potato Sweet'],
        'Potato White': ['Potato White'],
        'Quince': ['Quince'],
        'Rambutan': ['Rambutan'],
        'Raspberry': ['Raspberry'],
        'Redcurrant': ['Redcurrant'],
        'Salak': ['Salak'],
        'Strawberry': ['Strawberry'],
        'Strawberry Wedge': ['Strawberry Wedge'],
        'Tamarillo': ['Tamarillo'],
        'Tangelo': ['Tangelo'],
        'Tomato': ['Tomato 1', 'Tomato 2', 'Tomato 3', 'Tomato 4'],
        'Tomato Cherry Red': ['Tomato Cherry Red'],
        'Tomato Heart': ['Tomato Heart'],
        'Tomato Maroon': ['Tomato Maroon'],
        'Tomato not Ripened': ['Tomato not Ripened'],
        'Tomato Yellow': ['Tomato Yellow'],
        'Walnut': ['Walnut'],
        'Watermelon': ['Watermelon'],
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


import_fruit_dataset()
