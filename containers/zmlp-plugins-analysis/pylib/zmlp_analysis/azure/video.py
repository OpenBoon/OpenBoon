# flake8: noqa
from zmlp_analysis.azure.vision import *
from zmlpsdk import AssetProcessor, FileTypes, file_storage
from zmlpsdk.analysis import LabelDetectionAnalysis, ContentDetectionAnalysis
from zmlpsdk import proxy
from zmlp_analysis.google.cloud_timeline import save_timeline

from zmlpsdk.clips import ClipTracker
import zmlpsdk.video as video

__all__ = [
    'AzureVideoObjectDetector',
    'AzureVideoLabelDetector',
    'AzureVideoImageDescriptionDetector',
    'AzureVideoImageTagDetector',
    'AzureVideoCelebrityDetector',
    'AzureVideoLandmarkDetector',
    'AzureVideoLogoDetector',
    'AzureVideoCategoryDetector',
    'AzureVideoExplicitContentDetector',
    'AzureVideoFaceDetector',
    'AzureVideoTextDetector'
]

MAX_LENGTH_SEC = 120


class AzureVideoAbstract(AssetProcessor):
    """
    This base class is used for all Microsoft Computer Vision video features.  Subclasses
    only have to implement the set_analysis method.
    """
    file_types = FileTypes.videos

    def __init__(self):
        super(AzureVideoAbstract, self).__init__()
        self.vision_client = None

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
        extractor = video.ShotBasedFrameExtractor(local_path)
        clip_tracker = ClipTracker(asset, self.namespace)

        analysis, clip_tracker = self.set_analysis(extractor, clip_tracker, self.vision_client)
        asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        save_timeline(timeline)

    def set_analysis(self, extractor, clip_tracker, proc):
        """ Set up ClipTracker and Asset Detection Analysis

        Args:
            extractor: ShotBasedFrameExtractor
            clip_tracker: ClipTracker
            proc: Azure Computer Vision Client

        Returns:
            (tuple): asset detection analysis, clip_tracker
        """
        analysis = LabelDetectionAnalysis(collapse_labels=True)

        for time_ms, path in extractor:
            predictions = proc.predict(path)
            labels = [pred[0] for pred in predictions]
            clip_tracker.append(time_ms, labels)
            for ls in predictions:
                analysis.add_label_and_score(ls[0], ls[1])

        return analysis, clip_tracker


class AzureVideoObjectDetector(AzureVideoAbstract):
    """Object detection for a video frame using Azure Computer Vision """
    namespace = 'azure-video-object-detection'

    def __init__(self):
        super(AzureVideoObjectDetector, self).__init__()

    def init(self):
        self.vision_client = AzureVisionObjectDetection()
        self.vision_client.init()


class AzureVideoLabelDetector(AzureVideoAbstract):
    """Label detection for a video frame using Azure Computer Vision """
    namespace = 'azure-video-label-detection'

    def __init__(self):
        super(AzureVideoLabelDetector, self).__init__()

    def init(self):
        self.vision_client = AzureVisionLabelDetection()
        self.vision_client.init()


class AzureVideoImageDescriptionDetector(AzureVideoAbstract):
    """Get image descriptions for a video frame using Azure Computer Vision """
    namespace = 'azure-video-image-description-detection'

    def __init__(self):
        super(AzureVideoImageDescriptionDetector, self).__init__()

    def init(self):
        self.vision_client = AzureVisionImageDescription()
        self.vision_client.init()


class AzureVideoImageTagDetector(AzureVideoAbstract):
    """Get image tags for a video frame using Azure Computer Vision """
    namespace = 'azure-video-tag-detection'

    def __init__(self):
        super(AzureVideoImageTagDetector, self).__init__()

    def init(self):
        self.vision_client = AzureVisionImageTagsDetection()
        self.vision_client.init()


class AzureVideoCelebrityDetector(AzureVideoAbstract):
    """Celebrity detection for a video frame using Azure Computer Vision """
    namespace = 'azure-video-celebrity-detection'

    def __init__(self):
        super(AzureVideoCelebrityDetector, self).__init__()

    def init(self):
        self.vision_client = AzureVisionCelebrityDetection()
        self.vision_client.init()


class AzureVideoLandmarkDetector(AzureVideoAbstract):
    """Landmark detection for a video frame using Azure Computer Vision """
    namespace = 'azure-video-landmark-detection'

    def __init__(self):
        super(AzureVideoLandmarkDetector, self).__init__()

    def init(self):
        self.vision_client = AzureVisionLandmarkDetection()
        self.vision_client.init()


class AzureVideoLogoDetector(AzureVideoAbstract):
    """Logo detection for a video frame using Azure Computer Vision """
    namespace = 'azure-video-logo-detection'

    def __init__(self):
        super(AzureVideoLogoDetector, self).__init__()

    def init(self):
        self.vision_client = AzureVisionLogoDetection()
        self.vision_client.init()


class AzureVideoCategoryDetector(AzureVideoAbstract):
    """Category detection for a video frame using Azure Computer Vision """
    namespace = 'azure-video-category-detection'

    def __init__(self):
        super(AzureVideoCategoryDetector, self).__init__()

    def init(self):
        self.vision_client = AzureVisionCategoryDetection()
        self.vision_client.init()


class AzureVideoExplicitContentDetector(AzureVideoAbstract):
    """Explicit Content detection for a video frame using Azure Computer Vision """
    namespace = 'azure-video-explicit-detection'

    def __init__(self):
        super(AzureVideoExplicitContentDetector, self).__init__()

    def init(self):
        self.vision_client = AzureVisionExplicitContentDetection()
        self.vision_client.init()


class AzureVideoFaceDetector(AzureVideoAbstract):
    """Face detection for a video frame using Azure Computer Vision """
    namespace = 'azure-video-face-detection'

    def __init__(self):
        super(AzureVideoFaceDetector, self).__init__()

    def init(self):
        self.vision_client = AzureVisionFaceDetection()
        self.vision_client.init()


class AzureVideoTextDetector(AzureVideoAbstract):
    """Get OCR'd text for a video frame using Azure Computer Vision """
    namespace = 'azure-video-text-detection'

    def __init__(self):
        super(AzureVideoTextDetector, self).__init__()

    def init(self):
        self.vision_client = AzureVisionTextDetection()
        self.vision_client.init()

    def set_analysis(self, extractor, clip_tracker, proc):
        """ Set up ClipTracker and Asset Detection Analysis

        Args:
            extractor: ShotBasedFrameExtractor
            clip_tracker: ClipTracker
            proc: Azure Computer Vision Client

        Returns:
            (tuple): asset detection analysis, clip_tracker
        """
        analysis = ContentDetectionAnalysis(unique_words=True)

        for time_ms, path in extractor:
            predictions = proc.predict(path)
            labels = [predictions]
            clip_tracker.append(time_ms, labels)
            analysis.add_content(predictions)

        return analysis, clip_tracker
