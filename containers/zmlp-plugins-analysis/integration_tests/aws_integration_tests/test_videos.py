import os
import csv
import logging
from unittest.mock import patch

import pytest

from zmlp_analysis.aws import videos
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_prediction_labels, \
    zorroa_test_path, get_mock_stored_file


VID_MP4 = "video/ted_talk.mp4"
MODEL = "video/model.mp4"
BORIS_JOHNSON = "video/boris-johnson.mp4"

logging.basicConfig()


@pytest.mark.skip(reason='dont run automatically')
class RekognitionVideoTestCase(PluginUnitTestCase):

    def setUp(self):
        with open('aws_credentials.csv', 'r') as f:
            next(f)
            reader = csv.reader(f)
            for line in reader:
                access_key_id = line[2]
                secret_access_key = line[3]

        os.environ['ZORROA_AWS_KEY'] = access_key_id
        os.environ['ZORROA_AWS_SECRET'] = secret_access_key
        os.environ['ZORROA_AWS_BUCKET'] = 'zorroa-integration-tests'
        os.environ['ZORROA_AWS_REGION'] = 'us-east-1'
        os.environ['ZMLP_PROJECT_ID'] = '00000000-0000-0000-0000-000000000001'

    def tearDown(self):
        del os.environ['ZORROA_AWS_KEY']
        del os.environ['ZORROA_AWS_SECRET']
        del os.environ['ZORROA_AWS_BUCKET']
        del os.environ['ZORROA_AWS_REGION']
        del os.environ['ZMLP_PROJECT_ID']

    @patch("zmlp_analysis.aws.videos.labels.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.videos.labels.proxy.get_video_proxy')
    def test_label_detection_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.aws-label-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(videos.RekognitionVideoLabelDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        assert 'Person' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.aws.videos.faces.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.videos.faces.proxy.get_video_proxy')
    def test_face_detection_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.aws-face-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(videos.RekognitionVideoFaceDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        assert 'face0' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.aws.videos.nsfw.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.videos.nsfw.proxy.get_video_proxy')
    def test_unsafe_detection_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(MODEL)
        namespace = 'analysis.aws-unsafe-detection'

        get_vid_patch.return_value = zorroa_test_path(MODEL)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(videos.RekognitionVideoUnsafeDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        assert 'Suggestive' in get_prediction_labels(analysis)
        assert 'Female Swimwear Or Underwear' in get_prediction_labels(analysis)
        assert analysis['count'] == 3

    @patch("zmlp_analysis.aws.videos.celebs.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.videos.celebs.proxy.get_video_proxy')
    def test_celebrity_detection_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(BORIS_JOHNSON)
        namespace = 'analysis.aws-celebrity-detection'

        get_vid_patch.return_value = zorroa_test_path(BORIS_JOHNSON)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(videos.RekognitionVideoCelebrityDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        assert 'Boris Johnson' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.aws.videos.text.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.videos.text.proxy.get_video_proxy')
    def test_text_detection_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.aws-text-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(videos.RekognitionVideoTextDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        assert 'poop,' in get_prediction_labels(analysis)
        assert 11 == analysis['count']
