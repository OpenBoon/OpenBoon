import os
import cv2
import matplotlib.pyplot as plt
from IPython.display import HTML, display

from boonsdk import app_from_env
from .proxies import download_proxy

DISPLAY_HTML = "<img style='width: 200px; height: 200px; object-fit: contain; " \
               "margin: 3px; float: left; border: 2px solid black;' title='index: %d' src='%s' />"


def is_running_from_ipython():
    """Check that script is running from Jupyter notebook

    Returns:
        bool: True if in notebook otherwise False
    """
    from IPython import get_ipython
    return get_ipython() is not None


def show_thumbnails(search=None):
    """This function displays the thumbnails for the first page of assets for a given search.

    Args:
        search: Zorroa search or list of assets

    Returns:
        List[str]: list of image paths
    """
    app = app_from_env()

    if not os.path.exists('tmp'):
        os.makedirs('tmp')

    if search is None:
        search = app.assets.search({"size": 10})

    paths = []
    for entity in search:
        name = 'tmp/' + str(entity.id) + '.jpg'
        paths.append(name)
        img = download_proxy(entity, 0)
        if img is None:
            continue
        img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        cv2.imwrite(name, img)

    images_list = ''.join([DISPLAY_HTML % (i, str(s)) for i, s in enumerate(paths)])

    if is_running_from_ipython():
        display(HTML(images_list))

    return paths


def show_asset(asset):
    """This function downloads and displays the largest proxy for an asset

    Args:
        asset: (boonsdk.entity.asset.Asset) single analyzed processed file

    Returns:
        dict: asset's metadata
    """
    img = download_proxy(asset, -1)

    if is_running_from_ipython():
        plt.figure(figsize=(20, 10))
        plt.imshow(img)
        plt.show()

    return asset.document
