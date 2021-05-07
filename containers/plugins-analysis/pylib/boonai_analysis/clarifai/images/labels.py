import backoff
from clarifai.errors import ApiError
from clarifai_grpc.channel.clarifai_channel import ClarifaiChannel
from clarifai_grpc.grpc.api import service_pb2_grpc, service_pb2, resources_pb2
from clarifai_grpc.grpc.api.status import status_code_pb2

from boonflow import AssetProcessor, FileTypes
from boonflow.analysis import LabelDetectionAnalysis
from boonflow.proxy import get_proxy_level_path

from boonai_analysis.clarifai.util import get_clarifai_app, \
    not_a_quota_exception, model_map, model_id_map, log_backoff_exception

__all__ = [
    'ClarifaiLabelDetectionProcessor',
    'ClarifaiFoodDetectionProcessor',
    'ClarifaiTravelDetectionProcessor',
    'ClarifaiApparelDetectionProcessor',
    'ClarifaiWeddingDetectionProcessor',
    'ClarifaiExplicitDetectionProcessor',
    'ClarifaiModerationDetectionProcessor',
    'ClarifaiTexturesDetectionProcessor',
]


class AbstractClarifaiProcessor(AssetProcessor):
    """
        This base class is used for all Clarifai features.  Subclasses
        only have to implement the "predict(asset, image) method.
        """

    file_types = FileTypes.images | FileTypes.documents

    def __init__(self, model):
        super(AbstractClarifaiProcessor, self).__init__()
        self.clarifai = None
        self.auth = None
        self.model = model
        self.model_id = model_id_map[model]
        self.attribute = 'clarifai-{}'.format(model_map[model])

    def init(self):
        self.clarifai, self.auth = get_clarifai_app()

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 1)

        model = getattr(self.clarifai.public_models, self.model)
        response = self.predict(model, p_path)
        labels = response.outputs[0].data.concepts
        if not labels:
            return

        analysis = LabelDetectionAnalysis()
        [analysis.add_label_and_score(label.name, label.value) for label in labels]
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
            model: (Clarifai.Model) Clarifai Model type
            p_path: (str) image path

        Returns:
            (dict) prediction response
        """
        with open(p_path, "rb") as f:
            file_bytes = f.read()
            response = self.clarifai.PostModelOutputs(
                service_pb2.PostModelOutputsRequest(
                    model_id=self.model_id,
                    inputs=[
                        resources_pb2.Input(
                            data=resources_pb2.Data(
                                image=resources_pb2.Image(
                                    base64=file_bytes
                                )
                            )
                        )
                    ]
                ),
                metadata=self.auth
            )
        if response.status.code != status_code_pb2.SUCCESS:
            raise Exception("Post model outputs failed, status: " + response.status.description)

        return response

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
        super(ClarifaiLabelDetectionProcessor, self).__init__('general_model')


class ClarifaiFoodDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai food detection"""

    def __init__(self):
        super(ClarifaiFoodDetectionProcessor, self).__init__('food_model')


class ClarifaiTravelDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai travel detection"""

    def __init__(self):
        super(ClarifaiTravelDetectionProcessor, self).__init__('travel_model')


class ClarifaiApparelDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai apparel detection"""

    def __init__(self):
        super(ClarifaiApparelDetectionProcessor, self).__init__('apparel_model')


class ClarifaiWeddingDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai wedding detection"""

    def __init__(self):
        super(ClarifaiWeddingDetectionProcessor, self).__init__('wedding_model')


class ClarifaiExplicitDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai explicit detection"""

    def __init__(self):
        super(ClarifaiExplicitDetectionProcessor, self).__init__('nsfw_model')


class ClarifaiModerationDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai moderation detection"""

    def __init__(self):
        super(ClarifaiModerationDetectionProcessor, self).__init__('moderation_model')


class ClarifaiTexturesDetectionProcessor(AbstractClarifaiProcessor):
    """ Clarifai textures detection"""

    def __init__(self):
        super(ClarifaiTexturesDetectionProcessor, self).__init__('textures_and_patterns_model')
