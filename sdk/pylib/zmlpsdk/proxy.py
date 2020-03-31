import os

from zmlpsdk.storage import file_storage

__all__ = [
    'store_asset_proxy',
    'get_proxy_level_path',
    'get_proxy_level'
]


def store_asset_proxy(asset, path, size, type="image", attrs=None):
    """
    A convenience function that adds a proxy file to the Asset and
    uploads the file to ZMLP storage.

    Args:
        asset (Asset): The purpose of the file, ex proxy.
        path (str): The local path to the file.
        size (tuple of int): a tuple of width, height
        type (str): The media type
        attrs (dict): Additional media attrs
    Returns:
        dict: a ZMLP file storage dict.
    """
    _, ext = os.path.splitext(path)
    if not ext:
        raise ValueError('The path to the proxy file has no extension, but one is required.')
    name = '{}_{}x{}{}'.format(type, size[0], size[1], ext)
    proxy_attrs = asset.get_attr('tmp.{}_proxy_source_attrs'.format(type)) or {}
    proxy_attrs['width'] = size[0]
    proxy_attrs['height'] = size[1]
    if attrs:
        proxy_attrs.update(attrs)

    return file_storage.assets.store_file(asset, path, 'proxy', rename=name, attrs=proxy_attrs)


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
