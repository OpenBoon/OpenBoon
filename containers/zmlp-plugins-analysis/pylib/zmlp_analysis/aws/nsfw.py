from zmlpsdk import AssetProcessor, FileTypes
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path

from .util import AwsEnv


class RekognitionUnsafeDetection(AssetProcessor):
    """Get labels for an image using AWS Rekognition """

    namespace = 'aws-unsafe-detection'

    file_types = FileTypes.documents | FileTypes.images

    def __init__(self):
        super(RekognitionUnsafeDetection, self).__init__()
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

        for ls in self.predict(proxy_path, int(analysis.min_score)):
            analysis.add_label_and_score(ls[0], ls[1])

        asset.add_analysis(self.namespace, analysis)

    def predict(self, path, min_score):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path
            min_score (int): miminum confidence value

        Returns:
            list: a list of predictions
        """
        with open(path, 'rb') as f:
            source_bytes = f.read()

        # get predictions
        img_json = {'Bytes': source_bytes}
        response = self.client.detect_moderation_labels(
            Image=img_json,
            MinConfidence=min_score
        )

        # get list of labels
        return [(r['Name'], r['Confidence'] / 100.) for r in response['ModerationLabels']]
