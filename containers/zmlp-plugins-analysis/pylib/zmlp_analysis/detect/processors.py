import tempfile

import cv2
import cvlib as cv
import matplotlib.pyplot as plt
from cvlib.object_detection import draw_bbox

from zmlp.asset import Element
from zmlp.analysis import AssetBuilder

import zmlp.analysis.storage as storage


class PixelMLObjectDetectionProcessor(AssetBuilder):

    def process(self, frame):
        asset = frame.asset
        p_path = storage.get_proxy_level(asset, 0)

        im = cv2.imread(p_path)
        bbox, label, conf = cv.detect_common_objects(im)
        output = draw_bbox(im, bbox, label, conf)

        # Write out file with boxes around detected objects
        # We'll use this file for all elements.
        name = "standard-object-detection.jpg"
        with tempfile.NamedTemporaryFile(suffix=".jpg") as tf:
            plt.imsave(tf.name, output)
            attrs = {"width": output.shape[1], "height": output.shape[0]}
            efile = storage.add_pixml_file(asset, tf.name, 'element',
                                           rename=name, attrs=attrs)

        for elem in zip(bbox, label, conf):
            element = Element("object", elem[1], rect=elem[0], score=elem[2], stored_file=efile)
            asset.add_element(element)
