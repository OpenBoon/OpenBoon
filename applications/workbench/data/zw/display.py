
import os
import cv2
import matplotlib.pyplot as plt
from IPython.display import Image, HTML, display

from zmlp import app_from_env
from zw.proxies import download_proxy


def show_thumbnails(search=None):
    """This function displays the thumbnails for the first page of assets for a given search.
    search: Zorroa search or list of assets"""

    app = app_from_env()

    if not os.path.exists('tmp'):
        os.makedirs('tmp')

    DISPLAY_HTML = "<img style='width: 200px; height: 200px; object-fit: contain; margin: 3px; float: left; border: 2px solid black;' title='index: %d' src='%s' />"

    if search is None:
        search = app.assets.search({"size": 10})

    paths = []
    for asset in search:
        # Skip assets that are still being imported
        if asset.document['system']['state'] != 'Analyzed':
            continue

        name = 'tmp/' + str(asset.id) + '.jpg'
        paths.append(name)
        img = download_proxy(asset, 0)
        if img is None:
            continue
        img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        cv2.imwrite(name, img)

    images_list = ''.join([DISPLAY_HTML % (i, str(s))
                          for i, s in enumerate(paths)])

    display(HTML(images_list))


def show_asset(asset):
    """This function downloads and displays the largest proxy for an asset"""

    if not os.path.exists('tmp'):
        os.makedirs('tmp')

    img = download_proxy(asset, -1)

    plt.figure(figsize=(20, 10))
    plt.imshow(img)
    plt.show()
