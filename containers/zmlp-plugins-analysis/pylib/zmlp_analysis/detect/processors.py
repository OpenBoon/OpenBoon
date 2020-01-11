import cv2
import cvlib as cv
from cvlib.object_detection import draw_bbox

from zmlp.analysis import AssetProcessor
from zmlp.analysis.proxy import get_proxy_level, store_element_proxy
from zmlp.elements import Element

NAMESPACE = "zmlpObjectDetection"


class ZmlpObjectDetectionProcessor(AssetProcessor):

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level(asset, 3)

        im = cv2.imread(p_path)
        bbox, labels, conf = cv.detect_common_objects(im)
        if bbox:
            output = store_element_proxy(asset, draw_bbox(im, bbox, labels, conf), NAMESPACE)

            for elem in zip(bbox, labels, conf):
                element = Element("object",
                                  NAMESPACE,
                                  labels=elem[1],
                                  rect=elem[0],
                                  score=elem[2],
                                  proxy=output)
                asset.add_element(element)
