#!/usr/bin/env python

import os

from unittest.mock import patch
from ..processors import CloudVideoIntelligenceProcessor

from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_data, TestAsset


# Mocks Google's VideoAnnotationResults object
class MockVideoAnnotationResults:
    def __init__(self, input_uri=None, segment_label_annotations=[], shot_label_annotations=[],
                 frame_label_annotations=[], text_annotations=[], error=None):
        self.input_uri = input_uri
        self.segment_label_annotations = segment_label_annotations
        self.shot_label_annotations = shot_label_annotations
        self.frame_label_annotations = frame_label_annotations
        self.text_annotations = text_annotations
        self.error = error


# Mocks Google's LabelAnnotations object
class MockLabelAnnotation:
    def __init__(self, category_entities=[], entity=None):
        self.category_entities = category_entities
        self.entity = entity


# Mocks text object
class MockTextAnnotation:
    def __init__(self, text=None, segments=[]):
        self.text = text
        self.segments = segments


# Mocks Google's Entity object
class MockEntity:
    def __init__(self, entity_id=None, description=None, language_code=None):
        self.entity_id = entity_id
        self.description = description
        self.language_code = language_code


class CloudVideoIntelligenceUnitTest(PluginUnitTestCase):

    # Mock the processor client to bypass google authentication
    @patch('zmlp_analysis.google.processors.videointelligence.'
           'VideoIntelligenceServiceClient', autospec=True)
    def setUp(self, mock_videointelligence_client):
        self.mock_videointelligence_client = mock_videointelligence_client
        self.mock_annotation_results = MockVideoAnnotationResults()

        self.working_dir = os.path.dirname(__file__)
        self.zorroa_test_data_dir = zorroa_test_data()
        self.full_mock_data_dir = os.path.join(self.working_dir, self.zorroa_test_data_dir)

        self.processor = CloudVideoIntelligenceProcessor()
        self.init_processor(self.processor, None)

    # Patch get_clip_bytes since it relies on an external call to ffmpeg, which may not be
    # installed.
    # Patch get_video_annotations since it relies on google.cloud and instead return mock results.
    @patch(
        'zmlp_analysis.google.processors.CloudVideoIntelligenceProcessor.'
        '_get_clip_bytes')
    @patch(
        'zmlp_analysis.google.processors.CloudVideoIntelligenceProcessor.'
        '_get_video_annotations')
    def test_process_not_clip(self, get_video_annotations, get_clip_bytes):
        # Note: In order to prove that the 'process' method is returning early and skipping all of
        # the processing, we're setting this test up the same way as a valid video test to show
        # that the annotation label data is never stored within the asset's attributes dictionary.
        # Even the reference to a doc file below doesn't actually do anything because the processor
        # looks for 'clip' within the asset's attributes to determine if it's a clip, which
        # we aren't explicitly populating here.

        file_path = "/word/lighthouse.docx"

        self.mock_annotation_results.input_uri = file_path

        # Populate the mock with data to process.
        self.mock_annotation_results.segment_label_annotations = [
            MockLabelAnnotation(
                entity=MockEntity(
                    entity_id="/m/07s6nbt",
                    description="text",
                    language_code="en-US"
                )
            )
        ]

        # Set the mock returns
        get_clip_bytes.return_value = "bytes"
        get_video_annotations.return_value = self.mock_annotation_results

        # Instruct the processor to process annotation labels only
        self.processor.args['detect_labels'].value = True
        self.processor.args['detect_text'].value = False

        frame = Frame(TestAsset(os.path.join(self.full_mock_data_dir, file_path)))

        self.processor.process(frame)

        # If the process doesn't run, then the work isn't performed and data isn't stored within
        # asset attributes.
        self.assertEqual(frame.asset.get_attr('analysis'), None)

    # Patch get_clip_bytes since it relies on an external call to ffmpeg, which may not be
    # installed. Patch get_video_annotations since it relies on google.cloud and instead return mock
    # results.
    @patch(
        'zmlp_analysis.google.processors.CloudVideoIntelligenceProcessor.'
        '_get_clip_bytes')
    @patch(
        'zmlp_analysis.google.processors.CloudVideoIntelligenceProcessor.'
        '_get_video_annotations')
    def test_process_no_results(self, get_video_annotations, get_clip_bytes):
        # Note: Here's we run a normal test but we don't pass in any label results

        file_path = "/srt/srt_sample.mp4"

        # Set the mock returns, pass empty MockVideoAnnotationResults object
        get_clip_bytes.return_value = "bytes"
        get_video_annotations.return_value = self.mock_annotation_results

        # Instruct the processor to process both label annotation and text annotation
        self.processor.args['detect_labels'].value = True
        self.processor.args['detect_text'].value = True

        frame = Frame(TestAsset(os.path.join(self.full_mock_data_dir, file_path)))
        frame.asset.set_attr('clip.start', 0.0)
        frame.asset.set_attr('clip.length', 111.0)

        self.processor.process(frame)

        # Since there are no label results returned, data isn't stored within asset attributes under
        # analysis.
        self.assertEqual(frame.asset.get_attr('analysis'), None)

    # Patch get_clip_bytes since it relies on an external call to ffmpeg, which may not be
    # installed. Patch get_video_annotations since it relies on google.cloud and instead return mock
    # results.
    @patch(
        'zmlp_analysis.google.processors.CloudVideoIntelligenceProcessor.'
        '_get_clip_bytes')
    @patch(
        'zmlp_analysis.google.processors.CloudVideoIntelligenceProcessor.'
        '_get_video_annotations')
    def test_process_segment_labels_only(self, get_video_annotations, get_clip_bytes):
        file_path = "/srt/srt_sample.mp4"

        self.mock_annotation_results.input_uri = file_path

        # Populate the mock with data to process - 2 annotations, one without category_entities
        self.mock_annotation_results.segment_label_annotations = [
            MockLabelAnnotation(
                category_entities=[
                    MockEntity(
                        entity_id="/m/01g317",
                        description="person",
                        language_code="en-US"
                    )
                ],
                entity=MockEntity(
                    entity_id="/m/016pp7",
                    description="happiness",
                    language_code="en-US"
                )
            ),
            MockLabelAnnotation(
                entity=MockEntity(
                    entity_id="/m/07s6nbt",
                    description="text",
                    language_code="en-US"
                )
            )
        ]

        # Set the mock returns
        get_clip_bytes.return_value = "bytes"
        get_video_annotations.return_value = self.mock_annotation_results

        # Instruct the processor to process annotation labels only
        self.processor.args['detect_labels'].value = True
        self.processor.args['detect_text'].value = False

        # Establish frame object and pass in your asset.
        frame = Frame(TestAsset(os.path.join(self.full_mock_data_dir, file_path)))
        frame.asset.set_attr('clip.start', 0.0)
        frame.asset.set_attr('clip.length', 111.0)

        self.processor.process(frame)

        # Confirm that all entity descriptions made it to the attributes dictionary as intended.
        self.assertIn('person', frame.asset.get_attr('analysis.google.videoLabel.segment.keywords'))
        self.assertIn('happiness',
                      frame.asset.get_attr('analysis.google.videoLabel.segment.keywords'))
        self.assertIn('text', frame.asset.get_attr('analysis.google.videoLabel.segment.keywords'))

        # Confirm that no dictionaries were added to attributes for shot, frame and text annotations
        # as they weren't returned.
        self.assertNotIn('shot', frame.asset.get_attr('analysis.google.videoLabel'))
        self.assertNotIn('frame', frame.asset.get_attr('analysis.google.videoLabel'))
        self.assertNotIn('videoText', frame.asset.get_attr('analysis.google'))

    # Patch get_clip_bytes since it relies on an external call to ffmpeg, which may not be
    # installed.
    # Patch get_video_annotations since it relies on google.cloud and instead return mock results.
    @patch(
        'zmlp_analysis.google.processors.CloudVideoIntelligenceProcessor.'
        '_get_clip_bytes')
    @patch(
        'zmlp_analysis.google.processors.CloudVideoIntelligenceProcessor.'
        '_get_video_annotations')
    def test_process_shot_labels_only(self, get_video_annotations, get_clip_bytes):
        file_path = "/srt/srt_sample.mp4"

        self.mock_annotation_results.input_uri = file_path

        # Populate the mock with data to process - 1 annotation with category_entities
        self.mock_annotation_results.shot_label_annotations = [
            MockLabelAnnotation(
                category_entities=[
                    MockEntity(
                        entity_id="/m/01g317",
                        description="person",
                        language_code="en-US"
                    )
                ],
                entity=MockEntity(
                    entity_id="/m/01yrx",
                    description="facial expression",
                    language_code="en-US"
                )
            )
        ]

        # Set the mock returns
        get_clip_bytes.return_value = "bytes"
        get_video_annotations.return_value = self.mock_annotation_results

        # Instruct the processor to process annotation labels only
        self.processor.args['detect_labels'].value = True
        self.processor.args['detect_text'].value = False

        # Establish frame object and pass in your asset.
        frame = Frame(TestAsset(os.path.join(self.full_mock_data_dir, file_path)))
        frame.asset.set_attr('clip.start', 0.0)
        frame.asset.set_attr('clip.length', 111.0)

        self.processor.process(frame)

        # Confirm that all entity descriptions made it to the attributes dictionary as intended.
        self.assertIn('person', frame.asset.get_attr('analysis.google.videoLabel.shot.keywords'))
        self.assertIn('facial expression',
                      frame.asset.get_attr('analysis.google.videoLabel.shot.keywords'))

        # Confirm that no dictionaries were added to attributes for shot, frame and text annotations
        # as they weren't returned.
        self.assertNotIn('segment', frame.asset.get_attr('analysis.google.videoLabel'))
        self.assertNotIn('frame', frame.asset.get_attr('analysis.google.videoLabel'))
        self.assertNotIn('videoText', frame.asset.get_attr('analysis.google'))

    # Patch get_clip_bytes since it relies on an external call to ffmpeg, which may not be
    # installed.
    # Patch get_video_annotations since it relies on google.cloud and instead return mock results.
    @patch(
        'zmlp_analysis.google.processors.CloudVideoIntelligenceProcessor.'
        '_get_clip_bytes')
    @patch(
        'zmlp_analysis.google.processors.CloudVideoIntelligenceProcessor.'
        '_get_video_annotations')
    def test_process_frame_labels_only(self, get_video_annotations, get_clip_bytes):
        file_path = "/srt/srt_sample.mp4"

        self.mock_annotation_results.input_uri = file_path

        # Populate the mock with data to process - 1 annotation with no category_entities
        self.mock_annotation_results.frame_label_annotations = [
            MockLabelAnnotation(
                entity=MockEntity(
                    entity_id="/m/07s6nbt",
                    description="text",
                    language_code="en-US"
                )
            )
        ]

        # Set the mock returns
        get_clip_bytes.return_value = "bytes"
        get_video_annotations.return_value = self.mock_annotation_results

        # Instruct the processor to process annotation labels only
        self.processor.args['detect_labels'].value = True
        self.processor.args['detect_text'].value = False

        # Establish frame object and pass in your asset.
        frame = Frame(TestAsset(os.path.join(self.full_mock_data_dir, file_path)))
        frame.asset.set_attr('clip.start', 0.0)
        frame.asset.set_attr('clip.length', 111.0)

        self.processor.process(frame)

        # Confirm that all entity descriptions made it to the attributes dictionary as intended.
        self.assertIn('text', frame.asset.get_attr('analysis.google.videoLabel.frame.keywords'))

        # Confirm that no dictionaries were added to attributes for shot, frame and text annotations
        # as they weren't returned.
        self.assertNotIn('segment', frame.asset.get_attr('analysis.google.videoLabel'))
        self.assertNotIn('shot', frame.asset.get_attr('analysis.google.videoLabel'))
        self.assertNotIn('videoText', frame.asset.get_attr('analysis.google'))

    # Patch get_clip_bytes since it relies on an external call to ffmpeg, which may not be
    # installed.
    # Patch get_video_annotations since it relies on google.cloud and instead return mock results.
    @patch(
        'zmlp_analysis.google.processors.CloudVideoIntelligenceProcessor.'
        '_get_clip_bytes')
    @patch(
        'zmlp_analysis.google.processors.CloudVideoIntelligenceProcessor.'
        '_get_video_annotations')
    def test_process_video_text_only(self, get_video_annotations, get_clip_bytes):
        file_path = "/srt/srt_sample.mp4"

        self.mock_annotation_results.input_uri = file_path

        # Populate the mock with data to process - 1 annotation with no category_entities
        self.mock_annotation_results.text_annotations = [
            MockTextAnnotation(
                text="Interest"
            ),
            MockTextAnnotation(
                text="Robin"
            ),
            MockTextAnnotation(
                text="Rufatto"
            ),
            MockTextAnnotation(
                text="Mathematics"
            ),
            MockTextAnnotation(
                text="Instructor"
            )
        ]

        # Set the mock returns
        get_clip_bytes.return_value = "bytes"
        get_video_annotations.return_value = self.mock_annotation_results

        # Instruct the processor to process annotation labels only
        self.processor.args['detect_labels'].value = False
        self.processor.args['detect_text'].value = True

        # Establish frame object and pass in your asset.
        frame = Frame(TestAsset(os.path.join(self.full_mock_data_dir, file_path)))
        frame.asset.set_attr('clip.start', 0.0)
        frame.asset.set_attr('clip.length', 111.0)

        self.processor.process(frame)

        # Confirm that all video text made it to the attributes dictionary as intended.
        self.assertEqual(frame.asset.get_attr('analysis.google.videoText.content'),
                         'Interest Robin Rufatto Mathematics Instructor')

        # Confirm that no dictionaries were added to attributes for shot, frame and text annotations
        # as they weren't returned.
        self.assertNotIn('videoLabel', frame.asset.get_attr('analysis.google'))

    # Patch get_clip_bytes since it relies on an external call to ffmpeg, which may not be
    # installed.
    # Patch get_video_annotations since it relies on google.cloud and instead return mock results.
    @patch(
        'zmlp_analysis.google.processors.CloudVideoIntelligenceProcessor._get_clip_bytes')
    @patch(
        'zmlp_analysis.google.processors.CloudVideoIntelligenceProcessor._get_video_annotations')
    def test_process_all_annotations(self, get_video_annotations, get_clip_bytes):
        file_path = "/srt/srt_sample.mp4"

        self.mock_annotation_results.input_uri = file_path

        # Populate the mock with data to process - 2 annotations, one without category_entities
        self.mock_annotation_results.segment_label_annotations = [
            MockLabelAnnotation(
                category_entities=[
                    MockEntity(
                        entity_id="/m/01g317",
                        description="person",
                        language_code="en-US"
                    )
                ],
                entity=MockEntity(
                    entity_id="/m/016pp7",
                    description="happiness",
                    language_code="en-US"
                )
            ),
            MockLabelAnnotation(
                entity=MockEntity(
                    entity_id="/m/07s6nbt",
                    description="text",
                    language_code="en-US"
                )
            )
        ]

        # Populate the mock with data to process - 1 annotation with category_entities
        self.mock_annotation_results.shot_label_annotations = [
            MockLabelAnnotation(
                category_entities=[
                    MockEntity(
                        entity_id="/m/01g317",
                        description="person",
                        language_code="en-US"
                    )
                ],
                entity=MockEntity(
                    entity_id="/m/01yrx",
                    description="facial expression",
                    language_code="en-US"
                )
            )
        ]

        # Populate the mock with data to process - 1 annotation with no category_entities
        self.mock_annotation_results.frame_label_annotations = [
            MockLabelAnnotation(
                entity=MockEntity(
                    entity_id="/m/07s6nbt",
                    description="text",
                    language_code="en-US"
                )
            )
        ]

        # Populate the mock with data to process - 1 annotation with no category_entities
        self.mock_annotation_results.text_annotations = [
            MockTextAnnotation(
                text="Interest"
            ),
            MockTextAnnotation(
                text="Robin"
            ),
            MockTextAnnotation(
                text="Rufatto"
            ),
            MockTextAnnotation(
                text="Mathematics"
            ),
            MockTextAnnotation(
                text="Instructor"
            )
        ]

        # Set the mock returns
        get_clip_bytes.return_value = "bytes"
        get_video_annotations.return_value = self.mock_annotation_results

        # Instruct the processor to process both label annotation and text annotation
        self.processor.args['detect_labels'].value = True
        self.processor.args['detect_text'].value = True

        # Establish frame object and pass in your asset.
        frame = Frame(TestAsset(os.path.join(self.full_mock_data_dir, file_path)))
        frame.asset.set_attr('clip.start', 0.0)
        frame.asset.set_attr('clip.length', 111.0)

        self.processor.process(frame)

        # Confirm that all entity descriptions from all segments made it to the attributes
        # dictionary as intended.
        self.assertIn('person', frame.asset.get_attr('analysis.google.videoLabel.segment.keywords'))
        self.assertIn('happiness',
                      frame.asset.get_attr('analysis.google.videoLabel.segment.keywords'))
        self.assertIn('text', frame.asset.get_attr('analysis.google.videoLabel.segment.keywords'))

        # Confirm that all entity descriptions from all shots made it to the attributes dictionary
        # as intended.
        self.assertIn('person', frame.asset.get_attr('analysis.google.videoLabel.shot.keywords'))
        self.assertIn('facial expression',
                      frame.asset.get_attr('analysis.google.videoLabel.shot.keywords'))

        # Confirm that all entity descriptions from all frames made it to the attributes dictionary
        # as intended.
        self.assertIn('text', frame.asset.get_attr('analysis.google.videoLabel.frame.keywords'))

        # Confirm that all video text made it to the attributes dictionary as intended.
        self.assertEqual(frame.asset.get_attr('analysis.google.videoText.content'),
                         'Interest Robin Rufatto Mathematics Instructor')
