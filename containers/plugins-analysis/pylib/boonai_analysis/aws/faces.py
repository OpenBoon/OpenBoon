from boonflow import AssetProcessor, FileTypes
from boonflow.analysis import LabelDetectionAnalysis
from boonflow.proxy import get_proxy_level_path

from .util import AwsEnv


class RekognitionFaceDetection(AssetProcessor):
    """Get labels for an image using AWS Rekognition """

    namespace = 'aws-face-detection'

    file_types = FileTypes.documents | FileTypes.images

    def __init__(self):
        super(RekognitionFaceDetection, self).__init__()
        self.client = None

    def init(self):
        # AWS client
        self.client = AwsEnv.rekognition()

    def process(self, frame):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        """
        asset = frame.asset
        proxy_path = get_proxy_level_path(asset, 0)
        analysis = LabelDetectionAnalysis(min_score=0.01)

        for ls in self.predict(proxy_path):
            analysis.add_label_and_score(ls[0], ls[1], bbox=ls[2])

        asset.add_analysis(self.namespace, analysis)

    def predict(self, path):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path

        Returns:
            list: a list of predictions
        """
        with open(path, 'rb') as f:
            source_bytes = f.read()

        # get predictions
        img_json = {'Bytes': source_bytes}
        response = self.client.detect_faces(Image=img_json)

        # get bounding box
        results = []
        for i, r in enumerate(response['FaceDetails']):
            conf = r['Confidence']
            bbox = r['BoundingBox']

            left = bbox['Left']
            top = bbox['Top']
            width = bbox['Width']
            height = bbox['Height']

            confidence = conf / 100.
            results.append(("face{}".format(i), confidence, [left, top, left+width, top+height]))
        return results
