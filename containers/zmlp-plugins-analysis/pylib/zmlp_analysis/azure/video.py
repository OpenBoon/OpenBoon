import zmlp_analysis.azure.vision as vision
import zmlpsdk.video as video
from zmlp_analysis.google.cloud_timeline import save_timeline
from zmlpsdk import AssetProcessor, FileTypes, file_storage
from zmlpsdk import proxy
from zmlpsdk.analysis import LabelDetectionAnalysis, ContentDetectionAnalysis
from zmlpsdk.clips import ClipTracker

__all__ = [
    'AzureVideoObjectDetection',
    'AzureVideoLabelDetection',
    'AzureVideoImageDescriptionDetection',
    'AzureVideoImageTagDetection',
    'AzureVideoCelebrityDetection',
    'AzureVideoLandmarkDetection',
    'AzureVideoLogoDetection',
    'AzureVideoCategoryDetection',
    'AzureVideoExplicitContentDetection',
    'AzureVideoFaceDetection',
    'AzureVideoTextDetection'
]

MAX_LENGTH_SEC = 120


class AzureVideoAbstract(AssetProcessor):
    """
    This base class is used for all Microsoft Computer Vision video features.  Subclasses
    only have to implement the set_analysis method.
    """
    file_types = FileTypes.videos

    def __init__(self, extract_type=None):
        super(AzureVideoAbstract, self).__init__()
        self.vision_client = None
        self.extract_type = extract_type

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        final_time = asset.get_attr('media.length')

        if final_time > MAX_LENGTH_SEC:
            self.logger.warning(
                'Skipping, video is longer than {} seconds.'.format(MAX_LENGTH_SEC))
            return

        video_proxy = proxy.get_video_proxy(asset)

        if not video_proxy:
            self.logger.warning(f'No video could be found for {asset_id}')
            return

        local_path = file_storage.localize_file(video_proxy)

        if self.extract_type == 'time':
            extractor = video.TimeBasedFrameExtractor(local_path)
        else:
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


class AzureVideoObjectDetection(AzureVideoAbstract):
    """Object detection for a video frame using Azure Computer Vision """
    namespace = 'azure-object-detection'

    def __init__(self):
        super(AzureVideoObjectDetection, self).__init__()

    def init(self):
        self.vision_client = vision.AzureVisionObjectDetection()
        self.vision_client.init()


class AzureVideoLabelDetection(AzureVideoAbstract):
    """Label detection for a video frame using Azure Computer Vision """
    namespace = 'azure-label-detection'

    def __init__(self):
        super(AzureVideoLabelDetection, self).__init__()

    def init(self):
        self.vision_client = vision.AzureVisionLabelDetection()
        self.vision_client.init()


class AzureVideoImageDescriptionDetection(AzureVideoAbstract):
    """Get image descriptions for a video frame using Azure Computer Vision """
    namespace = 'azure-image-description-detection'

    def __init__(self):
        super(AzureVideoImageDescriptionDetection, self).__init__()

    def init(self):
        self.vision_client = vision.AzureVisionImageDescriptionDetection()
        self.vision_client.init()


class AzureVideoImageTagDetection(AzureVideoAbstract):
    """Get image tags for a video frame using Azure Computer Vision """
    namespace = 'azure-tag-detection'

    def __init__(self):
        super(AzureVideoImageTagDetection, self).__init__()

    def init(self):
        self.vision_client = vision.AzureVisionImageTagsDetection()
        self.vision_client.init()


class AzureVideoCelebrityDetection(AzureVideoAbstract):
    """Celebrity detection for a video frame using Azure Computer Vision """
    namespace = 'azure-celebrity-detection'

    def __init__(self):
        super(AzureVideoCelebrityDetection, self).__init__()

    def init(self):
        self.vision_client = vision.AzureVisionCelebrityDetection()
        self.vision_client.init()


class AzureVideoLandmarkDetection(AzureVideoAbstract):
    """Landmark detection for a video frame using Azure Computer Vision """
    namespace = 'azure-landmark-detection'

    def __init__(self):
        super(AzureVideoLandmarkDetection, self).__init__()

    def init(self):
        self.vision_client = vision.AzureVisionLandmarkDetection()
        self.vision_client.init()


class AzureVideoLogoDetection(AzureVideoAbstract):
    """Logo detection for a video frame using Azure Computer Vision """
    namespace = 'azure-logo-detection'

    def __init__(self):
        super(AzureVideoLogoDetection, self).__init__()

    def init(self):
        self.vision_client = vision.AzureVisionLogoDetection()
        self.vision_client.init()


class AzureVideoCategoryDetection(AzureVideoAbstract):
    """Category detection for a video frame using Azure Computer Vision """
    namespace = 'azure-category-detection'

    def __init__(self):
        super(AzureVideoCategoryDetection, self).__init__()

    def init(self):
        self.vision_client = vision.AzureVisionCategoryDetection()
        self.vision_client.init()


class AzureVideoExplicitContentDetection(AzureVideoAbstract):
    """Explicit Content detection for a video frame using Azure Computer Vision """
    namespace = 'azure-explicit-detection'

    def __init__(self):
        super(AzureVideoExplicitContentDetection, self).__init__(extract_type='time')

    def init(self):
        self.vision_client = vision.AzureVisionExplicitContentDetection()
        self.vision_client.init()


class AzureVideoFaceDetection(AzureVideoAbstract):
    """Face detection for a video frame using Azure Computer Vision """
    namespace = 'azure-face-detection'

    def __init__(self):
        super(AzureVideoFaceDetection, self).__init__()

    def init(self):
        self.vision_client = vision.AzureVisionFaceDetection()
        self.vision_client.init()


class AzureVideoTextDetection(AzureVideoAbstract):
    """Get OCR'd text for a video frame using Azure Computer Vision """
    namespace = 'azure-text-detection'

    def __init__(self):
        super(AzureVideoTextDetection, self).__init__()

    def init(self):
        self.vision_client = vision.AzureVisionTextDetection()
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
