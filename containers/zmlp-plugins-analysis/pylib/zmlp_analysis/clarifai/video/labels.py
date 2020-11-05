import os
import backoff
from clarifai.errors import ApiClientError

from zmlpsdk import AssetProcessor, Argument, FileTypes, file_storage, proxy, clips, video
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlp_analysis.clarifai.images import labels as labels_images
from zmlp_analysis.clarifai.util import not_a_quota_exception

__all__ = [
    'ClarifaiVideoLabelDetectionProcessor',
    'ClarifaiVideoFoodDetectionProcessor',
    'ClarifaiVideoTravelDetectionProcessor',
    'ClarifaiVideoApparelDetectionProcessor',
    'ClarifaiVideoWeddingDetectionProcessor',
    'ClarifaiVideoExplicitDetectionProcessor',
    'ClarifaiVideoModerationDetectionProcessor',
    'ClarifaiVideoTexturesDetectionProcessor',
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

MAX_LENGTH_SEC = 120
MAX_SIZE = 10**7  # 10MB


class AbstractClarifaiVideoProcessor(AssetProcessor):
    """
        This base class is used for all Clarifai features.  Subclasses
        only have to implement the "predict(asset, image) method.
        """

    file_types = FileTypes.images | FileTypes.documents

    namespace = 'clarifai'

    def __init__(self, model_name, reactor=None):
        super(AbstractClarifaiVideoProcessor, self).__init__()
        self.add_arg(Argument('debug', 'bool', default=False))
        self.reactor = reactor
        self.image_client = None
        self.model_name = model_name

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        final_time = asset.get_attr('media.length')

        if final_time > MAX_LENGTH_SEC:
            self.logger.warning(
                'Skipping, video is longer than {} seconds.'.format(self.max_length_sec))
            return

        video_proxy = proxy.get_video_proxy(asset)
        if not video_proxy:
            self.logger.warning(f'No video could be found for {asset_id}')
            return

        local_path = file_storage.localize_file(video_proxy)
        if os.path.getsize(local_path) >= MAX_SIZE:
            self.logger.warning(f'Video found in {asset_id} exceeds 10MB')
            return

        extractor = video.ShotBasedFrameExtractor(local_path)
        clip_tracker = clips.ClipTracker(asset, self.namespace)
        model = getattr(self.image_client.clarifai.public_models, self.model_name.replace("-", "_"))

        analysis, clip_tracker = self.set_analysis(extractor, clip_tracker, model)
        asset.add_analysis("-".join([self.namespace, self.model_name]), analysis)
        timeline = clip_tracker.build_timeline(final_time)
        video.save_timeline(timeline)

    def set_analysis(self, extractor, clip_tracker, model):
        """ Set up ClipTracker and Asset Detection Analysis

        Args:
            extractor: ShotBasedFrameExtractor
            clip_tracker: ClipTracker
            model: Clarifai.PublicModel

        Returns:
            (tuple): asset detection analysis, clip_tracker
        """
        analysis = LabelDetectionAnalysis(collapse_labels=True)

        for time_ms, path in extractor:
            response = self.predict(model, path)
            concepts = response['outputs'][0]['data'].get('concepts')
            if not concepts:
                continue
            labels = [c['name'] for c in concepts]
            clip_tracker.append(time_ms, labels)
            [analysis.add_label_and_score(c['name'], c['value']) for c in concepts]

        return analysis, clip_tracker

    @backoff.on_exception(backoff.expo,
                          ApiClientError,
                          max_time=3600,
                          giveup=not_a_quota_exception)
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


class ClarifaiVideoLabelDetectionProcessor(AbstractClarifaiVideoProcessor):
    """ Clarifai label detection"""

    def __init__(self):
        super(ClarifaiVideoLabelDetectionProcessor, self).__init__('general-model')

    def init(self):
        self.image_client = labels_images.ClarifaiLabelDetectionProcessor()
        self.image_client.init()


class ClarifaiVideoFoodDetectionProcessor(AbstractClarifaiVideoProcessor):
    """ Clarifai food detection"""

    def __init__(self):
        super(ClarifaiVideoFoodDetectionProcessor, self).__init__('food-model')

    def init(self):
        self.image_client = labels_images.ClarifaiFoodDetectionProcessor()
        self.image_client.init()


class ClarifaiVideoTravelDetectionProcessor(AbstractClarifaiVideoProcessor):
    """ Clarifai travel detection"""

    def __init__(self):
        super(ClarifaiVideoTravelDetectionProcessor, self).__init__('travel-model')

    def init(self):
        self.image_client = labels_images.ClarifaiTravelDetectionProcessor()
        self.image_client.init()


class ClarifaiVideoApparelDetectionProcessor(AbstractClarifaiVideoProcessor):
    """ Clarifai apparel detection"""

    def __init__(self):
        super(ClarifaiVideoApparelDetectionProcessor, self).__init__('apparel-model')

    def init(self):
        self.image_client = labels_images.ClarifaiApparelDetectionProcessor()
        self.image_client.init()


class ClarifaiVideoWeddingDetectionProcessor(AbstractClarifaiVideoProcessor):
    """ Clarifai wedding detection"""

    def __init__(self):
        super(ClarifaiVideoWeddingDetectionProcessor, self).__init__('wedding-model')

    def init(self):
        self.image_client = labels_images.ClarifaiWeddingDetectionProcessor()
        self.image_client.init()


class ClarifaiVideoExplicitDetectionProcessor(AbstractClarifaiVideoProcessor):
    """ Clarifai explicit detection"""

    def __init__(self):
        super(ClarifaiVideoExplicitDetectionProcessor, self).__init__('nsfw-model')

    def init(self):
        self.image_client = labels_images.ClarifaiExplicitDetectionProcessor()
        self.image_client.init()


class ClarifaiVideoModerationDetectionProcessor(AbstractClarifaiVideoProcessor):
    """ Clarifai moderation detection"""

    def __init__(self):
        super(ClarifaiVideoModerationDetectionProcessor, self).__init__('moderation-model')

    def init(self):
        self.image_client = labels_images.ClarifaiModerationDetectionProcessor()
        self.image_client.init()


class ClarifaiVideoTexturesDetectionProcessor(AbstractClarifaiVideoProcessor):
    """ Clarifai textures detection"""

    def __init__(self):
        super(ClarifaiVideoTexturesDetectionProcessor, self).__init__('textures-and-patterns-model')

    def init(self):
        self.image_client = labels_images.ClarifaiTexturesDetectionProcessor()
        self.image_client.init()
