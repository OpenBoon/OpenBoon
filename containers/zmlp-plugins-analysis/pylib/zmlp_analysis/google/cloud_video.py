import logging
import time

import backoff
import google.cloud.videointelligence_v1p3beta1 as videointelligence
from google.api_core.exceptions import ResourceExhausted

from zmlp_analysis.utils.prechecks import Prechecks
from zmlpsdk import Argument, AssetProcessor, FileTypes, file_storage, proxy
from zmlpsdk.analysis import LabelDetectionAnalysis, ContentDetectionAnalysis, Prediction
from zmlpsdk.base import ProcessorException
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
        'detect_explicit': 'An integer level of confidence to tag as explicit. 0=disabled, max=5',
        'detect_speech': 'Set to true to recognize speech in video.'
    }

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
        self.add_arg(Argument('detect_explicit', 'int', default=False,
                              toolTip=self.tool_tips['detect_explicit']))
        self.add_arg(Argument('detect_labels', 'float', default=False,
                              toolTip=self.tool_tips['detect_labels']))
        self.add_arg(Argument('detect_text', 'bool', default=False,
                              toolTip=self.tool_tips['detect_text']))
        self.add_arg(Argument('detect_objects', 'float', default=False,
                              toolTip=self.tool_tips['detect_objects']))
        self.add_arg(Argument('detect_logos', 'float', default=False,
                              toolTip=self.tool_tips['detect_logos']))
        self.add_arg(Argument('detect_speech', 'bool', default=False,
                              toolTip=self.tool_tips['detect_speech']))
        self.video_intel_client = None

    def init(self):
        super(AsyncVideoIntelligenceProcessor, self).init()
        self.video_intel_client = initialize_gcp_client(
            videointelligence.VideoIntelligenceServiceClient)

    def process(self, frame):
        asset = frame.asset

        # If the length is over time time
        if not Prechecks.is_valid_video_length(asset):
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

        if self.arg_value('detect_logos'):
            self.handle_detect_logos(asset, annotation_result)

        if self.arg_value('detect_objects'):
            self.handle_detect_objects(asset, annotation_result)

        if self.arg_value('detect_labels'):
            self.handle_detect_labels(asset, annotation_result)

        if self.arg_value('detect_text'):
            self.handle_detect_text(asset, annotation_result)

        if self.arg_value('detect_speech'):
            self.handle_detect_speech(asset, annotation_result)

        if self.arg_value('detect_explicit'):
            self.handle_detect_explicit(asset, annotation_result)

    def get_video_proxy_uri(self, asset):
        video_proxy = proxy.get_proxy_level(asset, 3, mimetype="video")
        if not video_proxy:
            raise ProcessorException("Unable to find video proxy for asset {}".format(asset.id))
        return file_storage.assets.get_native_uri(video_proxy)

    def handle_detect_logos(self, asset, results):
        """
        Detect logos in video and adds analysis to the given aszet.

        Args:
            asset (Asset): The asset.
            results (obj): The Logo detection result.
        """
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        for annotation in results.logo_recognition_annotations:
            for track in annotation.tracks:
                analysis.add_prediction(Prediction(
                    annotation.entity.description,
                    track.confidence))
        asset.add_analysis('gcp-video-logo-detection', analysis)
        cloud_timeline.save_logo_detection_timeline(asset, results)

    def handle_detect_objects(self, asset, annotation_result):
        """
        Detect objects in video.

        Args:
            asset (Asset): The asset to process.
            annotation_result (obj): The Object detection result.

        """
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        for annotation in annotation_result.object_annotations:
            pred = Prediction(annotation.entity.description,
                              annotation.confidence)
            analysis.add_prediction(pred)

        asset.add_analysis('gcp-video-object-detection', analysis)
        cloud_timeline.save_object_detection_timeline(asset, annotation_result)

    def handle_detect_labels(self, asset, annotation_result):
        """
        Handles processing segment and shot labels.

        Args:
            asset (Asset): The asset to process.
            annotation_result (dict): The label detection result.

        """
        def process_label_annotations(annotations):
            for annotation in annotations:
                labels = [annotation.entity.description]

                for category in annotation.category_entities:
                    labels.append(category.description)

                for segment in annotation.segments:
                    for label in labels:
                        analysis.add_prediction(Prediction(label, segment.confidence))

        analysis = LabelDetectionAnalysis(collapse_labels=True)

        process_label_annotations(annotation_result.segment_label_annotations)
        process_label_annotations(annotation_result.shot_label_annotations)
        process_label_annotations(annotation_result.shot_presence_label_annotations)
        asset.add_analysis('gcp-video-label-detection', analysis)

        cloud_timeline.save_label_detection_timeline(asset, annotation_result)

    def handle_detect_text(self, asset, annotation_result):
        """
        Detect text images in a video.
        Args:
            asset (Asset): The asset.
            annotation_result (obj): The detect text result.

        """
        analysis = ContentDetectionAnalysis()
        analysis.add_content(
            ' '.join(t.text for t in annotation_result.text_annotations))

        if analysis.content:
            asset.add_analysis('gcp-video-text-detection', analysis)
            cloud_timeline.save_text_detection_timeline(asset, annotation_result)

    def handle_detect_speech(self, asset, annotation_result):
        """
        Handle the detect speech result.

        Args:
            asset (Asset): The Asset
            annotation_result (obj): The detect speech result.

        """
        analysis = ContentDetectionAnalysis()
        for speech in annotation_result.speech_transcriptions:
            for alternative in speech.alternatives:
                # Find first one with words.
                if alternative.words:
                    analysis.add_content(alternative.transcript.strip())
                    break

        asset.add_analysis('gcp-video-speech-transcription', analysis)
        cloud_timeline.save_speech_transcription_timeline(asset, annotation_result)
        cloud_timeline.save_video_speech_transcription_webvtt(asset, annotation_result)

    def handle_detect_explicit(self, asset, annotation_result):
        """
        Detect explicit content.

        Args:
            asset (Asset): The asset.
            annotation_result (obj): The content moderation result.
        """
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        analysis.set_attr('explicit', False)

        for frame in annotation_result.explicit_annotation.frames:
            if frame.pornography_likelihood == 0:
                continue

            pred = Prediction(self.conf_labels[frame.pornography_likelihood], 1)
            analysis.add_prediction(pred)
            if frame.pornography_likelihood >= 4:
                analysis.set_attr('explicit', True)

        asset.add_analysis('gcp-video-explicit-detection', analysis)
        cloud_timeline.save_content_moderation_timeline(asset, annotation_result)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_tries=3, max_time=3600)
    def _get_video_annotations(self, uri):
        """Uses the Google Video Intelligence API to get video annotations.

        Args:
            uri (str): The gs:// uri where the video resides.

        Returns:
            VideoAnnotationResults: Results from Google API.

        """
        features = []
        video_context = {}
        if self.arg_value('detect_explicit'):
            features.append(videointelligence.enums.Feature.EXPLICIT_CONTENT_DETECTION)
        if self.arg_value('detect_labels'):
            features.append(videointelligence.enums.Feature.LABEL_DETECTION)
        if self.arg_value('detect_text'):
            features.append(videointelligence.enums.Feature.TEXT_DETECTION)
        if self.arg_value('detect_speech'):
            features.append(videointelligence.enums.Feature.SPEECH_TRANSCRIPTION)
            config = videointelligence.types.SpeechTranscriptionConfig(
                    language_code="en-US", enable_automatic_punctuation=True)
            video_context = videointelligence.types.VideoContext(
                    speech_transcription_config=config)
        if self.arg_value('detect_objects'):
            features.append(videointelligence.enums.Feature.OBJECT_TRACKING)
        if self.arg_value('detect_logos'):
            features.append(videointelligence.enums.Feature.LOGO_RECOGNITION)

        logger.info("Calling Google Video Intelligence,  ctx={}".format(video_context))
        operation = self.video_intel_client.annotate_video(input_uri=uri, features=features,
                                                           video_context=video_context)

        while not operation.done():
            logger.info("Waiting on Google Visual Intelligence {}".format(uri))
            time.sleep(5)

        res = operation.result()
        return res.annotation_results[0]
