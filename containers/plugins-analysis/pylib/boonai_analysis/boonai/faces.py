import cv2
import numpy as np
from facenet_pytorch import MTCNN, InceptionResnetV1

from boonflow import AssetProcessor
from boonflow.proxy import get_proxy_level_path, calculate_normalized_bbox
from boonflow.analysis import LabelDetectionAnalysis


class ZviFaceDetectionProcessor(AssetProcessor):
    """
    Simple Face Detection processor
    """
    namespace = "boonai-face-detection"

    def __init__(self):
        super(ZviFaceDetectionProcessor, self).__init__()
        self.mtcnn = None
        self.resnet = None

    def init(self):
        self.mtcnn = MTCNN(image_size=160, margin=20, keep_all=True)
        self.resnet = InceptionResnetV1(pretrained='vggface2').eval()

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 3)
        img = cv2.imread(p_path)

        rects, confidences = self.mtcnn.detect(img)

        if confidences[0] is None:
            return

        img_cropped = self.mtcnn(img)
        analysis = LabelDetectionAnalysis()

        for i, elem in enumerate(zip(rects, confidences)):
            # Calculate a face similarity hash
            img_embedding = self.resnet(img_cropped[i].unsqueeze(0))
            v_hash = np.clip(img_embedding.detach().numpy() + .25, 0, .5) * 50
            v_hash = v_hash.astype(int) + 65
            f_hash = "".join([chr(item) for item in v_hash[0]])

            rect = calculate_normalized_bbox(img.shape[1], img.shape[0], elem[0])
            analysis.add_label_and_score('face{}'.format(i), elem[1], bbox=rect, simhash=f_hash)

        asset.add_analysis(self.namespace, analysis)
