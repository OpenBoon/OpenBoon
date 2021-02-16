import json
import logging
import os
from unittest.mock import patch

import pytest

from zmlp_analysis.aws import video
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, \
    get_mock_stored_file, get_prediction_labels

VID_MP4 = "video/credits.mov"
MUSTANG = "video/mustang.mp4"
TED_TALK = "video/ted_talk.mp4"
MODEL = "video/model.mp4"
BORIS_JOHNSON = "video/boris-johnson.mp4"
MEDIA_LENGTH = 101.0

logging.basicConfig()


@pytest.mark.skip(reason='dont run automatically')
class AmazonVideoProcessorTestCase(PluginUnitTestCase):

    def setUp(self):
        self.path = zorroa_test_path('fallback/ted_talk.mp4')
        self.asset = TestAsset(self.path)
        self.asset.set_attr('media.length', MEDIA_LENGTH)

        with open('aws-env.json', 'r') as fp:
            self.aws_env = json.load(fp)

        for k, v in self.aws_env.items():
            os.environ[k] = v

    def tearDown(self):
        for k in self.aws_env.keys():
            del os.environ[k]

    @patch("zmlp_analysis.aws.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.proxy.get_video_proxy')
    def test_label_detection(self, get_prx_patch,
                             store_patch, store_blob_patch, tl_patch):
        video_path = zorroa_test_path(TED_TALK)
        get_prx_patch.return_value = zorroa_test_path(TED_TALK)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.RekognitionLabelDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        processor.preprocess([asset])
        processor.process(Frame(asset))

        analysis = asset.get_analysis('aws-label-detection')
        preds = get_prediction_labels(analysis)
        assert 'Person' in preds
        assert analysis['count'] > 10

        timeline = tl_patch.call_args_list[0][0][1]
        jtl = timeline.for_json()
        assert jtl['tracks'][0]['name'] == 'Arm'
        assert int(jtl['tracks'][0]['clips'][0]['score']) == 52

    @patch("zmlp_analysis.aws.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.proxy.get_video_proxy')
    def test_process_unsafe_detection(self, get_prx_patch,
                                      store_patch, store_blob_patch, tl_patch):
        video_path = zorroa_test_path(MODEL)
        get_prx_patch.return_value = zorroa_test_path(MODEL)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.RekognitionUnsafeDetection())
        asset = TestAsset(video_path, id="abcdefg")
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(asset)
        processor.preprocess([asset])
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-unsafe-detection')
        preds = get_prediction_labels(analysis)
        assert 'Suggestive' in preds
        assert analysis['count'] == 3

        timeline = tl_patch.call_args_list[0][0][1]
        jtl = timeline.for_json()
        assert jtl['tracks'][0]['name'] == 'Revealing Clothes'
        assert int(jtl['tracks'][0]['clips'][0]['score']) == 93

    @patch("zmlp_analysis.aws.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.proxy.get_video_proxy')
    def test_process_celebrity_detection(
            self, get_prx_patch, store_patch, store_blob_patch, tl_patch):
        video_path = zorroa_test_path(BORIS_JOHNSON)
        get_prx_patch.return_value = zorroa_test_path(BORIS_JOHNSON)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.RekognitionCelebrityDetection())
        asset = TestAsset(video_path, id="abcdefg")
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(asset)
        processor.preprocess([asset])
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-celebrity-detection')
        preds = get_prediction_labels(analysis)
        assert 'Boris Johnson' in preds
        assert analysis['count'] == 2

        timeline = tl_patch.call_args_list[0][0][1]
        jtl = timeline.for_json()
        assert jtl['tracks'][0]['name'] == 'Slobodan Živojinović'
        assert int(jtl['tracks'][0]['clips'][0]['score']) == 94

    @patch("zmlp_analysis.aws.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.proxy.get_video_proxy')
    def test_process_black_frame_detection(
            self, get_prx_patch, store_patch, store_blob_patch, tl_patch):
        video_path = zorroa_test_path(MUSTANG)
        get_prx_patch.return_value = zorroa_test_path(MUSTANG)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.BlackFramesVideoDetectProcessor())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(asset)
        processor.preprocess([asset])
        processor.process(frame)

        timeline = tl_patch.call_args_list[0][0][1]
        jtl = timeline.for_json()
        assert jtl['tracks'][0]['name'] == 'Black Frames'
        assert round(jtl['tracks'][0]['clips'][0]['score'], 3) == 1.0

    @patch("zmlp_analysis.aws.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.proxy.get_video_proxy')
    def test_process_end_credits_detection(
            self, get_prx_patch, store_patch, store_blob_patch, tl_patch):

        video_path = zorroa_test_path(VID_MP4)
        get_prx_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.EndCreditsVideoDetectProcessor())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(asset)
        processor.preprocess([asset])
        processor.process(frame)

        frame.asset.get_analysis('aws-credits-detection')

        timeline = tl_patch.call_args_list[0][0][1]
        jtl = timeline.for_json()
        assert jtl['tracks'][0]['name'] == 'End Credits'
        assert round(jtl['tracks'][0]['clips'][0]['score'], 3) == 0.998
