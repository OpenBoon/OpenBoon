import boonsdk
import utils
from os import walk, path

# download dataset from: https://www.kaggle.com/jessicali9530/stanford-dogs-dataset

# UPDATABLE
DS_NAME = 'Dog Breeds'
TEST_RATIO = 0.1
BATCH_SIZE = 50

# DO NOT CHANGE
zipped_file_location = path.dirname(path.realpath(__file__))
zipped_file_name = 'archive.zip'
images_base_path = 'images/Images/'


def import_dog_dataset():
    base_path = utils.prepare_dataset_folder(
        images_base_path, zipped_file_location, zipped_file_name)

    label_path_dict = {
        'Border_terrier': 'n02093754-Border_terrier',
        'Sussex_spaniel': 'n02102480-Sussex_spaniel',
        'Border_collie': 'n02106166-Border_collie',
        'curly-coated_retriever': 'n02099429-curly-coated_retriever',
        'whippet': 'n02091134-whippet',
        'Newfoundland': 'n02111277-Newfoundland',
        'kuvasz': 'n02104029-kuvasz',
        'dhole': 'n02115913-dhole',
        'Pembroke': 'n02113023-Pembroke',
        'basenji': 'n02110806-basenji',
        'African_hunting_dog': 'n02116738-African_hunting_dog',
        'English_springer': 'n02102040-English_springer',
        'dingo': 'n02115641-dingo',
        'EntleBucher': 'n02108000-EntleBucher',
        'Ibizan_hound': 'n02091244-Ibizan_hound',
        'German_short-haired_pointer': 'n02100236-German_short-haired_pointer',
        'bloodhound': 'n02088466-bloodhound',
        'Brittany_spaniel': 'n02101388-Brittany_spaniel',
        'Norwich_terrier': 'n02094258-Norwich_terrier',
        'Tibetan_terrier': 'n02097474-Tibetan_terrier',
        'Greater_Swiss_Mountain_dog': 'n02107574-Greater_Swiss_Mountain_dog',
        'Irish_water_spaniel': 'n02102973-Irish_water_spaniel',
        'cocker_spaniel': 'n02102318-cocker_spaniel',
        'Yorkshire_terrier': 'n02094433-Yorkshire_terrier',
        'Saluki': 'n02091831-Saluki',
        'collie': 'n02106030-collie',
        'Scotch_terrier': 'n02097298-Scotch_terrier',
        'English_setter': 'n02100735-English_setter',
        'Italian_greyhound': 'n02091032-Italian_greyhound',
        'wire-haired_fox_terrier': 'n02095314-wire-haired_fox_terrier',
        'Samoyed': 'n02111889-Samoyed',
        'Doberman': 'n02107142-Doberman',
        'Old_English_sheepdog': 'n02105641-Old_English_sheepdog',
        'Scottish_deerhound': 'n02092002-Scottish_deerhound',
        'miniature_pinscher': 'n02107312-miniature_pinscher',
        'papillon': 'n02086910-papillon',
        'Australian_terrier': 'n02096294-Australian_terrier',
        'standard_schnauzer': 'n02097209-standard_schnauzer',
        'komondor': 'n02105505-komondor',
        'American_Staffordshire_terrier': 'n02093428-American_Staffordshire_terrier',
        'Shih-Tzu': 'n02086240-Shih-Tzu',
        'vizsla': 'n02100583-vizsla',
        'Siberian_husky': 'n02110185-Siberian_husky',
        'Great_Pyrenees': 'n02111500-Great_Pyrenees',
        'Japanese_spaniel': 'n02085782-Japanese_spaniel',
        'Shetland_sheepdog': 'n02105855-Shetland_sheepdog',
        'Rhodesian_ridgeback': 'n02087394-Rhodesian_ridgeback',
        'Labrador_retriever': 'n02099712-Labrador_retriever',
        'redbone': 'n02090379-redbone',
        'Great_Dane': 'n02109047-Great_Dane',
        'cairn': 'n02096177-cairn',
        'clumber': 'n02101556-clumber',
        'Norfolk_terrier': 'n02094114-Norfolk_terrier',
        'Staffordshire_bullterrier': 'n02093256-Staffordshire_bullterrier',
        'English_foxhound': 'n02089973-English_foxhound',
        'Bedlington_terrier': 'n02093647-Bedlington_terrier',
        'Eskimo_dog': 'n02109961-Eskimo_dog',
        'Lakeland_terrier': 'n02095570-Lakeland_terrier',
        'Chihuahua': 'n02085620-Chihuahua',
        'Walker_hound': 'n02089867-Walker_hound',
        'Boston_bull': 'n02096585-Boston_bull',
        'German_shepherd': 'n02106662-German_shepherd',
        'schipperke': 'n02104365-schipperke',
        'Sealyham_terrier': 'n02095889-Sealyham_terrier',
        'Chesapeake_Bay_retriever': 'n02099849-Chesapeake_Bay_retriever',
        'affenpinscher': 'n02110627-affenpinscher',
        'toy_terrier': 'n02087046-toy_terrier',
        'Irish_terrier': 'n02093991-Irish_terrier',
        'Pekinese': 'n02086079-Pekinese',
        'Airedale': 'n02096051-Airedale',
        'Brabancon_griffon': 'n02112706-Brabancon_griffon',
        'Pomeranian': 'n02112018-Pomeranian',
        'pug': 'n02110958-pug',
        'black-and-tan_coonhound': 'n02089078-black-and-tan_coonhound',
        'otterhound': 'n02091635-otterhound',
        'Rottweiler': 'n02106550-Rottweiler',
        'Irish_wolfhound': 'n02090721-Irish_wolfhound',
        'boxer': 'n02108089-boxer',
        'briard': 'n02105251-briard',
        'kelpie': 'n02105412-kelpie',
        'Welsh_springer_spaniel': 'n02102177-Welsh_springer_spaniel',
        'Dandie_Dinmont': 'n02096437-Dandie_Dinmont',
        'Afghan_hound': 'n02088094-Afghan_hound',
        'Weimaraner': 'n02092339-Weimaraner',
        'beagle': 'n02088364-beagle',
        'Blenheim_spaniel': 'n02086646-Blenheim_spaniel',
        'bluetick': 'n02088632-bluetick',
        'Maltese_dog': 'n02085936-Maltese_dog',
        'West_Highland_white_terrier': 'n02098286-West_Highland_white_terrier',
        'miniature_schnauzer': 'n02097047-miniature_schnauzer',
        'Mexican_hairless': 'n02113978-Mexican_hairless',
        'toy_poodle': 'n02113624-toy_poodle',
        'Cardigan': 'n02113186-Cardigan',
        'groenendael': 'n02105056-groenendael',
        'malamute': 'n02110063-malamute',
        'golden_retriever': 'n02099601-golden_retriever',
        'keeshond': 'n02112350-keeshond',
        'Norwegian_elkhound': 'n02091467-Norwegian_elkhound',
        'Lhasa': 'n02098413-Lhasa',
        'flat-coated_retriever': 'n02099267-flat-coated_retriever',
        'malinois': 'n02105162-malinois',
        'Bouvier_des_Flandres': 'n02106382-Bouvier_des_Flandres',
        'miniature_poodle': 'n02113712-miniature_poodle',
        'French_bulldog': 'n02108915-French_bulldog',
        'basset': 'n02088238-basset',
        'chow': 'n02112137-chow',
        'standard_poodle': 'n02113799-standard_poodle',
        'Kerry_blue_terrier': 'n02093859-Kerry_blue_terrier',
        'Gordon_setter': 'n02101006-Gordon_setter',
        'Irish_setter': 'n02100877-Irish_setter',
        'silky_terrier': 'n02097658-silky_terrier',
        'borzoi': 'n02090622-borzoi',
        'Tibetan_mastiff': 'n02108551-Tibetan_mastiff',
        'bull_mastiff': 'n02108422-bull_mastiff',
        'soft-coated_wheaten_terrier': 'n02098105-soft-coated_wheaten_terrier',
        'Saint_Bernard': 'n02109525-Saint_Bernard',
        'Appenzeller': 'n02107908-Appenzeller',
        'giant_schnauzer': 'n02097130-giant_schnauzer',
        'Leonberg': 'n02111129-Leonberg',
        'Bernese_mountain_dog': 'n02107683-Bernese_mountain_dog',
    }

    app = boonsdk.app_from_env()
    assets = []

    ds = app.datasets.create_dataset(DS_NAME, boonsdk.DatasetType.Classification)

    for key in label_path_dict:
        for (dirpath, dirnames, filenames) in walk(path.join(base_path, label_path_dict[key])):
            test_count = int(TEST_RATIO * len(filenames)) + 1

            sanitized_label = utils.sanitize_label(key)
            test_label = ds.make_label(sanitized_label, scope=boonsdk.LabelScope.TEST)
            assets.extend([boonsdk.FileUpload(path.join(dirpath, name), label=test_label)
                           for name in filenames[0:test_count]])

            train_label = ds.make_label(sanitized_label, scope=boonsdk.LabelScope.TRAIN)
            assets.extend([boonsdk.FileUpload(path.join(dirpath, name), label=train_label)
                           for name in filenames[test_count:]])

    utils.print_dataset_info(DS_NAME, len(assets), TEST_RATIO)

    assets = [assets[offs:offs + BATCH_SIZE] for offs in range(0, len(assets), BATCH_SIZE)]

    for batch in assets:
        app.assets.batch_upload_files(batch)


import_dog_dataset()
