import os
import logging


from boonflow.storage import file_storage
from boonflow.audio import extract_audio_file

__all__ = [
    'get_proxy_level_path',
    'get_proxy_level',
    'get_audio_proxy',
    'get_audio_proxy_uri',
    'get_video_proxy',
    'get_ocr_proxy_image'
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


def get_video_proxy(asset):
    """
    Return the video proxy or None if there is None.

    Args:
        asset (Asset): The Asset.

    Returns:
         dict: A proxy file record.
    """
    return get_proxy_level(asset, 1, "video/mp4")


def get_audio_proxy(asset, auto_create=True):
    """
    Get a StoredFile record for an audio proxy.  Optionally make
    the proxy if one does not exist.

    Args:
        asset: (Asset): The asset to find an audio proxy for.
        auto_create (bool): Make the audio proxy if one does not exist

    Returns:
        dict: A ZVI file record to the audio proxy.
    """
    audio_proxy = asset.get_files(category="audio", name="audio_proxy.flac")
    if audio_proxy:
        return audio_proxy[0]
    elif auto_create:
        audio_file = extract_audio_file(file_storage.localize_file(asset))
        if not audio_file or not os.path.exists(audio_file):
            return None

        return file_storage.assets.store_file(
                audio_file, asset, 'audio', rename='audio_proxy.flac')
    else:
        return None


def get_audio_proxy_uri(asset, auto_create=True):
    """
    Get a URI to the audio proxy.  We either have one already
    made or have to make it.

    Args:
        asset: (Asset): The asset to find an audio proxy for.
        auto_create (bool): Make the audio proxy if one does not exist

    Returns:
        str: A URI to an audio proxy or None if no proxy exists
    """
    sfile = get_audio_proxy(asset, auto_create=auto_create)
    if sfile:
        return file_storage.assets.get_native_uri(sfile)
    else:
        return None


def calculate_normalized_bbox(img_width, img_height, poly):
    """
    Calculate points for normalized bounding box based on the given
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


def get_ocr_proxy_image(asset):
    """
    Choose a proper proxy image effort OCR.

    Args:
        asset (Asset): The asset to look at.

    Returns:
        StoredFile: A StoredFile instance.
    """
    ocr_proxy = asset.get_files(category='ocr-proxy')
    if ocr_proxy:
        return file_storage.localize_file(ocr_proxy[0])
    else:
        return get_proxy_level_path(asset, 3)
