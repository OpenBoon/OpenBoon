# flake8: noqa
from zmlp_analysis.azure.vision import *
from zmlpsdk import FileTypes, file_storage
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


class AzureVideoObjectDetector(AzureVisionObjectDetection):
    """
    This class is used for extracting labels from image frames taken from a video.
    """
    file_types = FileTypes.videos
    namespace = 'azure-video-object-detection'

    def __init__(self):
        super(AzureVideoObjectDetector, self).__init__()

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        final_time = asset.get_attr('media.length')

        if final_time > 120:
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

        frames = list(extractor)
        for time_ms, path in frames:
            predictions = self.predict(path)
            labels = [pred[0] for pred in predictions]
            clip_tracker.append(time_ms, labels)
            for ls in predictions:
                analysis.add_label_and_score(ls[0], ls[1], bbox=ls[2])

        asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        save_timeline(timeline)


class AzureVideoLabelDetector(AzureVisionLabelDetection):
    """
    This class is used for extracting labels from image frames taken from a video.
    """
    file_types = FileTypes.videos
    namespace = 'azure-video-label-detection'

    def __init__(self):
        super(AzureVideoLabelDetector, self).__init__()

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        final_time = asset.get_attr('media.length')

        if final_time > 120:
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

        frames = list(extractor)
        for time_ms, path in frames:
            predictions = self.predict(path)
            labels = [pred[0] for pred in predictions]
            clip_tracker.append(time_ms, labels)
            for ls in predictions:
                analysis.add_label_and_score(ls[0], ls[1])

        asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        save_timeline(timeline)


class AzureVideoImageDescriptionDetector(AzureVisionImageDescription):
    """
    This class is used for extracting labels from image frames taken from a video.
    """
    file_types = FileTypes.videos
    namespace = 'azure-video-image-description-detection'

    def __init__(self):
        super(AzureVideoImageDescriptionDetector, self).__init__()

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        final_time = asset.get_attr('media.length')

        if final_time > 120:
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

        frames = list(extractor)
        for time_ms, path in frames:
            predictions = self.predict(path)
            labels = [pred[0] for pred in predictions]
            clip_tracker.append(time_ms, labels)
            for ls in predictions:
                analysis.add_label_and_score(ls[0], ls[1])

        asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        save_timeline(timeline)


class AzureVideoImageTagDetector(AzureVisionImageTagsDetection):
    """
    This class is used for extracting labels from image frames taken from a video.
    """
    file_types = FileTypes.videos
    namespace = 'azure-video-tag-detection'

    def __init__(self):
        super(AzureVideoImageTagDetector, self).__init__()

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        final_time = asset.get_attr('media.length')

        if final_time > 120:
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

        frames = list(extractor)
        for time_ms, path in frames:
            predictions = self.predict(path)
            labels = [pred[0] for pred in predictions]
            clip_tracker.append(time_ms, labels)
            for ls in predictions:
                analysis.add_label_and_score(ls[0], ls[1])

        asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        save_timeline(timeline)


class AzureVideoCelebrityDetector(AzureVisionCelebrityDetection):
    """
    This class is used for extracting labels from image frames taken from a video.
    """
    file_types = FileTypes.videos
    namespace = 'azure-video-celebrity-detection'

    def __init__(self):
        super(AzureVideoCelebrityDetector, self).__init__()

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        final_time = asset.get_attr('media.length')

        if final_time > 120:
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

        frames = list(extractor)
        for time_ms, path in frames:
            predictions = self.predict(path)
            labels = [pred[0] for pred in predictions]
            clip_tracker.append(time_ms, labels)
            for ls in predictions:
                analysis.add_label_and_score(ls[0], ls[1], bbox=ls[2])

        asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        save_timeline(timeline)


