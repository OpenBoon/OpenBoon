import cv2
import numpy as np

from boonflow import AssetProcessor
from boonflow.proxy import get_proxy_level_path


class TiledColorSimilarity(AssetProcessor):
    """
    Creates a similarity hash which describes the HSV values for a 100x100 image.

    """
    def process(self, frame):
        asset = frame.asset

        img = cv2.imread(get_proxy_level_path(asset, 0))
        img = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
        img = cv2.resize(img, (100, 100), interpolation=cv2.INTER_AREA)

        img_height = img.shape[0]
        img_width = img.shape[0]

        m = img_height // 10
        n = img_width // 10

        criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 200, .1)
        flags = cv2.KMEANS_RANDOM_CENTERS

        colors = []
        for y in range(0, img_height, m):
            for x in range(0, img_width, n):
                tile = img[y:y + m, x:x + n]
                pixels = np.float32(tile.reshape(-1, 3))

                _, labels, palette = cv2.kmeans(pixels, 1, None, criteria, 10, flags)
                _, counts = np.unique(labels, return_counts=True)
                dom = palette[np.argmax(counts)]
                colors.extend((dom[0] / 180.0, dom[1] / 256.0, dom[2] / 256.0))

        ary = np.array(colors)
        mxh = np.clip((ary * 16).astype(int), 0, 15) + 65
        simhash = "".join([chr(f) for f in mxh])
        struct = {
            'type': 'similarity',
            'simhash': simhash
        }
        asset.add_analysis("boonai-tiled-color-similarity", struct)
