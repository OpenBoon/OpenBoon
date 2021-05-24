import functools
import re
import uuid
import decimal
import zipfile
import os
import json
import datetime


def is_valid_uuid(val):
    """
    Return true if the given value is a valid UUID.

    Args:
        val (str): a string which might be a UUID.

    Returns:
        bool: True if UUID

    """
    try:
        uuid.UUID(str(val))
        return True
    except ValueError:
        return False


def as_collection(value):
    """If the given value is not a collection of some type, return
    the value wrapped in a list.

    Args:
        value (:obj:`mixed`):

    Returns:
        :obj:`list` of :obj:`mixed`: The value wrapped in alist.

    """
    if value is None:
        return None
    if isinstance(value, (set, list, tuple)):
        return value
    return [value]


class ObjectView:
    """
    Wraps a dictionary and provides an object based view.

    """
    snake = re.compile(r'(?<!^)(?=[A-Z])')

    def __init__(self, d):
        d = dict([(self.snake.sub('_', k).lower(), v) for k, v in d.items()])
        self.__dict__ = d


def as_id(value):
    """
    If 'value' is an object, return the 'id' property, otherwise return
    the value.  This is useful for when you need an entity's unique Id
    but the user passed in an instance of the entity.

    Args:
        value (mixed): A string o an object with an 'id' property.

    Returns:
        str: The id property.
    """
    return getattr(value, 'id', value)


def as_id_collection(value):
    """If the given value is not a collection of some type, return
    the value wrapped in a list.  Additionally entity instances
    are resolved into their unique id.

    Args:
        value (:obj:`mixed`):

    Returns:
        list: A list of entity unique ids.

    """
    if value is None:
        return None
    if isinstance(value, (set, list, tuple, dict)):
        return [getattr(it, 'id', it) for it in value]
    return [getattr(value, 'id', value)]


def as_name(value):
    """
    Return the name of an Entity or the string value of the object.

    Args:
        value (Entity): The BoonAI entity.

    Returns:
        str: The name of the eneity.
    """
    return getattr(value, 'name', str(value))


def as_name_collection(value):
    """
    Iterate values and convert to a list of names using the 'name' property.  Single
    object saree automatically wrapped in a list.

    Args:
        value (list):  A list or scalar object.

    Returns:
        list: A list of strings.
    """
    if value is None:
        return None
    if isinstance(value, (set, list, tuple)):
        return [getattr(it, 'name', it) for it in value]
    return [getattr(value, 'name', value)]


def memoize(func):
    """
    Cache the result of the given function.

    Args:
        func (function): A function to wrap.

    Returns:
        function: a wrapped function
    """
    cache = func.cache = {}

    @functools.wraps(func)
    def memoized_func(*args, **kwargs):
        key = str(args) + str(kwargs)
        if key not in cache:
            cache[key] = func(*args, **kwargs)
        return cache[key]

    return memoized_func


def truncate(number, places):
    """
    Truncate a float to the given number of places.

    Args:
        number (float): The number to truncate.
        places (int): The number of plaes to preserve.

    Returns:
        Decimal: The truncated decimal value.
    """
    if not isinstance(places, int):
        raise ValueError('Decimal places must be an integer.')
    if places < 1:
        raise ValueError('Decimal places must be at least 1.')

    with decimal.localcontext() as context:
        context.rounding = decimal.ROUND_DOWN
        exponent = decimal.Decimal(str(10 ** - places))
        return decimal.Decimal(str(number)).quantize(exponent)


def round_all(items, precision=3):
    """
    Round all items in the list.

    Args:
        items (list): A list of floats.
        precision: (int): number of decimal places.

    Returns:
        list: A rounded list.
    """
    return [round(i, precision) for i in items]


def zip_directory(src_dir, dst_file, zip_root_name=""):
    """
    A utility function for ziping a directory of files.

    Args:
        src_dir (str): The source directory.
        dst_file (str): The destination file.s
        zip_root_name (str): A optional root directory to place files in the zip.
    Returns:
        str: The dst file.

    """

    def zipdir(path, ziph, root_name):
        for root, dirs, files in os.walk(path):
            for file in files:
                if file == ".DS_Store":
                    continue
                zip_entry = os.path.join(root_name, root.replace(path, ""), file)
                ziph.write(os.path.join(root, file), zip_entry)

    src_dir = os.path.abspath(src_dir)
    zipf = zipfile.ZipFile(dst_file, 'w', zipfile.ZIP_DEFLATED)
    zipdir(src_dir + '/', zipf, zip_root_name)
    zipf.close()
    return dst_file


def denormalize_bbox(img_width, img_height, poly):
    """
    Denormalize a bounding box.
    Args:
        img_width (int): The width of the image to draw box on.
        img_height (int): The height of the image to draw box on.
        poly (list): A list of relative points.

    Returns:

    """
    result = []
    for idx, value in enumerate(poly):
        if idx % 2 == 0:
            result.append(int(poly[idx] * img_width))
        else:
            result.append(int(poly[idx] * img_height))
    return result


def to_json(obj, indent=None):
    """
    Convert the given object to a JSON string using
    the BoonSdkJsonEncoder.

    Args:
        obj (mixed): any json serializable python object.
        indent (int): The indentation level for the json, or None for compact.
    Returns:
        str: The serialized object

    """
    return json.dumps(obj, cls=BoonSdkJsonEncoder, indent=indent)


def print_json(obj, indent=2):
    """
    Convert the given object to a JSON string and print to stdout.

    Args:
        obj (mixed): any json serializable python object.
        indent (int): The indentation level for the json, or None for compact.
    Returns:
        str: The serialized object

    """
    print(json.dumps(obj, cls=BoonSdkJsonEncoder, indent=indent))


class BoonSdkJsonEncoder(json.JSONEncoder):
    """
    JSON encoder for with Boon AI specific serialization defaults.
    """

    def default(self, obj):
        if hasattr(obj, 'for_json'):
            return obj.for_json()
        elif isinstance(obj, (set, frozenset)):
            return list(obj)
        elif isinstance(obj, datetime.datetime):
            return obj.isoformat()
        elif isinstance(obj, datetime.date):
            return obj.isoformat()
        elif isinstance(obj, datetime.time):
            return obj.isoformat()
        elif isinstance(obj, decimal.Decimal):
            return float(obj)

        # Let the base class default method raise the TypeError
        return json.JSONEncoder.default(self, obj)
