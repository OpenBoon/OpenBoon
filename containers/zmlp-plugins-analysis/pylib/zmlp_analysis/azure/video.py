from zmlp_analysis.azure.vision import AzureVisionLabelDetection
from zmlpsdk import FileTypes, file_storage
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk import proxy
from zmlpsdk.audio import has_audio_channel
from zmlp_analysis.google.cloud_timeline import save_timeline

from zmlpsdk.clips import ClipTracker
import zmlpsdk.video as video


class AzureVideoDetector(AzureVisionLabelDetection):
    """
    This class is used for extracting labels from image frames taken from a video.
    """
    file_types = FileTypes.videos
    namespace = 'azure-video-label-detection'

    def __init__(self):
        super(AzureVideoDetector, self).__init__()

    def process(self, frame):
        """

        Args:
            frame:

        Returns:

        """
        asset = frame.asset
        asset_id = asset.id
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        final_time = asset.get_attr('media.length')

        if final_time > 120:
            self.logger.warning(
                'Skipping, video is longer than {} seconds.'.format(self.max_length_sec))
            return

        audio_proxy = proxy.get_video_proxy(asset)

        if not audio_proxy:
            self.logger.warning(f'No audio could be found for {asset_id}')
            return

        local_path = file_storage.localize_file(audio_proxy)
        if not has_audio_channel(local_path):
            self.logger.warning(f'No audio channel could be found for {asset_id}')
            return

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
