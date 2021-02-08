import os
import logging
from unittest.mock import patch

import pytest

from zmlp_analysis.google.cloud_video import AsyncVideoIntelligenceProcessor
import zmlpsdk
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_prediction_labels

CREDS = os.path.join(os.path.dirname(__file__)) + '/gcp-creds.json'

logging.basicConfig(level=logging.INFO)


@pytest.mark.skip(reason='dont run automatically')
class AsyncVideoIntelligenceProcessorITestCase(PluginUnitTestCase):

    def setUp(self):
        os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = CREDS

    def tearDown(self):
        del os.environ['GOOGLE_APPLICATION_CREDENTIALS']

    @patch("zmlp_analysis.google.cloud_timeline.save_timeline", return_value={})
    @patch("zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.get_video_proxy_uri")
    @patch.object(file_storage.assets, 'store_blob')
    def test_detect_logos(self, store_blob_patch, native_patch, tl_patch):
        uri = 'gs://zorroa-dev-data/video/mustang.mp4'
        store_blob_patch.return_value = None
        native_patch.return_value = uri
        processor = self.init_processor(
            AsyncVideoIntelligenceProcessor(), {
                'detect_logos': 0.5,
            })

        uri = 'gs://zorroa-dev-data/video/mustang.mp4'
        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-logo-detection')

        assert 'labels' == analysis['type']
        assert 'Volvo' in get_prediction_labels(analysis)
        assert 'Ford Motor Company' in get_prediction_labels(analysis)
        assert 17 == analysis['count']

        timeline = tl_patch.call_args_list[0][0][1]
        assert "Wyoming Cowboys" in timeline.tracks
        assert "Nike" in timeline.tracks
        assert "Acura" in timeline.tracks

    @patch("zmlp_analysis.google.cloud_video.proxy.get_proxy_level", return_value=1)
    @patch("zmlp_analysis.google.cloud_timeline.save_timeline", return_value={})
    @patch("zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.get_video_proxy_uri")
    @patch.object(file_storage.assets, 'store_blob')
    def test_detect_labels(self, store_blob_patch, native_patch, tl_patch, _):
        uri = 'gs://zorroa-dev-data/video/ted_talk.mp4'
        store_blob_patch.return_value = None
        native_patch.return_value = uri

        processor = self.init_processor(
            AsyncVideoIntelligenceProcessor(), {
                'detect_labels': 0.5
            })

        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-label-detection')
        assert 'labels' == analysis['type']
        assert 'stage' in get_prediction_labels(analysis)
        assert 16 == analysis['count']

        timeline = tl_patch.call_args_list[0][0][1]
        assert "television program" in timeline.tracks
        assert "performance" in timeline.tracks
        assert "performing arts" in timeline.tracks

    @patch("zmlp_analysis.google.cloud_timeline.save_timeline", return_value={})
    @patch("zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.get_video_proxy_uri")
    @patch.object(file_storage.assets, 'store_blob')
    def test_detect_text(self, store_blob_patch, native_patch, tl_patch):
        uri = 'gs://zorroa-dev-data/video/ted_talk.mp4'
        store_blob_patch.return_value = None
        native_patch.return_value = uri

        processor = self.init_processor(
            AsyncVideoIntelligenceProcessor(), {
                'detect_text': 0.5
            })

        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-text-detection')
        assert 'content' == analysis['type']
        assert 'sanitation, toilets and poop' in analysis['content']
        assert 20 == analysis['words']

        timeline = tl_patch.call_args_list[0][0][1]
        assert "Detected Text" in timeline.tracks
        assert timeline.tracks['Detected Text']['clips'][0]['content'] == ["there's more coming -- (Laughter)-"]

    @patch("zmlp_analysis.google.cloud_timeline.save_timeline", return_value={})
    @patch("zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.get_video_proxy_uri")
    @patch.object(file_storage.assets, 'store_blob')
    def test_detect_objects(self, blob_patch, native_patch, tl_patch):
        uri = 'gs://zorroa-dev-data/video/model.mp4'
        blob_patch.return_value = None
        native_patch.return_value = uri

        processor = self.init_processor(AsyncVideoIntelligenceProcessor(), {
            'detect_objects': 0.25,
        })

        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-object-detection')
        assert 'labels' == analysis['type']
        assert 'swimwear' in get_prediction_labels(analysis)
        assert 'person' in get_prediction_labels(analysis)
        assert 4 == analysis['count']

        timeline = tl_patch.call_args_list[0][0][1]
        assert "footwear" in timeline.tracks
        assert "figurine" in timeline.tracks
        assert "swimwear" in timeline.tracks

    @patch("zmlp_analysis.google.cloud_timeline.save_timeline", return_value={})
    @patch("zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.get_video_proxy_uri")
    @patch.object(file_storage.assets, 'store_blob')
    def test_detect_explicit(self, blob_patch, native_patch, tl_patch):
        uri = 'gs://zorroa-dev-data/video/model.mp4'
        blob_patch.return_value = None
        native_patch.return_value = uri

        processor = self.init_processor(AsyncVideoIntelligenceProcessor(), {
            'detect_explicit': 3,
        })

        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-explicit-detection')
        assert 'labels' == analysis['type']
        assert 'very_unlikely' in get_prediction_labels(analysis)
        assert 'unlikely' in get_prediction_labels(analysis)
        assert 2 == analysis['count']

        timeline = tl_patch.call_args_list[0][0][1]
        assert "Very Unlikely" in timeline.tracks
        assert "Unlikely" in timeline.tracks

    @patch("zmlp_analysis.google.cloud_timeline.save_timeline", return_value={})
    @patch("zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.get_video_proxy_uri")
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    def test_speech_transcription(self, webvtt_patch, blob_patch, native_patch, tl_patch):
        uri = 'gs://zorroa-dev-data/video/ted_talk.mp4'
        webvtt_patch.return_value = None
        blob_patch.return_value = None
        native_patch.return_value = uri

        processor = self.init_processor(
            AsyncVideoIntelligenceProcessor(), {
                'detect_speech': True
            })

        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-speech-transcription')

        assert 'content' == analysis['type']
        assert "Sanitation" in analysis['content']

        timeline = tl_patch.call_args_list[0][0][1]
        assert 'Speech Transcription' in timeline.tracks

        with open(webvtt_patch.call_args_list[0][0][0]) as fp:
            vtt = fp.read()
        assert "Toilets and poop and I have yet to emerge." in vtt
        assert "Sanitation. There's more coming Sanitation." in vtt
