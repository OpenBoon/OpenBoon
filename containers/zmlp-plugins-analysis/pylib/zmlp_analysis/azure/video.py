import zmlp_analysis.azure.vision as vision
from zmlp_analysis.utils.prechecks import Prechecks
import zmlpsdk.video as video
from zmlpsdk import AssetProcessor, FileTypes, file_storage
from zmlpsdk import proxy
from zmlpsdk.analysis import LabelDetectionAnalysis, ContentDetectionAnalysis
from zmlpsdk.clips import ClipTracker, TimelineBuilder
from zmlpsdk.video import save_timeline

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


class AzureVideoAbstract(AssetProcessor):
    """
    This base class is used for all Microsoft Computer Vision video features.  Subclasses
    only have to implement the set_analysis method.
    """
    file_types = FileTypes.videos

    def __init__(self, extract_type=None):
        super(AzureVideoAbstract, self).__init__()
        self.vision_proc = None
        self.extract_type = extract_type

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

        if self.extract_type == 'time':
            extractor = video.TimeBasedFrameExtractor(local_path)
        else:
            extractor = video.ShotBasedFrameExtractor(local_path)

        clip_tracker = ClipTracker(asset, self.namespace)

        analysis, clip_tracker = self.set_analysis(extractor, clip_tracker, self.vision_proc)
        asset.add_analysis(self.namespace, analysis)

        # If we have text detection the tracks are combined
        # into a single Detected Text track.
        if isinstance(self, AzureVideoTextDetection):
            old_timeline = clip_tracker.build_timeline(final_time)
            timeline = TimelineBuilder(asset, self.namespace)

            for track_name, track in old_timeline.tracks.items():
                for clip in track['clips']:
                    timeline.add_clip('Detected Text',
                                      clip['start'], clip['stop'],
                                      clip['content'], clip['score'])
        else:
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
            pred_dict = {pred[0]: pred[1] for pred in predictions}
            clip_tracker.append(time_ms, pred_dict)
            for ls in predictions:
                analysis.add_label_and_score(ls[0], ls[1])

        return analysis, clip_tracker


class AzureVideoObjectDetection(AzureVideoAbstract):
    """Object detection for a video frame using Azure Computer Vision """
    namespace = 'azure-object-detection'

    def __init__(self):
        super(AzureVideoObjectDetection, self).__init__()

    def init(self):
        self.vision_proc = vision.AzureVisionObjectDetection()
        self.vision_proc.init()


class AzureVideoLabelDetection(AzureVideoAbstract):
    """Label detection for a video frame using Azure Computer Vision """
    namespace = 'azure-label-detection'

    def __init__(self):
        super(AzureVideoLabelDetection, self).__init__()

    def init(self):
        self.vision_proc = vision.AzureVisionLabelDetection()
        self.vision_proc.init()


class AzureVideoImageDescriptionDetection(AzureVideoAbstract):
    """Get image descriptions for a video frame using Azure Computer Vision """
    namespace = 'azure-image-description-detection'

    def __init__(self):
        super(AzureVideoImageDescriptionDetection, self).__init__()

    def init(self):
        self.vision_proc = vision.AzureVisionImageDescriptionDetection()
        self.vision_proc.init()


class AzureVideoImageTagDetection(AzureVideoAbstract):
    """Get image tags for a video frame using Azure Computer Vision """
    namespace = 'azure-tag-detection'

    def __init__(self):
        super(AzureVideoImageTagDetection, self).__init__()

    def init(self):
        self.vision_proc = vision.AzureVisionImageTagsDetection()
        self.vision_proc.init()


class AzureVideoCelebrityDetection(AzureVideoAbstract):
    """Celebrity detection for a video frame using Azure Computer Vision """
    namespace = 'azure-celebrity-detection'

    def __init__(self):
        super(AzureVideoCelebrityDetection, self).__init__()

    def init(self):
        self.vision_proc = vision.AzureVisionCelebrityDetection()
        self.vision_proc.init()


class AzureVideoLandmarkDetection(AzureVideoAbstract):
    """Landmark detection for a video frame using Azure Computer Vision """
    namespace = 'azure-landmark-detection'

    def __init__(self):
        super(AzureVideoLandmarkDetection, self).__init__()

    def init(self):
        self.vision_proc = vision.AzureVisionLandmarkDetection()
        self.vision_proc.init()


class AzureVideoLogoDetection(AzureVideoAbstract):
    """Logo detection for a video frame using Azure Computer Vision """
    namespace = 'azure-logo-detection'

    def __init__(self):
        super(AzureVideoLogoDetection, self).__init__()

    def init(self):
        self.vision_proc = vision.AzureVisionLogoDetection()
        self.vision_proc.init()


class AzureVideoCategoryDetection(AzureVideoAbstract):
    """Category detection for a video frame using Azure Computer Vision """
    namespace = 'azure-category-detection'

    def __init__(self):
        super(AzureVideoCategoryDetection, self).__init__()

    def init(self):
        self.vision_proc = vision.AzureVisionCategoryDetection()
        self.vision_proc.init()


class AzureVideoExplicitContentDetection(AzureVideoAbstract):
    """Explicit Content detection for a video frame using Azure Computer Vision """
    namespace = 'azure-explicit-detection'

    def __init__(self):
        super(AzureVideoExplicitContentDetection, self).__init__(extract_type='time')

    def init(self):
        self.vision_proc = vision.AzureVisionExplicitContentDetection()
        self.vision_proc.init()


class AzureVideoFaceDetection(AzureVideoAbstract):
    """Face detection for a video frame using Azure Computer Vision """
    namespace = 'azure-face-detection'

    def __init__(self):
        super(AzureVideoFaceDetection, self).__init__()

    def init(self):
        self.vision_proc = vision.AzureVisionFaceDetection()
        self.vision_proc.init()


class AzureVideoTextDetection(AzureVideoAbstract):
    """Get OCR'd text for a video frame using Azure Computer Vision """
    namespace = 'azure-text-detection'

    def __init__(self):
        super(AzureVideoTextDetection, self).__init__()

    def init(self):
        self.vision_proc = vision.AzureVisionTextDetection()
        self.vision_proc.init()

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
            pred = {pred: 1 for pred in predictions}
            clip_tracker.append(time_ms, pred)
            analysis.add_content(predictions)

        return analysis, clip_tracker
