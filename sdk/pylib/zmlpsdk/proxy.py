import logging

from zmlpsdk.storage import file_storage

__all__ = [
    'get_proxy_level_path',
    'get_proxy_level'
]

logger = logging.getLogger(__name__)


def get_proxy_level_path(asset, level, mimetype="image/"):
    """
    Localize and return the given proxy level.  The smallest proxy is
    level 0, the largest proxy is 0 or greater.  Out of bounds level
    values will be clamped to the correct range automatically.  For example
    if there are only 2 proxies and you pass level 3, then you will get the
    level 2 proxy.

    Args:
        asset (Asset): The Asset.
        level (int): The proxy level, the larger the number the bigger the file.
        mimetype (str): The proxy mimetype, defaults to image/

    Returns:
        str: A path to the localized proxy file or None on no match.

    """
    files = asset.get_files(mimetype=mimetype, category='proxy', attr_keys=['width'],
                            sort_func=lambda f: f.attrs.get('width', 0))
    if level >= len(files):
        level = -1
    try:
        proxy = files[level]
        return file_storage.localize_file(proxy)
    except IndexError:
        logger.warning("No proxies found for {}".format(asset))
        return None


def get_proxy_level(asset, level, mimetype="image/"):
    """
    Return the given proxy level record. The smallest proxy is level 0,
    the largest proxy is 0 or greater. Calling this method does not localize
    the proxy.

    Args:
        asset: (Asset): The Asset.
        level (int): The proxy level identifier, 0 for smallest, 1 for middle, etc.
        mimetype: (str): A mimetype filter, defaults to image/

    Returns:
        dict: A proxy file record.
    """
    files = asset.get_files(mimetype=mimetype, category='proxy', attr_keys=['width'],
                            sort_func=lambda f: f.attrs.get('width', 0))
    if level >= len(files):
        level = -1
    try:
        return files[level]
    except IndexError:
        return None


def calculate_normalized_bbox(img_width, img_height, poly):
    """
    Calculate points for normalized bouding box based on the given
    image width and height.

    Args:
        img_width (int): The width of the image the rect was calculated in.
        img_height (int): The height of the image the rect was calculated in.
        poly: (list): An array of 4 points, assuming x,y,x,y,x,y..

    Returns:
        list<float> An array of points for a normalized rectangle.

    """
    result = []
    for idx, value in enumerate(poly):
        if idx % 2 == 0:
            result.append(round(poly[idx] / float(img_width), 3))
        else:
            result.append(round(poly[idx] / float(img_height), 3))
    return result
