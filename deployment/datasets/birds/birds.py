import boonsdk
import zipfile

from tempfile import mkdtemp
from os import walk, path

# download dataset from: https://www.kaggle.com/gpiosenka/100-bird-species

batch_size = 50
zipped_file_location = path.dirname(path.realpath(__file__))
zipped_file_name = 'archive.zip'
test_ratio = 0.1
images_base_path = '/birds_rev2/'
ds_name = 'birds'


def import_birds_dataset():
    base_path = prepare_dataset_folder()

    set_base_paths = [path.join(base_path, 'train'), path.join(base_path, 'test'), path.join(base_path, 'valid')]

    label_path_dict = {
        'SNOWY OWL': ['SNOWY OWL'],
        'BLACK-CAPPED CHICKADEE': ['BLACK-CAPPED CHICKADEE'],
        'CAPUCHINBIRD': ['CAPUCHINBIRD'],
        'TRUMPTER SWAN': ['TRUMPTER SWAN'],
        'SHORT BILLED DOWITCHER': ['SHORT BILLED DOWITCHER'],
        'ROADRUNNER': ['ROADRUNNER'],
        'SHOEBILL': ['SHOEBILL'],
        'PHILIPPINE EAGLE': ['PHILIPPINE EAGLE'],
        'WILD TURKEY': ['WILD TURKEY'],
        'BOBOLINK': ['BOBOLINK'],
        'YELLOW HEADED BLACKBIRD': ['YELLOW HEADED BLACKBIRD'],
        'CACTUS WREN': ['CACTUS WREN'],
        'ROSY FACED LOVEBIRD': ['ROSY FACED LOVEBIRD'],
        'BIRD OF PARADISE': ['BIRD OF PARADISE'],
        'CRESTED AUKLET': ['CRESTED AUKLET'],
        'BEARDED REEDLING': ['BEARDED REEDLING'],
        'BLACK-NECKED GREBE': ['BLACK-NECKED GREBE'],
        'PELICAN': ['PELICAN'],
        'CEDAR WAXWING': ['CEDAR WAXWING'],
        'BLUE GROUSE': ['BLUE GROUSE'],
        'COMMON LOON': ['COMMON LOON'],
        'CUBAN TODY': ['CUBAN TODY'],
        'HOOPOES': ['HOOPOES'],
        'TREE SWALLOW': ['TREE SWALLOW'],
        'REGENT BOWERBIRD': ['REGENT BOWERBIRD'],
        'RED FACED CORMORANT': ['RED FACED CORMORANT'],
        'AMERICAN PIPIT': ['AMERICAN PIPIT'],
        'FRIGATE': ['FRIGATE'],
        'KIWI': ['KIWI'],
        'HELMET VANGA': ['HELMET VANGA'],
        'PURPLE GALLINULE': ['PURPLE GALLINULE'],
        'OVENBIRD': ['OVENBIRD'],
        'TURQUOISE MOTMOT': ['TURQUOISE MOTMOT'],
        'OCELLATED TURKEY': ['OCELLATED TURKEY'],
        'SPOONBILL': ['SPOONBILL'],
        'WHITE TAILED TROPIC': ['WHITE TAILED TROPIC'],
        'BARN SWALLOW': ['BARN SWALLOW'],
        'GAMBELS QUAIL': ['GAMBELS QUAIL'],
        'SORA': ['SORA'],
        'KING VULTURE': ['KING VULTURE'],
        'AFRICAN FIREFINCH': ['AFRICAN FIREFINCH'],
        'GOLDEN CHLOROPHONIA': ['GOLDEN CHLOROPHONIA'],
        'NORTHERN FLICKER': ['NORTHERN FLICKER'],
        'LARK BUNTING': ['LARK BUNTING'],
        'CALIFORNIA GULL': ['CALIFORNIA GULL'],
        'WHIMBREL': ['WHIMBREL'],
        'TOUCHAN': ['TOUCHAN'],
        'GUINEAFOWL': ['GUINEAFOWL'],
        'TASMANIAN HEN': ['TASMANIAN HEN'],
        'NORTHERN BALD IBIS': ['NORTHERN BALD IBIS'],
        'HOUSE FINCH': ['HOUSE FINCH'],
        'BORNEAN BRISTLEHEAD': ['BORNEAN BRISTLEHEAD'],
        'COMMON HOUSE MARTIN': ['COMMON HOUSE MARTIN'],
        'BALD EAGLE': ['BALD EAGLE'],
        'GANG GANG COCKATOO': ['GANG GANG COCKATOO'],
        'BAR-TAILED GODWIT': ['BAR-TAILED GODWIT'],
        'EMPEROR PENGUIN': ['EMPEROR PENGUIN'],
        'STORK BILLED KINGFISHER': ['STORK BILLED KINGFISHER'],
        'BLACK & YELLOW bROADBILL': ['BLACK & YELLOW bROADBILL'],
        'AMERICAN BITTERN': ['AMERICAN BITTERN'],
        'AMERICAN GOLDFINCH': ['AMERICAN GOLDFINCH'],
        'JABIRU': ['JABIRU'],
        'SPLENDID WREN': ['SPLENDID WREN'],
        'MOURNING DOVE': ['MOURNING DOVE'],
        'CROW': ['CROW'],
        'WILSONS BIRD OF PARADISE': ['WILSONS BIRD OF PARADISE'],
        'TOWNSENDS WARBLER': ['TOWNSENDS WARBLER'],
        'PARUS MAJOR': ['PARUS MAJOR'],
        'BANDED BROADBILL': ['BANDED BROADBILL'],
        'GOLDEN EAGLE': ['GOLDEN EAGLE'],
        'MALABAR HORNBILL': ['MALABAR HORNBILL'],
        'VENEZUELIAN TROUPIAL': ['VENEZUELIAN TROUPIAL'],
        'BALTIMORE ORIOLE': ['BALTIMORE ORIOLE'],
        'FLAMINGO': ['FLAMINGO'],
        'GOLDEN CHEEKED WARBLER': ['GOLDEN CHEEKED WARBLER'],
        'MALEO': ['MALEO'],
        'GOLD WING WARBLER': ['GOLD WING WARBLER'],
        'BLACK THROATED BUSHTIT': ['BLACK THROATED BUSHTIT'],
        'GUINEA TURACO': ['GUINEA TURACO'],
        'TEAL DUCK': ['TEAL DUCK'],
        'SUPERB STARLING': ['SUPERB STARLING'],
        'RAZORBILL': ['RAZORBILL'],
        'CRESTED CARACARA': ['CRESTED CARACARA'],
        'DARK EYED JUNCO': ['DARK EYED JUNCO'],
        'RED BROWED FINCH': ['RED BROWED FINCH'],
        'GREEN MAGPIE': ['GREEN MAGPIE'],
        'RUFUOS MOTMOT': ['RUFUOS MOTMOT'],
        'BORNEAN LEAFBIRD': ['BORNEAN LEAFBIRD'],
        'NORTHERN CARDINAL': ['NORTHERN CARDINAL'],
        'STRIPPED SWALLOW': ['STRIPPED SWALLOW'],
        'BROWN NOODY': ['BROWN NOODY'],
        'PEREGRINE FALCON': ['PEREGRINE FALCON'],
        'BLACK THROATED WARBLER': ['BLACK THROATED WARBLER'],
        'WOOD DUCK': ['WOOD DUCK'],
        'PAINTED BUNTIG': ['PAINTED BUNTIG'],
        'CHUKAR PARTRIDGE': ['CHUKAR PARTRIDGE'],
        'HIMALAYAN MONAL': ['HIMALAYAN MONAL'],
        'BLUE HERON': ['BLUE HERON'],
        'TURKEY VULTURE': ['TURKEY VULTURE'],
        'CURL CRESTED ARACURI': ['CURL CRESTED ARACURI'],
        'INDIGO BUNTING': ['INDIGO BUNTING'],
        'MALACHITE KINGFISHER': ['MALACHITE KINGFISHER'],
        'MASKED BOOBY': ['MASKED BOOBY'],
        'RED TAILED THRUSH': ['RED TAILED THRUSH'],
        'GILA WOODPECKER': ['GILA WOODPECKER'],
        'CALIFORNIA QUAIL': ['CALIFORNIA QUAIL'],
        'ANNAS HUMMINGBIRD': ['ANNAS HUMMINGBIRD'],
        'PALILA': ['PALILA'],
        'PYGMY KINGFISHER': ['PYGMY KINGFISHER'],
        'NORTHERN JACANA': ['NORTHERN JACANA'],
        'RING-NECKED PHEASANT': ['RING-NECKED PHEASANT'],
        'EASTERN MEADOWLARK': ['EASTERN MEADOWLARK'],
        'OKINAWA RAIL': ['OKINAWA RAIL'],
        'BLACK SWAN': ['BLACK SWAN'],
        'BROWN THRASHER': ['BROWN THRASHER'],
        'INDIAN BUSTARD': ['INDIAN BUSTARD'],
        'GREY PLOVER': ['GREY PLOVER'],
        'ALBATROSS': ['ALBATROSS'],
        'SNOWY EGRET': ['SNOWY EGRET'],
        'VARIED THRUSH': ['VARIED THRUSH'],
        'BLACK FRANCOLIN': ['BLACK FRANCOLIN'],
        'CINNAMON TEAL': ['CINNAMON TEAL'],
        'COCKATOO': ['COCKATOO'],
        'SAND MARTIN': ['SAND MARTIN'],
        'CALIFORNIA CONDOR': ['CALIFORNIA CONDOR'],
        'HOATZIN': ['HOATZIN'],
        'COMMON POORWILL': ['COMMON POORWILL'],
        'HAWAIIAN GOOSE': ['HAWAIIAN GOOSE'],
        'GRAY PARTRIDGE': ['GRAY PARTRIDGE'],
        'CANARY': ['CANARY'],
        'AMERICAN COOT': ['AMERICAN COOT'],
        'BANANAQUIT': ['BANANAQUIT'],
        'ROBIN': ['ROBIN'],
        'NORTHERN PARULA': ['NORTHERN PARULA'],
        'COMMON GRACKLE': ['COMMON GRACKLE'],
        'NORTHERN GOSHAWK': ['NORTHERN GOSHAWK'],
        'ROYAL FLYCATCHER': ['ROYAL FLYCATCHER'],
        'BLACK SKIMMER': ['BLACK SKIMMER'],
        'PARAKETT  AKULET': ['PARAKETT  AKULET'],
        'OSPREY': ['OSPREY'],
        'PINK ROBIN': ['PINK ROBIN'],
        'GOLDEN PIPIT': ['GOLDEN PIPIT'],
        'RED WINGED BLACKBIRD': ['RED WINGED BLACKBIRD'],
        'NORTHERN GANNET': ['NORTHERN GANNET'],
        'RED BELLIED PITTA': ['RED BELLIED PITTA'],
        'MYNA': ['MYNA'],
        'CASSOWARY': ['CASSOWARY'],
        'IMPERIAL SHAQ': ['IMPERIAL SHAQ'],
        'HARPY EAGLE': ['HARPY EAGLE'],
        'MIKADO  PHEASANT': ['MIKADO  PHEASANT'],
        'RUBY THROATED HUMMINGBIRD': ['RUBY THROATED HUMMINGBIRD'],
        'PURPLE MARTIN': ['PURPLE MARTIN'],
        'COMMON STARLING': ['COMMON STARLING'],
        'GO AWAY BIRD': ['GO AWAY BIRD'],
        'SPOON BILED SANDPIPER': ['SPOON BILED SANDPIPER'],
        'WHITE NECKED RAVEN': ['WHITE NECKED RAVEN'],
        'DOWNY WOODPECKER': ['DOWNY WOODPECKER'],
        'RED BEARDED BEE EATER': ['RED BEARDED BEE EATER'],
        'JAVA SPARROW': ['JAVA SPARROW'],
        'KAKAPO': ['KAKAPO'],
        'BLACK TAIL CRAKE': ['BLACK TAIL CRAKE'],
        'ARARIPE MANAKIN': ['ARARIPE MANAKIN'],
        'BELTED KINGFISHER': ['BELTED KINGFISHER'],
        'GRAY CATBIRD': ['GRAY CATBIRD'],
        'COUCHS KINGBIRD': ['COUCHS KINGBIRD'],
        'RED FACED WARBLER': ['RED FACED WARBLER'],
        'ROCK DOVE': ['ROCK DOVE'],
        'CHARA DE COLLAR': ['CHARA DE COLLAR'],
        'RAINBOW LORIKEET': ['RAINBOW LORIKEET'],
        'GREAT POTOO': ['GREAT POTOO'],
        'D-ARNAUDS BARBET': ['D-ARNAUDS BARBET'],
        'GYRFALCON': ['GYRFALCON'],
        'HORNED SUNGEM': ['HORNED SUNGEM'],
        'PARADISE TANAGER': ['PARADISE TANAGER'],
        'INCA TERN': ['INCA TERN'],
        'AFRICAN CROWNED CRANE': ['AFRICAN CROWNED CRANE'],
        'PEACOCK': ['PEACOCK'],
        'LILAC ROLLER': ['LILAC ROLLER'],
        'CHIPPING SPARROW': ['CHIPPING SPARROW'],
        'MALLARD DUCK': ['MALLARD DUCK'],
        'SRI LANKA BLUE MAGPIE': ['SRI LANKA BLUE MAGPIE'],
        'EURASIAN MAGPIE': ['EURASIAN MAGPIE'],
        'TAIWAN MAGPIE': ['TAIWAN MAGPIE'],
        'CARMINE BEE-EATER': ['CARMINE BEE-EATER'],
        'TIT MOUSE': ['TIT MOUSE'],
        'INDIAN PITTA': ['INDIAN PITTA'],
        'CRESTED NUTHATCH': ['CRESTED NUTHATCH'],
        'YELLOW BELLIED FLOWERPECKER': ['YELLOW BELLIED FLOWERPECKER'],
        'ENGGANO MYNA': ['ENGGANO MYNA'],
        'VICTORIA CROWNED PIGEON': ['VICTORIA CROWNED PIGEON'],
        'GLOSSY IBIS': ['GLOSSY IBIS'],
        'GREEN JAY': ['GREEN JAY'],
        'BULWERS PHEASANT': ['BULWERS PHEASANT'],
        'NOISY FRIARBIRD': ['NOISY FRIARBIRD'],
        'CAPE MAY WARBLER': ['CAPE MAY WARBLER'],
        'MANDRIN DUCK': ['MANDRIN DUCK'],
        'BEARDED BARBET': ['BEARDED BARBET'],
        'TAKAHE': ['TAKAHE'],
        'PUFFIN': ['PUFFIN'],
        'EVENING GROSBEAK': ['EVENING GROSBEAK'],
        'KILLDEAR': ['KILLDEAR'],
        'HOUSE SPARROW': ['HOUSE SPARROW'],
        'DOUBLE BARRED FINCH': ['DOUBLE BARRED FINCH'],
        'MASKED LAPWING': ['MASKED LAPWING'],
        'PURPLE FINCH': ['PURPLE FINCH'],
        'BARRED PUFFBIRD': ['BARRED PUFFBIRD'],
        'ANHINGA': ['ANHINGA'],
        'STRAWBERRY FINCH': ['STRAWBERRY FINCH'],
        'EURASIAN GOLDEN ORIOLE': ['EURASIAN GOLDEN ORIOLE'],
        'BARN OWL': ['BARN OWL'],
        'CROWNED PIGEON': ['CROWNED PIGEON'],
        'BLACKBURNIAM WARBLER': ['BLACKBURNIAM WARBLER'],
        'OSTRICH': ['OSTRICH'],
        'VIOLET GREEN SWALLOW': ['VIOLET GREEN SWALLOW'],
        'KOOKABURRA': ['KOOKABURRA'],
        'COCK OF THE  ROCK': ['COCK OF THE  ROCK'],
        'VERMILION FLYCATHER': ['VERMILION FLYCATHER'],
        'OYSTER CATCHER': ['OYSTER CATCHER'],
        'STEAMER DUCK': ['STEAMER DUCK'],
        'RUFOUS KINGFISHER': ['RUFOUS KINGFISHER'],
        'ASIAN CRESTED IBIS': ['ASIAN CRESTED IBIS'],
        'EASTERN ROSELLA': ['EASTERN ROSELLA'],
        'SWINHOES PHEASANT': ['SWINHOES PHEASANT'],
        'CLARKS NUTCRACKER': ['CLARKS NUTCRACKER'],
        'AMERICAN REDSTART': ['AMERICAN REDSTART'],
        'ANTBIRD': ['ANTBIRD'],
        'HORNBILL': ['HORNBILL'],
        'SAMATRAN THRUSH': ['SAMATRAN THRUSH'],
        'WHITE CHEEKED TURACO': ['WHITE CHEEKED TURACO'],
        'GOULDIAN FINCH': ['GOULDIAN FINCH'],
        'ELEGANT TROGON': ['ELEGANT TROGON'],
        'GREATOR SAGE GROUSE': ['GREATOR SAGE GROUSE'],
        'BLACK-THROATED SPARROW': ['BLACK-THROATED SPARROW'],
        'AMERICAN AVOCET': ['AMERICAN AVOCET'],
        'EASTERN TOWEE': ['EASTERN TOWEE'],
        'BLACK VULTURE': ['BLACK VULTURE'],
        'RED HEADED WOODPECKER': ['RED HEADED WOODPECKER'],
        'VULTURINE GUINEAFOWL': ['VULTURINE GUINEAFOWL'],
        'YELLOW CACIQUE': ['YELLOW CACIQUE'],
        'NORTHERN SHOVELER': ['NORTHERN SHOVELER'],
        'SCARLET IBIS': ['SCARLET IBIS'],
        'PURPLE SWAMPHEN': ['PURPLE SWAMPHEN'],
        'SMITHS LONGSPUR': ['SMITHS LONGSPUR'],
        'ROUGH LEG BUZZARD': ['ROUGH LEG BUZZARD'],
        'LEARS MACAW': ['LEARS MACAW'],
        'SCARLET MACAW': ['SCARLET MACAW'],
        'BAY-BREASTED WARBLER': ['BAY-BREASTED WARBLER'],
        'EASTERN BLUEBIRD': ['EASTERN BLUEBIRD'],
        'WATTLED CURASSOW': ['WATTLED CURASSOW'],
        'ALEXANDRINE PARAKEET': ['ALEXANDRINE PARAKEET'],
        'CASPIAN TERN': ['CASPIAN TERN'],
        'EMU': ['EMU'],
        'NORTHERN MOCKINGBIRD': ['NORTHERN MOCKINGBIRD'],
        'WHITE THROATED BEE EATER': ['WHITE THROATED BEE EATER'],
        'HORNED GUAN': ['HORNED GUAN'],
        'RED HEADED DUCK': ['RED HEADED DUCK'],
        'NORTHERN RED BISHOP': ['NORTHERN RED BISHOP'],
        'GILDED FLICKER': ['GILDED FLICKER'],
        'SPANGLED COTINGA': ['SPANGLED COTINGA'],
        'AMERICAN KESTREL': ['AMERICAN KESTREL'],
        'FLAME TANAGER': ['FLAME TANAGER'],
        'COMMON FIRECREST': ['COMMON FIRECREST'],
        'MARABOU STORK': ['MARABOU STORK'],
        'GOLDEN PHEASANT': ['GOLDEN PHEASANT'],
        'RED WISKERED BULBUL': ['RED WISKERED BULBUL'],
        'FIRE TAILLED MYZORNIS': ['FIRE TAILLED MYZORNIS'],
        'HOODED MERGANSER': ['HOODED MERGANSER'],
        'BALI STARLING': ['BALI STARLING'],
        'RED HONEY CREEPER': ['RED HONEY CREEPER'],
        'UMBRELLA BIRD': ['UMBRELLA BIRD'],
        'LONG-EARED OWL': ['LONG-EARED OWL'],
        'ELLIOTS  PHEASANT': ['ELLIOTS  PHEASANT'],
        'NICOBAR PIGEON': ['NICOBAR PIGEON'],
        'MAGPIE GOOSE': ['MAGPIE GOOSE'],
        'QUETZAL': ['QUETZAL']
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


import_birds_dataset()
