from clarifai_grpc.grpc.api import resources_pb2, service_pb2

from zmlpsdk import AssetProcessor, Argument, FileTypes
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path
from zmlp_analysis.clarifai.util import get_clarifai_grpc_app


class ClarifaiTextModerationProcessor(AssetProcessor):
    """ Determine text moderation using Clarifai Text Moderation pre-trained model. """

    namespace = 'clarifai'

    def __init__(self, model_name, reactor=None):
        super(ClarifaiTextModerationProcessor, self).__init__()
        self.add_arg(Argument('debug', 'bool', default=False))
        self.reactor = reactor
        self.clarifai = None
        self.model_name = model_name

    def init(self):
        self.clarifai = get_clarifai_grpc_app()

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