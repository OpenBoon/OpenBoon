from zmlp.util import round_all

from zmlpsdk import AssetProcessor, FileTypes
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path

from .util import AwsEnv


class RekognitionCelebrityDetection(AssetProcessor):
    """Get labels for an image using AWS Rekognition """

    namespace = 'aws-celebrity-detection'

    file_types = FileTypes.documents | FileTypes.images

    def __init__(self):
        super(RekognitionCelebrityDetection, self).__init__()
        self.client = None

    def init(self):
        # AWS client
        self.client = AwsEnv.rekognition()

    def process(self, frame):
        asset = frame.asset
        proxy_path = get_proxy_level_path(asset, 0)
        analysis = LabelDetectionAnalysis(min_score=0.01)

        result = self.predict(proxy_path)
        for person in result['CelebrityFaces']:

            name = person['Name']
            face = person['Face']
            conf = face['Confidence']
            bbox = face['BoundingBox']

            left = bbox['Left']
            top = bbox['Top']
            width = bbox['Width']
            height = bbox['Height']

            confidence = conf / 100.
            analysis.add_label_and_score(name, confidence,
                                         bbox=round_all([left, top, left+width, top+height]))

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
        return self.client.recognize_celebrities(Image=img_json)
