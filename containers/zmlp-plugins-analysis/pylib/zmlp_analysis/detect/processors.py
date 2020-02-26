import cv2
import cvlib as cv

from zmlp.asset import Element
from zmlpsdk import AssetProcessor
from zmlpsdk.proxy import get_proxy_level_path


class ZmlpObjectDetectionProcessor(AssetProcessor):

    namespace = "zvi.object-detection"

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 3)

        im = cv2.imread(p_path)
        bbox, labels, conf = cv.detect_common_objects(im)
        if bbox:
            for elem in zip(bbox, labels, conf):
                rect = Element.calculate_normalized_rect(im.shape[1], im.shape[0], elem[0])
                element = Element("object",
                                  self.namespace,
                                  labels=elem[1],
                                  rect=rect,
                                  score=elem[2])
                asset.add_element(element)

            asset.add_analysis(self.namespace, {"detected": len(bbox)})
