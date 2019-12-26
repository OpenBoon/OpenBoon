import os

from zmlp.analysis.storage import file_storage


def store_asset_proxy(asset, path, size):
    """
    A convenience function that adds a proxy file to the Asset and
    uploads the file to ZMLP storage.

    Args:
        asset (Asset): The purpose of the file, ex proxy.
        path (str): The local path to the file.
        size (tuple of int): a tuple of width, height

    Returns:
        dict: a ZMLP file storage dict.
    """
    _, ext = os.path.splitext(path)
    if not ext:
        raise ValueError("The path to the proxy file has no extension, but one is required.")
    name = "proxy_{}x{}{}".format(size[0], size[1], ext)
    return file_storage.store_asset_file(asset, path, "proxy", rename=name,
                                         attrs={"width": size[0], "height": size[1]})


def get_proxy_min_width(asset, min_width, mimetype="image/", fallback=False):
    """
    Return a tuple containing a suitable proxy file or fallback to the source media.
    The first element of the tuple is the name of proxy file such as "proxy_200x200.jpg"
    or simply "source" if the source was selected.

    Args:
        asset (Asset): an Asset instance
        min_width (int): The minimum width to accept for the proxy.
        mimetype (str): A mimetype filter, returns only files that start with this filter.
        fallback (bool): Fallback to the source if the proxy is not available.

    Returns:
        str: A path to the localized proxy file or None on no match.

    """
    files = asset.get_files(mimetype=mimetype, category="proxy", attr_keys=["width"],
                            sort_func=lambda f: f['attrs']['width'])
    # Trim out smaller ones
    files = [file for file in files if file["attrs"]["width"] >= min_width]

    if files:
        return file_storage.localize_asset_file(asset, files[0])
    elif fallback:
        return file_storage.localize_remote_file(asset)
    else:
        raise ValueError("No suitable proxy file was found.")


def get_proxy_level(asset, level, mimetype="image/"):
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
    files = asset.get_files(mimetype=mimetype, category="proxy", attr_keys=["width"],
                            sort_func=lambda f: f['attrs']['width'])
    if level >= len(files):
        level = -1
    try:
        proxy = files[level]
        return file_storage.localize_asset_file(asset, proxy)
    except IndexError:
        return None
