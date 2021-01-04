from zmlpsdk import AssetProcessor, FileTypes
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path

from .util import AwsEnv


class RekognitionTextDetection(AssetProcessor):
    """Get text by line for an image using AWS Rekognition """

    namespace = 'aws-text-detection'

    # Only images, this isn't for OCR I don't think.
    file_types = FileTypes.images

    def __init__(self):
        super(RekognitionTextDetection, self).__init__()
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
        analysis = LabelDetectionAnalysis()

        for ls in self.predict(proxy_path):
            analysis.add_label_and_score(ls[0], ls[1], bbox=ls[2])

        self.add_analysis(asset, self.namespace, analysis)

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
        response = self.client.detect_text(Image=img_json)

        # get bounding box
        results = []
        bbox_result = []
        for i, r in enumerate(response['TextDetections']):
            text = r['DetectedText']
            conf = r['Confidence']
            geometry = r['Geometry']

            confidence = conf / 100.
            if geometry:
                bbox = geometry['BoundingBox']

                left = bbox['Left']
                top = bbox['Top']
                width = bbox['Width']
                height = bbox['Height']

                bbox_result = [left, top, left + width, top + height]
            results.append((text, confidence, bbox_result))

        return results
