import cv2
from facenet_pytorch import MTCNN

from zmlpsdk import AssetProcessor
from zmlpsdk.proxy import get_proxy_level_path, calculate_normalized_bbox
from zmlpsdk.analysis import LabelDetectionAnalysis


class ZviFaceDetectionProcessor(AssetProcessor):
    """
    Simple Face Detection processor
    """
    namespace = "zvi-face-detection"

    def __init__(self):
        super(ZviFaceDetectionProcessor, self).__init__()
        self.mtcnn = None

    def init(self):
        self.mtcnn = MTCNN(image_size=160, margin=20, keep_all=True)

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 1)
        img = cv2.imread(p_path)

        rects, confidences = self.mtcnn.detect(img)

        if rects == []:
            return

        analysis = LabelDetectionAnalysis()

        for i, elem in enumerate(zip(rects, confidences)):
            rect = calculate_normalized_bbox(img.shape[1], img.shape[0], elem[0])
            analysis.add_label_and_score('face' + str(i), elem[1], bbox=rect)

        asset.add_analysis(self.namespace, analysis)