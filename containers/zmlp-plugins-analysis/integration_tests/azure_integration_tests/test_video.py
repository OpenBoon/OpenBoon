# flake8: noqa
import os
import pytest
from unittest.mock import patch

from zmlp_analysis.azure.video import AzureVideoDetector
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_prediction_labels, \
    zorroa_test_path, get_mock_stored_file


@pytest.mark.skip(reason='dont run automatically')
class AzureVideoDetectorTestCase(PluginUnitTestCase):

    def setUp(self):
        cred_location = os.path.dirname(__file__) + '/azure-creds'
        with open(cred_location, 'rb') as f:
            key = f.read().decode()
        os.environ['ZORROA_AZURE_VISION_KEY'] = key
        os.environ['ZORROA_AZURE_VISION_REGION'] = 'eastus'
        os.environ["PATH"] += ':/usr/local/bin/'

    def tearDown(self):
        del os.environ['ZORROA_AZURE_VISION_KEY']

    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.get_video_proxy')
    @patch('zmlp_analysis.azure.video.get_audio_proxy')
    def test_process_video_proxy(self,
                                 get_prx_patch, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path("video/ted_talk.mp4")
        namespace = 'analysis.azure-video-label-detection'

        get_prx_patch.return_value = 0
        get_vid_patch.return_value = zorroa_test_path("video/ted_talk.mp4")
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(AzureVideoDetector())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'person' in predictions
