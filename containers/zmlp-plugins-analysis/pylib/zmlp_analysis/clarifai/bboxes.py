import cv2

from zmlpsdk import AssetProcessor, Argument
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path

from .util import get_clarifai_app

models = [
    'face-detection-model',
    'logo-model'
]


class ClarifaiBoundingBoxDetectionProcessor(AssetProcessor):
    namespace = 'clarifai-bbox'

    def __init_(self):
        super(ClarifaiBoundingBoxDetectionProcessor, self).__init__()
        for model in models:
            self.add_arg(Argument(model, "boolean", required=False,
                                  toolTip="Enable the {} model".format(model)))
        self.clarifai = None

    def init(self):
        self.clarifai = get_clarifai_app()

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 1)
        im = cv2.imread(p_path)
        h, w, _ = im.shape

        for model_name in models:
            if self.arg_value(model_name):
                model = getattr(self.clarifai.public_models, model_name.replace("-", "_"))
                response = model.predict_by_filename(p_path)
                labels = response['outputs'][0]['data'].get('regions')
                if not labels:
                    continue

                analysis = LabelDetectionAnalysis()
                for label in labels:
                    box = label['region_info']['bounding_box']
                    bbox = self.get_bbox(box=box, height=h, width=w)
                    concepts = label['data'].get('concepts')[0]
                    analysis.add_label_and_score(concepts['name'], concepts['value'], bbox=bbox)

                asset.add_analysis("-".join([self.namespace, model_name]), analysis)

    def get_bbox(self, box, height, width):
        """ Get Bounding Box from Clarifai regions

        Args:
            box: (dict) bounding box top/bottom row, left/right col
            height: image height
            width: image width

        Returns:
            list[str] bounding box in [x, y, w, h]
        """
        top = box['top_row']
        bottom = box['bottom_row']
        left = box['left_col']
        right = box['right_col']

        x = left * width
        y = top * height
        w = (right * width) - x
        h = (bottom * height) - y
        return [x, y, w, h]
