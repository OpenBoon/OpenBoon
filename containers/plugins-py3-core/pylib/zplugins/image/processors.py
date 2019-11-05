import math
from pathlib2 import Path
import cv2
import numpy as np
from zorroa.zsdk import DocumentProcessor, Argument


class HSVSimilarityProcessor(DocumentProcessor):
    """Compute a hash based on an hsv space histogram.

    We normalize the histogram because otherwise values just go close to zero. Not sure what this
    will do to a search.
    """
    toolTips = {
        'experimental': 'generate extra, experimental color similarity hashes.'
    }

    def __init__(self):
        super(HSVSimilarityProcessor, self).__init__()
        self.add_arg(Argument('experimental', 'boolean', default=False,
                              toolTip=self.toolTips['experimental']))

    def _process(self, frame):
        asset = frame.asset

        # Get the proxy as an opencv image
        f = Path(asset.get_thumbnail_path()).open('rb')
        arr = np.asarray(bytearray(f.read()), dtype=np.uint8)
        img = cv2.imdecode(arr, -1)

        size = 256
        max_hist_val = size ** 2  # max histogram bucket value for a SIZE x SIZE image
        hsv_range_max = [180, 256, 256]

        # this works with floats or numpy arrays! but be careful to pass in floats for the
        # range values
        # def remap (x, min1, max1, min2, max2):
        #   return min2 + (max2 - min2) * ((x - min1) / (max1 - min1))

        # encode an array of float values in the range [0,maxVal] into an
        # array of float values in the range [0,1]
        # encoding may be non-linear
        def histogram_to_encoded_normal_histogram(hist, max_val):
            # offset linear encoding: (mostly) linear histogram, a threshold of 10 pixels makes
            # the value non-zero
            # return remap(hist, 10.0, float(maxVal), 1.0/16.0, 1.0)

            # square root encoding
            return np.sqrt(hist) / math.sqrt(max_val)

            # log encoding
            # with np.errstate(divide='ignore'):
            #   return (np.where(hist != 0, np.log(hist), 0) / math.log(maxVal))

        # turn an array of float values in the range [0,1] into a hash string of chars 'a'-'p'
        def encoded_normal_histogram_to_hash_string(hex_array):
            hex_digits = 'abcdefghijklmnop'  # use a hamming-friendly encoding for hex digits
            int_clipped_array = (16.0 * hex_array).astype(int).clip(0, 15)
            return ''.join((hex_digits[x] for x in int_clipped_array))

        def histogram_to_hash_string(hist, max_val):
            return encoded_normal_histogram_to_hash_string(
                histogram_to_encoded_normal_histogram(hist, max_val))

        resized_img = cv2.resize(img, (size, size))
        if len(img.shape) < 3:
            resized_img = cv2.cvtColor(resized_img, cv2.COLOR_GRAY2BGR)

        hsv = cv2.cvtColor(resized_img, cv2.COLOR_BGR2HSV)

        # independent hsv hash (hue, sat, val are hashed separately)
        if self.arg_value('experimental'):
            # This is the length of the histogram, per channel
            hsv_hash_len = [12, 5, 3]

            def channel_hash(channel):
                hist1 = cv2.calcHist([hsv], [channel], None, [hsv_hash_len[channel]],
                                     [0, hsv_range_max[channel]])
                return histogram_to_hash_string(hist1.ravel(), max_hist_val)

            hsvhash = channel_hash(0) + channel_hash(1) + channel_hash(2)
            asset.add_analysis('hsvSimilarity', {"shash": hsvhash})

        # dependent hsv hash
        if self.arg_value('experimental'):
            dep_hsv_hash_len = [6, 3, 3]
            dep_hist = cv2.calcHist([hsv], [0, 1, 2], None, dep_hsv_hash_len, [0, hsv_range_max[0],
                                                                               0, hsv_range_max[1],
                                                                               0, hsv_range_max[2]])
            flat_dep_hist = dep_hist.ravel()  # row-major flattened view
            dep_hash = histogram_to_hash_string(flat_dep_hist, max_hist_val)
            asset.add_analysis('depHsvSimilarity', {"shash": dep_hash})

        # hue hash
        hue_hash_len = 12
        hue_hist = cv2.calcHist([hsv], [0], None, [hue_hash_len], [0, hsv_range_max[0]])
        hue_hash = histogram_to_hash_string(hue_hist.ravel(), max_hist_val)
        asset.add_analysis('hueSimilarity', {"shash": hue_hash})

        # dependent rgb hash
        if self.arg_value('experimental'):
            rgb = cv2.cvtColor(resized_img, cv2.COLOR_BGR2RGB)
            dep_rgb_hash_len = [4, 4, 4]
            dep_rgb_hist = cv2.calcHist([rgb], [0, 1, 2], None, dep_rgb_hash_len,
                                        [0, 256, 0, 256, 0, 256])
            flat_dep_rgb_hist = dep_rgb_hist.ravel()  # row-major flattened view
            dep_rgb_hash = histogram_to_hash_string(flat_dep_rgb_hist, max_hist_val)
            asset.add_analysis('depRgbSimilarity', {"shash": dep_rgb_hash})
