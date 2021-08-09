from .env import app_instance

__all__ = [
    'get_proxy_image',
    'get_proxy_record'
]


def get_proxy_image(asset, level):
    """
    Download and return the raw proxy image BytesIO or None if no proxy exists.

    Args:
        asset (Asset): The asset.
        level (int): The proxy level identifier, 0 for smallest, 1 for middle, etc.

    Returns:
        io.BytesIO: The bytes of the proxy image.

    """
    prx = get_proxy_record(asset, level)
    if not prx:
        return None
    return app_instance().assets.download_file(prx)


def get_proxy_record(asset, level):
    """
    Return the given proxy level record. The smallest proxy is level 0,
    the largest proxy is 0 or greater. Calling this method does not localize
    the proxy.

    Args:
        asset: (Asset): The Asset.
        level (int): The proxy level identifier, 0 for smallest, 1 for middle, etc.

    Returns:
        StoredFile:
    """
    files = asset.get_files(mimetype="image/", category='proxy', attr_keys=['width'],
                            sort_func=lambda f: f.attrs.get('width', 0))
    if level >= len(files):
        level = -1
    try:
        return files[level]
    except IndexError:
        return None
