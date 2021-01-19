import backoff
from clarifai.errors import ApiClientError

from zmlpsdk import AssetProcessor, FileTypes, file_storage, proxy, clips, video
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlp_analysis.clarifai.images import regions as regions_images
from zmlp_analysis.utils.prechecks import Prechecks
from zmlp_analysis.clarifai.util import not_a_quota_exception, model_map

__all__ = [
    'ClarifaiVideoCelebrityDetectionProcessor',
    'ClarifaiVideoDemographicsDetectionProcessor'
]


class AbstractClarifaiVideoProcessor(AssetProcessor):
    """
        This base class is used for all Clarifai features.  Subclasses
        only have to implement the "predict(asset, image) method.
        """

    file_types = FileTypes.videos

    namespace = 'clarifai'

    def __init__(self, model):
        super(AbstractClarifaiVideoProcessor, self).__init__()
        self.image_client = None
        self.model = model
        self.attribute = 'clarifai-{}'.format(model_map[model])

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
        model = getattr(self.image_client.clarifai.public_models, self.model)

        analysis, clip_tracker = self.set_analysis(extractor, clip_tracker, model)
        asset.add_analysis(self.attribute, analysis)
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
                concepts = response['outputs'][0]['data']['regions'][0]['data'].get('concepts')
            except KeyError:
                continue
            if not concepts:
                continue
            predictions = {c['name']: c['value'] for c in concepts}
            clip_tracker.append(time_ms, predictions)
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


class ClarifaiVideoCelebrityDetectionProcessor(AbstractClarifaiVideoProcessor):
    """ Clarifai celebrity detection"""

    def __init__(self):
        super(ClarifaiVideoCelebrityDetectionProcessor, self).__init__('celebrity_model')

    def init(self):
        self.image_client = regions_images.ClarifaiCelebrityDetectionProcessor()
        self.image_client.init()


class ClarifaiVideoDemographicsDetectionProcessor(AbstractClarifaiVideoProcessor):
    """ Clarifai landmark detection"""

    def __init__(self):
        super(ClarifaiVideoDemographicsDetectionProcessor, self).__init__('demographics_model')

    def init(self):
        self.image_client = regions_images.ClarifaiDemographicsDetectionProcessor()
        self.image_client.init()
