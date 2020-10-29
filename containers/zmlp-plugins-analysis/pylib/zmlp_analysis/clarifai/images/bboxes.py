import cv2

from zmlpsdk import AssetProcessor, Argument, FileTypes
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path

from zmlp_analysis.clarifai.util import get_clarifai_app

models = [
    'face-detection-model',
    'logo-model'
]


class AbstractClarifaiProcessor(AssetProcessor):
    """
    This base class is used for all Microsoft Computer Vision features.  Subclasses
    only have to implement the "predict(asset, image) method.
    """

    file_types = FileTypes.images | FileTypes.documents

    namespace = 'clarifai'
    model_name = 'general-model'

    def __init__(self, reactor=None):
        super(AbstractClarifaiProcessor, self).__init__()
        self.add_arg(Argument('debug', 'bool', default=False))
        self.reactor = reactor

        self.clarifai = None

    def init(self):
        self.clarifai = get_clarifai_app()

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 1)
        im = cv2.imread(p_path)
        h, w, _ = im.shape

        model = getattr(self.clarifai.public_models, self.model_name.replace("-", "_"))
        response = model.predict_by_filename(p_path)
        labels = response['outputs'][0]['data'].get('regions')
        if not labels:
            return

        analysis = LabelDetectionAnalysis()
        for label in labels:
            box = label['region_info']['bounding_box']
            bbox = [box['left_col'], box['top_row'], box['right_col'], box['bottom_row']]
            concepts = label['data'].get('concepts')[0]
            analysis.add_label_and_score(concepts['name'], concepts['value'], bbox=bbox)

        asset.add_analysis("-".join([self.namespace, self.model_name]), analysis)

    def emit_status(self, msg):
        """
        Emit a status back to the Archivist.

        Args:
            msg (str): The message to emit.

        """
        if not self.reactor:
            return
        self.reactor.emit_status(msg)


class ClarifaiFaceDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai face detection"""

    def __init__(self):
        super(ClarifaiFaceDetectionProcessor, self).__init__()
        self.model_name = 'face-detection-model'


class ClarifaiLogoDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai logo detection"""

    def __init__(self):
        super(ClarifaiLogoDetectionProcessor, self).__init__()
        self.model_name = 'logo-model'
