from zmlpsdk import AssetProcessor, FileTypes
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path

from .util import AwsEnv


class RekognitionPPEDetection(AssetProcessor):
    """Get labels for an image using AWS Rekognition """

    namespace = 'aws-ppe-detection'

    file_types = FileTypes.documents | FileTypes.images

    def __init__(self):
        super(RekognitionPPEDetection, self).__init__()
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
        analysis = LabelDetectionAnalysis(min_score=0.01, collapse_labels=True)

        for ls in self.predict(proxy_path):
            analysis.add_label_and_score(ls[0], ls[1], bbox=ls[2], body_part=ls[3], type=ls[4])

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
        response = self.client.detect_protective_equipment(Image=img_json)

        # get bounding box
        results = []
        for i, r in enumerate(response['Persons']):
            for bp in r['BodyParts']:
                name = bp['Name']
                for eq in bp['EquipmentDetections']:
                    try:
                        equipment_type = eq['Type']
                        conf = eq['Confidence']
                        bbox = eq['BoundingBox']

                        left = bbox['Left']
                        top = bbox['Top']
                        width = bbox['Width']
                        height = bbox['Height']

                        bounding_box = [left, top, left+width, top+height]
                    except IndexError:
                        continue
                    confidence = conf / 100.
                    results.append((f"person{i}", confidence, bounding_box, name, equipment_type))
        return results
