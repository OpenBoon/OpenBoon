import numpy as np
from PIL import Image
import zmlp
from zmlp import app_from_env


def download_proxy(entity, level=-1):
    """ Download image proxy from an asset or clip

    Args:
        entity: (zmlp.entity.asset.Asset or zmlp.entity.clip.VideoClip)
                asset or clip to get a proxy for
        level: (int) proxy image type (-1 is largest proxy)

    Returns:
        np.ndarray: img as numpy array
    """
    app = app_from_env()

    if type(entity) == zmlp.entity.asset.Asset:
        proxies = entity.get_files(category="proxy",
                                  mimetype="image/",
                                  sort_func=lambda f: f.attrs["width"])

        if not proxies:
            return None

        if level >= len(proxies):
            level = -1

        proxy = proxies[level]
        img_data = app.assets.download_file(proxy.id)

    elif type(entity) == zmlp.entity.clip.VideoClip:
        img_data = app.assets.download_file(entity.files[0])

    if not img_data:
        return None
    else:
        img = np.array(Image.open(img_data))
        return img
