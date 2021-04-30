from PIL import Image
import numpy as np
from facenet_pytorch import MTCNN, InceptionResnetV1

from boonflow import AssetProcessor, Singleton
from boonflow.proxy import get_proxy_level_path, calculate_normalized_bbox
from boonflow.analysis import LabelDetectionAnalysis
from boonflow import FileTypes


class ZviFaceDetectionProcessor(AssetProcessor):
    """
    Simple Face Detection processor
    """
    namespace = 'boonai-face-detection'

    # Running in video just detects a face in the proxy
    # which means results are ambitious and inconsistent.
    file_types = FileTypes.documents | FileTypes.images

    def __init__(self):
        super(ZviFaceDetectionProcessor, self).__init__()
        self.engine = None

    def init(self):
        self.engine = MtCnnFaceDetectionEngine()

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 3)
        asset.add_analysis(self.namespace,  self.engine.get_analysis(p_path))


class MtCnnFaceDetectionEngine(metaclass=Singleton):
    """
    A class for sharing the MTCNN face detection algorithm.
    """
    def __init__(self):
        self.mtcnn = MTCNN(image_size=160, margin=20, keep_all=True)
        self.resnet = InceptionResnetV1(pretrained='vggface2').eval()

    def detect(self, path):
        result = []
        img = Image.open(path).convert('RGB')
        img_cropped = self.mtcnn(img)
        rects, confidences = self.mtcnn.detect(img)

        if rects is None:
            return result

        for i, item in enumerate(zip(rects, confidences)):
            img_embedding = self.resnet(img_cropped[i].unsqueeze(0))
            v_hash = np.clip(img_embedding.detach().numpy() + .25, 0, .5) * 50
            v_hash = v_hash.astype(int) + 65
            f_hash = ''.join([chr(item) for item in v_hash[0]])

            rect = calculate_normalized_bbox(img.size[0], img.size[1], item[0])
            result.append({'bbox': rect, 'score': item[1], 'simhash': f_hash})

        return result

    def get_analysis(self, obj):
        analysis = LabelDetectionAnalysis()
        for i, elem in enumerate(self.detect(obj)):
            analysis.add_label_and_score('face{}'.format(i),
                                         elem['score'], bbox=elem['bbox'], simhash=elem['simhash'])
        return analysis
