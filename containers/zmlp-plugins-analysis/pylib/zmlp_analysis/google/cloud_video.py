import logging
import time

import backoff
import google.cloud.videointelligence_v1p3beta1 as videointelligence
from google.api_core.exceptions import ResourceExhausted

from zmlpsdk import Argument, AssetProcessor, FileTypes, file_storage, proxy
from zmlpsdk.analysis import LabelDetectionAnalysis, ContentDetectionAnalysis, Prediction
from . import cloud_timeline
from .gcp_client import initialize_gcp_client

logger = logging.getLogger(__name__)

tip = 'Minimum confidence score between 0 and 1 required to add a ' \
      'prediction.  Defaults to disabled'


class AsyncVideoIntelligenceProcessor(AssetProcessor):
    """Use Google Cloud Video Intelligence to detect explicit content."""

    file_types = FileTypes.videos

    tool_tips = {
        'detect_labels': tip,
        'detect_text': "Set to true to enable text detection.",
        'detect_objects': tip,
        'detect_logos': tip,
        'detect_explicit': 'An integer level of confidence to tag as explicit. 0=disabled, max=5'
    }

    max_length_sec = 30 * 60
    """By default we allow up to 30 minutes of video."""

    conf_labels = [
        "IGNORE",
        "very_unlikely",
        "unlikely",
        "possible",
        "likely",
        "very_likely"]
    """A list of confidence labels used by Google."""

    def __init__(self):
        super(AsyncVideoIntelligenceProcessor, self).__init__()
        self.add_arg(Argument('detect_explicit', 'int', default=-1,
                              toolTip=self.tool_tips['detect_explicit']))
        self.add_arg(Argument('detect_labels', 'float', default=-1,
                              toolTip=self.tool_tips['detect_labels']))
        self.add_arg(Argument('detect_text', 'bool', default=False,
                              toolTip=self.tool_tips['detect_text']))
        self.add_arg(Argument('detect_objects', 'float', default=-1,
                              toolTip=self.tool_tips['detect_objects']))
        self.add_arg(Argument('detect_logos', 'float', default=-1,
                              toolTip=self.tool_tips['detect_logos']))
        self.video_intel_client = None

    def init(self):
        super(AsyncVideoIntelligenceProcessor, self).init()
        self.video_intel_client = initialize_gcp_client(
            videointelligence.VideoIntelligenceServiceClient)

    def process(self, frame):
        asset = frame.asset

        # Cannot run on clips without transcoding the clip
        if asset.get_attr('clip.track') != 'full':
            self.logger.info('Skipping, cannot run processor on clips.')
            return -1

        # If the length is over time time
        if asset.get_attr('media.length') > self.max_length_sec:
            self.logger.info(
                'Skipping, video is longer than {} seconds.'.format(self.max_length_sec))
            return

        # You can't run this on the source because our google creds
        # don't allow us access to other people's buckets. Using the
        # customer creds would use their VidInt quota, which would
        # be badd, mmmkay.
        proxy_uri = self.get_video_proxy_uri(asset)
        annotation_result = self._get_video_annotations(proxy_uri)
        file_storage.assets.store_blob(annotation_result.SerializeToString(),
                                       asset,
                                       'gcp',
                                       'video-intelligence.dat')

        if self.arg_value('detect_logos') != -1:
            self.handle_detect_logos(asset, annotation_result)

        if self.arg_value('detect_objects') != -1:
            self.handle_detect_objects(asset, annotation_result)

        if self.arg_value('detect_labels') != -1:
            self.handle_detect_labels(asset, annotation_result)

        if self.arg_value('detect_text'):
            self.handle_detect_text(asset, annotation_result)

        if self.arg_value('detect_explicit') != -1:
            self.handle_detect_explicit(asset, annotation_result)

    def get_video_proxy_uri(self, asset):
        video_proxy = proxy.get_proxy_level(asset, 3, mimetype="video")
        return file_storage.assets.get_native_uri(video_proxy)

    def handle_detect_logos(self, asset, results):
        """
        Detect logos in video and adds analysis to the given aszet.

        Args:
            asset (Asset): The asset.
            results (obj): The video intelligence result.
        """
        analysis = LabelDetectionAnalysis(min_score=self.arg_value('detect_logos'),
                                          collapse_labels=True)
        for annotation in results.logo_recognition_annotations:
            for track in annotation.tracks:
                analysis.add_prediction(Prediction(
                    annotation.entity.description,
                    track.confidence))
        asset.add_analysis('gcp-video-logo-detection', analysis)
        timeline = cloud_timeline.build_logo_detection_timeline(results)
        file_storage.assets.store_timeline(asset, timeline)

    def handle_detect_objects(self, asset, annotation_result):
        analysis = LabelDetectionAnalysis(min_score=self.arg_value('detect_objects'),
                                          collapse_labels=True)
        for annotation in annotation_result.object_annotations:
            pred = Prediction(annotation.entity.description,
                              annotation.confidence)
            analysis.add_prediction(pred)

        asset.add_analysis('gcp-video-object-detection', analysis)

    def handle_detect_labels(self, asset, results):
        """
        Handles processing segment and shot labels.

        Args:
            asset (Asset): The asset to process.
            results (dict): The JSON compatible result.

        """
        def process_label_annotations(annotations):
            for annotation in annotations:
                labels = [annotation.entity.description]

                for category in annotation.category_entities:
                    labels.append(category.description)

                for segment in annotation.segments:
                    for label in labels:
                        analysis.add_prediction(Prediction(label, segment.confidence))

        analysis = LabelDetectionAnalysis(min_score=self.arg_value('detect_labels'),
                                          collapse_labels=True)

        process_label_annotations(results.segment_label_annotations)
        process_label_annotations(results.shot_label_annotations)
        process_label_annotations(results.shot_presence_label_annotations)
        asset.add_analysis('gcp-video-label-detection', analysis)

        timeline = cloud_timeline.build_label_detection_timeline(results)
        file_storage.assets.store_timeline(asset, timeline)

    def handle_detect_text(self, asset, annotation_result):
        analysis = ContentDetectionAnalysis()
        analysis.add_content(
            ' '.join(t.text for t in annotation_result.text_annotations))

        if analysis.content:
            asset.add_analysis('gcp-video-text-detection', analysis)

            timeline = cloud_timeline.build_text_detection_timeline(annotation_result)
            file_storage.assets.store_timeline(asset, timeline)

    def handle_detect_explicit(self, asset, annotation_result):
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        analysis.set_attr('explicit', False)

        for frame in annotation_result.explicit_annotation.frames:
            if frame.pornography_likelihood == 0:
                continue

            pred = Prediction(self.conf_labels[frame.pornography_likelihood], 1)
            analysis.add_prediction(pred)
            if frame.pornography_likelihood >= self.arg_value("detect_explicit"):
                analysis.set_attr('explicit', True)

        asset.add_analysis('gcp-video-explicit-detection', analysis)

        timeline = cloud_timeline.build_content_moderation_timeline(annotation_result)
        file_storage.assets.store_timeline(asset, timeline)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_tries=3, max_time=3600)
    def _get_video_annotations(self, uri):
        """Uses the Google Video Intelligence API to get video annotations.

        Args:
            uri (str): The gs:// uri where the video resides.

        Returns:
            VideoAnnotationResults: Results from Google API.

        """
        features = []
        if self.arg_value('detect_explicit') > -1:
            features.append(videointelligence.enums.Feature.EXPLICIT_CONTENT_DETECTION)
        if self.arg_value('detect_labels') > -1:
            features.append(videointelligence.enums.Feature.LABEL_DETECTION)
        if self.arg_value('detect_text'):
            features.append(videointelligence.enums.Feature.TEXT_DETECTION)
        if self.arg_value('detect_objects') > -1:
            features.append(videointelligence.enums.Feature.OBJECT_TRACKING)
        if self.arg_value('detect_logos') > -1:
            features.append(videointelligence.enums.Feature.LOGO_RECOGNITION)

        operation = self.video_intel_client.annotate_video(input_uri=uri, features=features)

        while not operation.done():
            logger.info("Waiting on Google Visual Intelligence {}".format(uri))
            time.sleep(0.5)

        res = operation.result()
        return res.annotation_results[0]
