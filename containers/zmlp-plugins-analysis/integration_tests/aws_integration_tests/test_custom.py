import logging
import os
import csv
from unittest.mock import patch

import pytest

from zmlp_analysis.aws import videos
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, \
    get_mock_stored_file, get_prediction_labels

VID_MP4 = "video/sample_ipad.m4v"
MEDIA_LENGTH = 101.0

logging.basicConfig()


@pytest.mark.skip(reason='dont run automatically')
class CustomLabelDetectionProcessorTestCase(PluginUnitTestCase):

    def setUp(self):
        self.path = zorroa_test_path(VID_MP4)
        self.asset = TestAsset(self.path)
        self.asset.set_attr('media.length', MEDIA_LENGTH)

        with open("aws_credentials.csv", 'r') as f:
            next(f)
            reader = csv.reader(f)
            for line in reader:
                access_key_id = line[2]
                secret_access_key = line[3]

        with open('rekog_project_version_arn', 'r') as f:
            lines = f.readlines()
            os.environ['ZORROA_AWS_ML_USER_PROJECT_ARN'] = lines[0].strip()
            os.environ['ZORROA_AWS_ML_USER_PROJECT_VERSION_ARN'] = lines[1]

        os.environ['ZORROA_AWS_KEY'] = access_key_id
        os.environ['ZORROA_AWS_SECRET'] = secret_access_key
        os.environ['ZORROA_AWS_BUCKET'] = 'zorroa-integration-tests'
        os.environ['ZORROA_AWS_REGION'] = 'us-east-2'
        os.environ['ZMLP_PROJECT_ID'] = '00000000-0000-0000-0000-000000000001'

    def tearDown(self):
        del os.environ['ZORROA_AWS_KEY']
        del os.environ['ZORROA_AWS_SECRET']
        del os.environ['ZORROA_AWS_BUCKET']
        del os.environ['ZORROA_AWS_REGION']
        del os.environ['ZMLP_PROJECT_ID']

    @patch("zmlp_analysis.aws.videos.custom.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.videos.custom.proxy.get_video_proxy')
    def test_process_label_detection(self, get_prx_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(VID_MP4)
        get_prx_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(videos.CustomLabelVideoDetectProcessor())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(self.asset)
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-video-detection')
        preds = get_prediction_labels(analysis)
        assert 'bird' in preds
        assert analysis['count'] == 1