class AzureVideoLandmarkDetector(AzureVisionLandmarkDetection):
    """
    This class is used for extracting labels from image frames taken from a video.
    """
    file_types = FileTypes.videos
    namespace = 'azure-video-landmark-detection'

    def __init__(self):
        super(AzureVideoLandmarkDetector, self).__init__()

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        final_time = asset.get_attr('media.length')

        if final_time > 120:
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

        frames = list(extractor)
        for time_ms, path in frames:
            predictions = self.predict(path)
            labels = [pred[0] for pred in predictions]
            clip_tracker.append(time_ms, labels)
            for ls in predictions:
                analysis.add_label_and_score(ls[0], ls[1])

        asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        save_timeline(timeline)


class AzureVideoLogoDetector(AzureVisionLogoDetection):
    """
    This class is used for extracting labels from image frames taken from a video.
    """
    file_types = FileTypes.videos
    namespace = 'azure-video-logo-detection'

    def __init__(self):
        super(AzureVideoLogoDetector, self).__init__()

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        final_time = asset.get_attr('media.length')

        if final_time > 120:
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

        frames = list(extractor)
        for time_ms, path in frames:
            predictions = self.predict(path)
            labels = [pred[0] for pred in predictions]
            clip_tracker.append(time_ms, labels)
            for ls in predictions:
                analysis.add_label_and_score(ls[0], ls[1], bbox=ls[2])

        asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        save_timeline(timeline)


class AzureVideoCategoryDetector(AzureVisionCategoryDetection):
    """
    This class is used for extracting labels from image frames taken from a video.
    """
    file_types = FileTypes.videos
    namespace = 'azure-video-category-detection'

    def __init__(self):
        super(AzureVideoCategoryDetector, self).__init__()

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        final_time = asset.get_attr('media.length')

        if final_time > 120:
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

        frames = list(extractor)
        for time_ms, path in frames:
            predictions = self.predict(path)
            labels = [pred[0] for pred in predictions]
            clip_tracker.append(time_ms, labels)
            for ls in predictions:
                analysis.add_label_and_score(ls[0], ls[1])

        asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        save_timeline(timeline)


class AzureVideoExplicitContentDetector(AzureVisionExplicitContentDetection):
    """
    This class is used for extracting labels from image frames taken from a video.
    """
    file_types = FileTypes.videos
    namespace = 'azure-video-explicit-detection'

    def __init__(self):
        super(AzureVideoExplicitContentDetector, self).__init__()

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        final_time = asset.get_attr('media.length')

        if final_time > 120:
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

        frames = list(extractor)
        for time_ms, path in frames:
            predictions = self.predict(path)
            labels = [pred[0] for pred in predictions]
            clip_tracker.append(time_ms, labels)
            for ls in predictions:
                analysis.add_label_and_score(ls[0], ls[1])

        asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        save_timeline(timeline)


class AzureVideoFaceDetector(AzureVisionFaceDetection):
    """
    This class is used for extracting labels from image frames taken from a video.
    """
    file_types = FileTypes.videos
    namespace = 'azure-video-face-detection'

    def __init__(self):
        super(AzureVideoFaceDetector, self).__init__()

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        final_time = asset.get_attr('media.length')

        if final_time > 120:
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

        frames = list(extractor)
        for time_ms, path in frames:
            predictions = self.predict(path)
            labels = [pred[0] for pred in predictions]
            clip_tracker.append(time_ms, labels)
            for ls in predictions:
                analysis.add_label_and_score(ls[0], ls[1], bbox=ls[2], age=ls[3])

        asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        save_timeline(timeline)


class AzureVideoTextDetector(AzureVisionTextDetection):
    """
    This class is used for extracting labels from image frames taken from a video.
    """
    file_types = FileTypes.videos
    namespace = 'azure-video-text-detection'

    def __init__(self):
        super(AzureVideoTextDetector, self).__init__()

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        analysis = ContentDetectionAnalysis(unique_words=True)
        final_time = asset.get_attr('media.length')

        if final_time > 120:
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

        frames = list(extractor)
        for time_ms, path in frames:
            predictions = self.predict(path)
            labels = [predictions]
            clip_tracker.append(time_ms, labels)
            analysis.add_content(predictions)

        asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        save_timeline(timeline)
