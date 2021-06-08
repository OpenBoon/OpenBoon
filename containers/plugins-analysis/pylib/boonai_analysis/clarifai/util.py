import logging
import os
from types import SimpleNamespace

import backoff
from boonai_analysis.utils.prechecks import Prechecks
from clarifai_grpc.channel.clarifai_channel import ClarifaiChannel
from clarifai_grpc.grpc.api import resources_pb2, service_pb2, service_pb2_grpc
from clarifai_grpc.grpc.api.status import status_code_pb2

from boonflow import AssetProcessor, FileTypes, file_storage, proxy, clips, video

logger = logging.getLogger('clarifai')


def not_a_quota_exception(exp):
    """
    Returns true if the exception is not a Clarifai quota exception.  This ensures the backoff
    function doesn't sleep on the wrong exceptions.

    Args:
        exp (Exception): The exception

    Returns:
        bool: True if not a quota exception.
    """
    return getattr(exp, 'status_code', 999) != 429


def log_backoff_exception(details):
    """
    Log an exception from the backoff library.

    Args:
        details (dict): The details of the backoff call.

    """
    logger.warning(
        'Waiting on quota {wait:0.1f} seconds afters {tries} tries'.format(**details))


class AbstractClarifaiProcessor(AssetProcessor):
    """This base class is used for all Clarifai features.  Subclasses
    only have to implement the "predict(asset, image) method.

    Cvars:
        file_types (FileType): File types the processor can work on.
        attribute_name (str): Attribute name to use for the analysis field on the asset.
         I will be prefixed with "clarifai-".
        model_id (str): UUID of the Clarifai model that will be used.

    """
    file_types = None
    attribute_name = None
    model_id = None

    def __init__(self):
        super(AbstractClarifaiProcessor, self).__init__()
        self.attribute = 'clarifai-{}'.format(self.attribute_name)
        self.stub = service_pb2_grpc.V2Stub(ClarifaiChannel.get_grpc_channel())
        self.auth_meta = (('authorization', 'Key {}'.format(os.environ.get("CLARIFAI_KEY"))),)

    @backoff.on_exception(backoff.expo,
                          IOError,
                          max_time=3600,
                          giveup=not_a_quota_exception,
                          on_backoff=log_backoff_exception)
    def predict(self, p_path):
        """
        Make a prediction from the filename for a given model

        Args:
            model_id: (str) UUID of a public Clarifai model.
            p_path: (str) image path

        Returns:
            (dict) prediction response
        """
        with open(p_path, 'rb') as f:
            file_bytes = f.read()
        image = resources_pb2.Image(base64=file_bytes)
        data = resources_pb2.Data(image=image)
        input = resources_pb2.Input(data=data)
        request = service_pb2.PostModelOutputsRequest(model_id=self.model_id, inputs=[input])
        outputs_response = self.stub.PostModelOutputs(request, metadata=self.auth_meta)
        if outputs_response.status.code != status_code_pb2.SUCCESS:
            exception = IOError('Failed to get model predictions from Clarafai. '
                                'Status: {}'.format(outputs_response.status.code))
            exception.status_code = outputs_response.status.code
        return outputs_response

    def emit_status(self, msg):
        """
        Emit a status back to the Archivist.

        Args:
            msg (str): The message to emit.

        """
        if not self.reactor:
            return
        self.reactor.emit_status(msg)


class AbstractClarifaiVideoProcessor(AbstractClarifaiProcessor):
    """
    This base class is used for all Clarifai features.  Subclasses
    only have to implement the "predict(asset, image) method.
    """
    file_types = FileTypes.videos
    image_client_class = None

    def init(self):
        self.image_client = self.image_client_class()
        self.image_client.init()
        self.model_id = self.image_client_class.model_id

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        final_time = asset.get_attr('media.length')

        if not Prechecks.is_valid_video_length(asset):
            return

        video_proxy = proxy.get_video_proxy(asset)
        if not video_proxy:
            self.logger.warning(f'No video could be found for {asset_id}')
            return

        local_path = file_storage.localize_file(video_proxy)

        extractor = video.ShotBasedFrameExtractor(local_path)
        clip_tracker = clips.ClipTracker(asset, self.attribute)
        analysis, clip_tracker = self.set_analysis(extractor, clip_tracker)
        asset.add_analysis(self.attribute, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        video.save_timeline(asset, timeline)

    def set_analysis(self, extractor, clip_tracker):
        """ Set up ClipTracker and Asset Detection Analysis. Must be overridden in the
        concrete class and return an analysis and clip_tracker

        Args:
            extractor: ShotBasedFrameExtractor
            clip_tracker: ClipTracker

        Returns:
            (tuple): asset detection analysis, clip_tracker
        """
        raise NotImplementedError


class MockClarifaiPredictionResponse(object):
    """Mocks the response object returned by the Clarifai API. Used for unit testing
    Clarifai processors.

    """
    def __init__(self):
        output = SimpleNamespace()
        output.data = SimpleNamespace()
        output.data.regions = []
        output.data.concepts = []
        self.outputs = [output]


class RecursiveNamespace(SimpleNamespace):
    """Recursive simplenamespace to help with mocking the clarifai api."""
    @staticmethod
    def map_entry(entry):
        if isinstance(entry, dict):
            return RecursiveNamespace(**entry)
        return entry

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        for key, val in kwargs.items():
            if type(val) == dict:
                setattr(self, key, RecursiveNamespace(**val))
            elif type(val) == list:
                setattr(self, key, list(map(self.map_entry, val)))
