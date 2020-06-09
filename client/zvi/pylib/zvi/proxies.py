import numpy as np
from PIL import Image
from zmlp import app_from_env


def download_proxy(asset, level):
    """ Download image proxy

    Args:
        asset: (zmlp.entity.asset.Asset) single analyzed processed file
        level: (int) proxy image type (-1 is largest proxy)

    Returns:
        np.ndarray: img as numpy array
    """
    app = app_from_env()

    proxies = asset.get_files(category="proxy",
                              mimetype="image/",
                              sort_func=lambda f: f.attrs["width"])

    if not proxies:
        return None

    if level >= len(proxies):
        level = -1

    proxy = proxies[level]
    img_data = app.assets.download_file(proxy.id)
    img = np.array(Image.open(img_data))

    return img
