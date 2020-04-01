import subprocess
import tempfile

import backoff
from google.api_core.exceptions import ResourceExhausted
from google.cloud import videointelligence_v1p2beta1 as videointelligence
from pathlib import Path

from zmlpsdk import Argument, AssetProcessor
from zmlpsdk.proxy import get_proxy_level_path

from .gcp_client import initialize_gcp_client


class CloudVideoIntelligenceProcessor(AssetProcessor):
    """Use Google Cloud Video Intelligence API to label videos."""

    file_types = ['mov', 'mp4', 'mpg', 'mpeg', 'm4v', 'webm', 'ogv', 'ogg']

    tool_tips = {
        'detect_labels': 'If True then label detection will be run.',
        'detect_text': 'If True OCR will be run.'
    }

    def __init__(self):
        super(CloudVideoIntelligenceProcessor, self).__init__()
        self.add_arg(Argument('detect_labels', 'bool', default=True,
                              toolTip=self.tool_tips['detect_labels']))
        self.add_arg(Argument('detect_text', 'bool', default=True,
                              toolTip=self.tool_tips['detect_text']))
        self.video_intel_client = None

    def init(self):
        super(CloudVideoIntelligenceProcessor, self).init()
        self.video_intel_client = initialize_gcp_client(
            videointelligence.VideoIntelligenceServiceClient)

    def process(self, frame):
        asset = frame.asset
        if not asset.attr_exists("clip"):
            self.logger.info('Skipping this frame, it is not a video clip.')
            return
        clip_contents = self._get_clip_bytes(asset)
        annotation_result = self._get_video_annotations(clip_contents)
        if self.arg_value('detect_labels'):
            self._add_video_intel_labels(asset, 'google.videoLabel.segment.keywords',
                                         annotation_result.segment_label_annotations)
            self._add_video_intel_labels(asset, 'google.videoLabel.shot.keywords',
                                         annotation_result.shot_label_annotations)
            self._add_video_intel_labels(asset, 'google.videoLabel.frame.keywords',
                                         annotation_result.frame_label_annotations)
        if self.arg_value('detect_text'):
            text = ' '.join(t.text for t in annotation_result.text_annotations)
            if text:
                asset.add_analysis('google.videoText.content', text)

    def _get_clip_bytes(self, asset):
        """Gets the in/out points for the clip specified in the metadata and transcodes
        that section into a new video. The bytes of that new video file are returned.

        Args:
            asset (Asset): Asset that has clip metadata.

        Returns:
            str: Byte contents of the video clip that was created.

        """
        clip_start = float(asset.get_attr('clip.start'))
        clip_length = float(asset.get_attr('clip.length'))
        video_length = asset.get_attr('media.duration')
        seek = max(clip_start - 0.25, 0)
        duration = min(clip_length + 0.5, video_length)
        clip_path = Path(tempfile.mkdtemp(),
                         next(tempfile._get_candidate_names()) + '.mp4')

        # Construct ffmpeg command line
        # check for proxy
        command = ['ffmpeg',
                   '-i', get_proxy_level_path(asset, 3, mimetype="video/"),
                   '-ss', str(seek),
                   '-t', str(duration),
                   '-s', '512x288',
                   str(clip_path)]
        self.logger.info('Executing: %s' % command)
        subprocess.check_call(command)

        # Read the extracted video file
        with clip_path.open('rb') as _file:
            return _file.read()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=5 * 60 * 60)
    def _get_video_annotations(self, video_contents):
        """Uses the Google Video Intelligence API to get video annotations.

        Args:
            video_contents (str): Contents of a video file to send to the Google API.

        Returns:
            VideoAnnotationResults: Results from Google API.

        """
        features = []
        if self.arg_value('detect_labels'):
            features.append(videointelligence.enums.Feature.LABEL_DETECTION)
        if self.arg_value('detect_text'):
            features.append(videointelligence.enums.Feature.TEXT_DETECTION)
        operation = self.video_intel_client.annotate_video(input_content=video_contents,
                                                           features=features, )
        return operation.result(timeout=500).annotation_results[0]

    def _add_video_intel_labels(self, asset, analysis_field, annotations):
        """Extracts labels from a Google VideoAnnotationResults object and adds them to
        the metadata of an Asset.

        Args:
            asset (Asset): Asset to add metadata to.
            analysis_field (str): Metadata field in the "analysis" namespace to add
             values to.
            annotations (VideoAnnotationResults): Results from a call to Cloud Video
             Intelligence API to get labels from.

        """
        keywords = []
        for annotation in annotations:
            keywords.append(annotation.entity.description)
            for category_entity in annotation.category_entities:
                keywords.append(category_entity.description)
        if keywords:
            asset.add_analysis(analysis_field, keywords)
