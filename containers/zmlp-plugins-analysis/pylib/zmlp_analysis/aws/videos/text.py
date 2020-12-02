import os

from zmlp_analysis.aws.util import AwsEnv
from zmlp_analysis.utils.prechecks import Prechecks
from zmlpsdk import AssetProcessor, Argument, FileTypes, ZmlpEnv, file_storage, proxy, clips, video
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlp_analysis.aws.text import RekognitionTextDetection


class RekognitionVideoTextDetection(AssetProcessor):
    """Get text for a video using AWS Rekognition """

    file_types = FileTypes.videos

    namespace = 'aws-text-detection'

    def __init__(self, extract_type=None, reactor=None):
        super(RekognitionVideoTextDetection, self).__init__()
        self.add_arg(Argument('debug', 'bool', default=False))
        self.extract_type = extract_type
        self.reactor = reactor
        self.image_client = None
        self.s3_client = None

    def init(self):
        self.image_client = RekognitionTextDetection()
        self.image_client.init()
        self.s3_client = AwsEnv.s3()

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
        ext = os.path.splitext(local_path)[1]
        bucket_file = f'{ZmlpEnv.get_project_id()}/video/{asset_id}{ext}'
        bucket_name = AwsEnv.get_bucket_name()

        # upload to s3
        self.s3_client.upload_file(local_path, bucket_name, bucket_file)

        if self.extract_type == 'time':
            extractor = video.TimeBasedFrameExtractor(local_path)
        else:
            extractor = video.ShotBasedFrameExtractor(local_path)

        clip_tracker = clips.ClipTracker(asset, self.namespace)

        analysis, clip_tracker = self.set_analysis(extractor, clip_tracker, self.image_client)
        asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        video.save_timeline(timeline)

    def set_analysis(self, extractor, clip_tracker, proc):
        """ Set up ClipTracker and Asset Detection Analysis

        Args:
            extractor: ShotBasedFrameExtractor
            clip_tracker: ClipTracker
            proc: Amazon Rekog image Client

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

    def emit_status(self, msg):
        """
        Emit a status back to the Archivist.

        Args:
            msg (str): The message to emit.

        """
        if not self.reactor:
            return
        self.reactor.emit_status(msg)
