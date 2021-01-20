import backoff
from clarifai.errors import ApiError

from zmlpsdk import AssetProcessor, FileTypes
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path

from zmlp_analysis.clarifai.util import get_clarifai_app, \
    not_a_quota_exception, model_map, log_backoff_exception


__all__ = [
    'ClarifaiCelebrityDetectionProcessor',
    'ClarifaiDemographicsDetectionProcessor'
]


class AbstractClarifaiProcessor(AssetProcessor):
    """
        This base class is used for all Microsoft Computer Vision features.  Subclasses
        only have to implement the "predict(asset, image) method.
        """

    file_types = FileTypes.images | FileTypes.documents

    def __init__(self, model):
        super(AbstractClarifaiProcessor, self).__init__()
        self.model = model
        self.attribute = 'clarifai-{}'.format(model_map[model])
        self.clarifai = None

    def init(self):
        self.clarifai = get_clarifai_app()

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 1)

        model = getattr(self.clarifai.public_models, self.model)
        response = self.predict(model, p_path)
        try:
            labels = response['outputs'][0]['data']['regions'][0]['data'].get('concepts')
        except KeyError:
            return
        if not labels:
            return

        analysis = LabelDetectionAnalysis()
        [analysis.add_label_and_score(label['name'], label['value']) for label in labels]
        asset.add_analysis(self.attribute, analysis)

    @backoff.on_exception(backoff.expo,
                          ApiError,
                          max_time=3600,
                          giveup=not_a_quota_exception,
                          on_backoff=log_backoff_exception)
    def predict(self, model, p_path):
        """
        Make a prediction from the filename for a given model

        Args:
            model: (Clarifai.Model) CLarifai Model type
            p_path: (str) image path

        Returns:
            (dict) prediction response
        """
        return model.predict_by_filename(p_path)

    def emit_status(self, msg):
        """
        Emit a status back to the Archivist.

        Args:
            msg (str): The message to emit.

        """
        if not self.reactor:
            return
        self.reactor.emit_status(msg)


class ClarifaiCelebrityDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai label detection"""

    def __init__(self):
        super(ClarifaiCelebrityDetectionProcessor, self).__init__('celebrity_model')


class ClarifaiDemographicsDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai label detection"""

    def __init__(self):
        super(ClarifaiDemographicsDetectionProcessor, self).__init__('demographics_model')
