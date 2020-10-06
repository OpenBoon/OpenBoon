from zmlpsdk import AssetProcessor, Argument, FileTypes
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path

from .util import get_clarifai_app

__all__ = [
    'ClarifaiLabelDetectionProcessor',
    'ClarifaiFoodDetectionProcessor',
    'ClarifaiApparelDetectionProcessor',
    'ClarifaiWeddingDetectionProcessor',
    'ClarifaiExplicitDetectionProcessor',
    'ClarifaiModerationDetectionProcessor',
    'ClarifaiTexturesDetectionProcessor',
]

models = [
    'apparel-model',
    'food-model',
    'general-model',
    'moderation-model',
    'nsfw-model',
    'textures-and-patterns-model',
    'travel-model',
    'wedding-model',
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
        labels = response['outputs'][0]['data'].get('concepts')
        if not labels:
            return

        analysis = LabelDetectionAnalysis()
        [analysis.add_label_and_score(label['name'], label['value']) for label in labels]
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


class ClarifaiLabelDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai label detection"""

    def __init__(self):
        super(ClarifaiLabelDetectionProcessor, self).__init__()
        self.model_name = 'general-model'


class ClarifaiFoodDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai food detection"""

    def __init__(self):
        super(ClarifaiFoodDetectionProcessor, self).__init__()
        self.model_name = 'food-model'


class ClarifaiApparelDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai apparel detection"""

    def __init__(self):
        super(ClarifaiApparelDetectionProcessor, self).__init__()
        self.model_name = 'apparel-model'


class ClarifaiWeddingDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai wedding detection"""

    def __init__(self):
        super(ClarifaiWeddingDetectionProcessor, self).__init__()
        self.model_name = 'wedding-model'


class ClarifaiExplicitDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai explicit detection"""

    def __init__(self):
        super(ClarifaiExplicitDetectionProcessor, self).__init__()
        self.model_name = 'nsfw-model'


class ClarifaiModerationDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai moderation detection"""

    def __init__(self):
        super(ClarifaiModerationDetectionProcessor, self).__init__()
        self.model_name = 'moderation-model'


class ClarifaiTexturesDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai textures detection"""

    def __init__(self):
        super(ClarifaiTexturesDetectionProcessor, self).__init__()
        self.model_name = 'textures-and-patterns-model'
