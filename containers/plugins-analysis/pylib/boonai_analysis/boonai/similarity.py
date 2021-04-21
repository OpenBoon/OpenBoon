import os
from pathlib import Path
from collections import namedtuple
import math
import numpy as np
import cv2

from boonflow import AssetProcessor
from boonflow.proxy import get_proxy_level_path
from boonai_analysis.utils.simengine import SimilarityEngine

package_directory = os.path.dirname(os.path.abspath(__file__))

Batch = namedtuple('Batch', ['data'])


class HSVSimilarityProcessor(AssetProcessor):
    """Compute a hash based on an hsv space histogram.

    We normalize the histogram because otherwise values just go close to zero. Not sure what this
    will do to a search.
    """

    namespace = "boonai-color-similarity"

    def __init__(self):
        super(HSVSimilarityProcessor, self).__init__()

    def process(self, frame):
        asset = frame.asset

        # Get the proxy as an opencv image
        f = Path(get_proxy_level_path(asset, 0)).open('rb')
        arr = np.asarray(bytearray(f.read()), dtype=np.uint8)
        img = cv2.imdecode(arr, -1)

        size = 256
        max_hist_val = size ** 2  # max histogram bucket value for a SIZE x SIZE image
        hsv_range_max = [180, 256, 256]

        # encode an array of float values in the range [0,maxVal] into an
        # array of float values in the range [0,1]
        # encoding may be non-linear
        def histogram_to_encoded_normal_histogram(hist, max_val):
            return np.sqrt(hist) / math.sqrt(max_val)

        # turn an array of float values in the range [0,1] into a hash string of chars 'A'-'P'
        def encoded_normal_histogram_to_hash_string(hex_array):
            hex_digits = 'ABCDEFGHIJKLMNOP'  # use a hamming-friendly encoding for hex digits
            int_clipped_array = (16.0 * hex_array).astype(int).clip(0, 15)
            return ''.join((hex_digits[x] for x in int_clipped_array))

        def histogram_to_hash_string(hist, max_val):
            return encoded_normal_histogram_to_hash_string(
                histogram_to_encoded_normal_histogram(hist, max_val))

        resized_img = cv2.resize(img, (size, size))
        if len(img.shape) < 3:
            resized_img = cv2.cvtColor(resized_img, cv2.COLOR_GRAY2BGR)

        hsv = cv2.cvtColor(resized_img, cv2.COLOR_BGR2HSV)

        # Make a color hash with three lenghts of hue, and two short saturation and value
        # components. The three hue component have the effect of "blending" the colors, this
        # improves matches.

        hue_hash_len = 11
        hue_hist = cv2.calcHist([hsv], [0], None, [hue_hash_len], [0, hsv_range_max[0]])
        hue_hash = histogram_to_hash_string(hue_hist.ravel(), max_hist_val)

        hue_hash_len = 7
        hue_hist = cv2.calcHist([hsv], [0], None, [hue_hash_len], [0, hsv_range_max[0]])
        hue_hash += histogram_to_hash_string(hue_hist.ravel(), max_hist_val)

        hue_hash_len = 3
        hue_hist = cv2.calcHist([hsv], [0], None, [hue_hash_len], [0, hsv_range_max[0]])
        hue_hash += histogram_to_hash_string(hue_hist.ravel(), max_hist_val)

        hue_hash_len = 3
        hue_hist = cv2.calcHist([hsv], [1], None, [hue_hash_len], [0, hsv_range_max[1]])
        hue_hash += histogram_to_hash_string(hue_hist.ravel(), max_hist_val)

        hue_hash_len = 3
        hue_hist = cv2.calcHist([hsv], [1], None, [hue_hash_len], [0, hsv_range_max[1]])
        hue_hash += histogram_to_hash_string(hue_hist.ravel(), max_hist_val)

        struct = {
            'type': 'similarity',
            'simhash': hue_hash
        }

        asset.add_analysis(self.namespace, struct)


class ZviSimilarityProcessor(AssetProcessor):
    """
    make a hash with ResNet
    """

    namespace = "boonai-image-similarity"

    # MXNet is not thread safe.
    use_threads = False

    model_path = "/models/resnet-152"

    def __init__(self):
        super(ZviSimilarityProcessor, self).__init__()
        self.engine = None

    def init(self):
        self.engine = SimilarityEngine(self.model_path)

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 0)
        mxhash = self.engine.calculate_hash(p_path)
        struct = {
            'type': 'similarity',
            'simhash': mxhash
        }

        asset.add_analysis(self.namespace, struct)
