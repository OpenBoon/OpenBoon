import cv2
import cvlib as cv

from zmlpsdk import AssetProcessor
from zmlpsdk.proxy import get_proxy_level_path, calculate_normalized_bbox
from zmlpsdk.analysis import LabelDetectionAnalysis


class ZviObjectDetectionProcessor(AssetProcessor):

    namespace = "zvi-object-detection"

    use_threads = False

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 1)

        im = cv2.imread(p_path)
        bbox, labels, conf = cv.detect_common_objects(im)

        if not bbox:
            return

        analysis = LabelDetectionAnalysis()
        for elem in zip(bbox, labels, conf):
            rect = calculate_normalized_bbox(im.shape[1], im.shape[0], elem[0])
            analysis.add_label_and_score(elem[1], elem[2], bbox=rect)

        asset.add_analysis(self.namespace, analysis)
