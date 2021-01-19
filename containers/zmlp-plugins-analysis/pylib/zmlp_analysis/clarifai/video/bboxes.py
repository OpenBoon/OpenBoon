import backoff
from clarifai.errors import ApiClientError

from zmlp_analysis.clarifai.images import bboxes as bboxes_images
from zmlp_analysis.clarifai.util import not_a_quota_exception, model_map
from zmlp_analysis.utils.prechecks import Prechecks
from zmlpsdk import AssetProcessor, FileTypes, file_storage, proxy, clips, video
from zmlpsdk.analysis import LabelDetectionAnalysis

__all__ = [
    'ClarifaiVideoFaceDetectionProcessor',
    'ClarifaiVideoLogoDetectionProcessor'
]


class AbstractClarifaiVideoProcessor(AssetProcessor):
    """
    This base class is used for all Microsoft Computer Vision features.  Subclasses
    only have to implement the "predict(asset, image) method.
    """

    file_types = FileTypes.videos

    def __init__(self, model):
        super(AbstractClarifaiVideoProcessor, self).__init__()
        self.image_client = None
        self.model = model
        self.attribiute = 'clarifai-{}'.format(model_map[model])

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
        clip_tracker = clips.ClipTracker(asset, self.attribiute)
        model = getattr(self.image_client.clarifai.public_models, self.model)

        analysis, clip_tracker = self.set_analysis(extractor, clip_tracker, model)
        asset.add_analysis(self.attribiute, analysis)
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
            try:
                concepts = response['outputs'][0]['data'].get('regions')
            except KeyError:
                continue
            if not concepts:
                continue
            for concept in concepts:
                c = concept['data'].get('concepts')[0]
                pred = {c['name']: c['value']}
                clip_tracker.append(time_ms, pred)
                analysis.add_label_and_score(c['name'], c['value'])

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


class ClarifaiVideoFaceDetectionProcessor(AbstractClarifaiVideoProcessor):
    """ Clarifai face detection"""

    def __init__(self):
        super(ClarifaiVideoFaceDetectionProcessor, self).__init__('face_detection_model')

    def init(self):
        self.image_client = bboxes_images.ClarifaiFaceDetectionProcessor()
        self.image_client.init()


class ClarifaiVideoLogoDetectionProcessor(AbstractClarifaiVideoProcessor):
    """ Clarifai logo detection"""

    def __init__(self):
        super(ClarifaiVideoLogoDetectionProcessor, self).__init__('logo_model')

    def init(self):
        self.image_client = bboxes_images.ClarifaiLogoDetectionProcessor()
        self.image_client.init()
