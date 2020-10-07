from zmlpsdk import AssetProcessor, Argument, FileTypes
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path

from .util import get_clarifai_app

__all__ = [
    'ClarifaiColorDetectionProcessor',
]

models = [
    'color-model'
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

        model = getattr(self.clarifai.public_models, self.model_name.replace("-", "_"))
        response = model.predict_by_filename(p_path)
        labels = response['outputs'][0]['data'].get('colors')
        if not labels:
            return

        analysis = LabelDetectionAnalysis()
        for label in labels:
            analysis.add_label_and_score(
                label['w3c']['name'],
                label['value'],
                hex=label['w3c']['hex']
            )
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


class ClarifaiColorDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai label detection"""

    def __init__(self):
        super(ClarifaiColorDetectionProcessor, self).__init__()
        self.model_name = 'color-model'
