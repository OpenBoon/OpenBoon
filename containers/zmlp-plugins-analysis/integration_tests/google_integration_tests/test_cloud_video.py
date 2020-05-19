import os
import pytest
from unittest.mock import patch

from zmlp_analysis.google.cloud_video import AsyncVideoIntelligenceProcessor
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_prediction_labels

CREDS = os.path.join(os.path.dirname(__file__)) + '/gcp-creds.json'


@pytest.mark.skip(reason='dont run automatically')
class AsyncVideoIntelligenceProcessorITestCase(PluginUnitTestCase):

    def setUp(self):
        os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = os.path.dirname(__file__) + '/gcp-creds.json'

    def tearDown(self):
        del os.environ['GOOGLE_APPLICATION_CREDENTIALS']

    @patch.object(file_storage.assets, 'store_blob')
    def test_detect_logos(self, store_blob_patch):
        store_blob_patch.return_value = None

        processor = self.init_processor(
            AsyncVideoIntelligenceProcessor(), {
                'detect_logos': 0.5,
            })

        uri = 'gs://zorroa-dev-data/video/mustang.mp4'
        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        asset.set_attr('clip.track', 'full')
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-logo-detection')
        assert 'labels' == analysis['type']
        assert 'Volvo' in get_prediction_labels(analysis)
        assert 'Ford Motor Company' in get_prediction_labels(analysis)
        assert 16 == analysis['count']

    @patch.object(file_storage.assets, 'store_blob')
    def test_detect_labels(self, store_blob_patch):
        store_blob_patch.return_value = None

        processor = self.init_processor(
            AsyncVideoIntelligenceProcessor(), {
                'detect_labels': 0.5
            })

        uri = 'gs://zorroa-dev-data/video/ted_talk.mp4'
        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        asset.set_attr('clip.track', 'full')
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-label-detection')
        assert 'labels' == analysis['type']
        assert 'stage' in get_prediction_labels(analysis)
        assert 12 == analysis['count']

    @patch.object(file_storage.assets, 'store_blob')
    def test_detect_text(self, store_blob_patch):
        store_blob_patch.return_value = None

        processor = self.init_processor(
            AsyncVideoIntelligenceProcessor(), {
                'detect_text': 0.5
            })

        uri = 'gs://zorroa-dev-data/video/ted_talk.mp4'
        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        asset.set_attr('clip.track', 'full')
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-text-detection')
        assert 'content' == analysis['type']
        assert 'sanitation, toilets and poop' in analysis['content']
        assert 20 == analysis['words']

    @patch.object(file_storage.assets, 'store_blob')
    def test_detect_objects(self, blob_patch):
        blob_patch.return_value = None

        processor = self.init_processor(AsyncVideoIntelligenceProcessor(), {
            'detect_objects': 0.25,
        })

        uri = 'gs://zorroa-dev-data/video/model.mp4'
        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        asset.set_attr('clip.track', 'full')
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-object-detection')
        assert 'labels' == analysis['type']
        assert 'swimwear' in get_prediction_labels(analysis)
        assert 'person' in get_prediction_labels(analysis)
        assert 4 == analysis['count']

    @patch.object(file_storage.assets, 'store_blob')
    def test_detect_explicit(self, blob_patch):
        blob_patch.return_value = None

        processor = self.init_processor(AsyncVideoIntelligenceProcessor(), {
            'detect_explicit': 3,
        })

        uri = 'gs://zorroa-dev-data/video/model.mp4'
        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        asset.set_attr('clip.track', 'full')
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-explicit-detection')
        assert 'labels' == analysis['type']
        assert 'very_unlikely' in get_prediction_labels(analysis)
        assert 'unlikely' in get_prediction_labels(analysis)
        assert 2 == analysis['count']
