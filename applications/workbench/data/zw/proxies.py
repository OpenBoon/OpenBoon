import numpy as np
import cv2
from PIL import Image
from zmlp import app_from_env


def download_proxy(asset, level):

    app = app_from_env()

    proxies = asset.get_files(category="proxy", mimetype="image/", sort_func=lambda f: f.attrs["width"])

    if not proxies:
        return None

    if level >= len(proxies):
        level = -1

    proxy = proxies[level]
    img_data = app.assets.download_file(proxy.id)
    img = np.array(Image.open(img_data))

    return img


