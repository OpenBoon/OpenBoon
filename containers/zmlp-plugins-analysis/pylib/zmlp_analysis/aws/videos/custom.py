
import os

from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlp_analysis.utils.prechecks import Prechecks
from zmlp_analysis.aws.util import AwsEnv
from zmlpsdk import AssetProcessor, Argument, FileTypes, ZmlpEnv, file_storage, proxy, clips, video


class CustomLabelVideoDetectProcessor(AssetProcessor):
    """ AWS Rekognition for Video Detection for Custom Labels """
    namespace = 'aws-video-detection'
    file_types = FileTypes.videos

    def __init__(self, reactor=None, extract_type=None):
        super(CustomLabelVideoDetectProcessor, self).__init__()
        self.add_arg(Argument('debug', 'bool', default=False))
        self.reactor = reactor
        self.extract_type = extract_type

        self.rek_client = None
        self.s3_client = None
        self.project_version_arn = None

    def init(self):
        self.rek_client = AwsEnv.rekognition()
        self.s3_client = AwsEnv.s3()
        self.project_version_arn = ''

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

        clip_tracker = clips.ClipTracker(asset, self.namespace)

        ext = os.path.splitext(local_path)[1]
        bucket_file = f'{ZmlpEnv.get_project_id()}/video/{asset_id}{ext}'
        bucket_name = AwsEnv.get_bucket_name()

        # upload to s3
        self.s3_client.upload_file(local_path, bucket_name, bucket_file)

        # analyze video
        analysis, clip_tracker = self.analyze_video(extractor, clip_tracker)
        asset.add_analysis(self.namespace, analysis)

        timeline = clip_tracker.build_timeline(final_time)
        video.save_timeline(timeline)

    def analyze_video(self, extractor, clip_tracker):
        """ Analyze video using custom label model

        Args:
            extractor: ShotBased or TimeBased frame extractor depending on extract_type
            clip_tracker: ClipTracker for building timeline

        Returns:
            (tuple): asset detection analysis, clip_tracker
        """
        analysis = LabelDetectionAnalysis(collapse_labels=True)

        for time_ms, path in extractor:
            predictions = self.rek_client.detect_custom_labels(
                    Image={
                        'Bytes': path,
                    },
                    ProjectVersionArn=self.project_version_arn
                )
            labels = [pred['Name'] for pred in predictions['CustomLabels']]
            clip_tracker.append(time_ms, labels)
            for ls in predictions['CustomLabels']:
                start_time = ls['Timestamp'] / 1000  # ms to s
                analysis.add_label_and_score(ls['Name'], ls['Confidence'], timestamp=start_time)

        return analysis, clip_tracker
