import os
from unittest.mock import patch
import pytest

from boonai_analysis.clarifai.video import bboxes
from boonflow import Frame
from boonflow.testing import PluginUnitTestCase, test_path, TestAsset, get_prediction_labels


@pytest.mark.skip(reason='dont run automatically')
class ClarifaiBboxDetectionPublicModelsProcessorIntegrationTests(PluginUnitTestCase):

    def setUp(self):
        cred_location = os.path.join(os.path.dirname(__file__), '..', 'clarifai-creds')
        with open(cred_location, 'rb') as f:
            key = f.read().decode()
        os.environ['CLARIFAI_KEY'] = key

    def tearDown(self):
        del os.environ['CLARIFAI_KEY']

    @patch("boonai_analysis.clarifai.video.bboxes.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.video.bboxes.proxy.get_video_proxy')
    def run_process(self, proxy_path_patch, _, video_path, detector, attr, assertions):
        proxy_path_patch.return_value = video_path

        processor = self.init_processor(detector)
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)

        processor.process(frame)

        analysis = frame.asset.get_analysis(attr)
        predictions = get_prediction_labels(analysis)
        for label in assertions['labels']:
            assert label in predictions
        assert 'labels' in analysis['type']
        assert assertions['count'] == analysis['count']

    def test_face_detection_process(self):
        self.run_process(
            video_path=test_path('video/julia_roberts.mp4'),
            detector=bboxes.ClarifaiVideoFaceDetectionProcessor(),
            attr='clarifai-face-detection',
            assertions={'labels': ['face'], 'count': 1}
        )

    def test_logo_process(self):
        self.run_process(
            video_path=test_path('video/logos.mp4'),
            detector=bboxes.ClarifaiVideoLogoDetectionProcessor(),
            attr='clarifai-logo-detection',
            assertions={'labels': ['Skype', 'Instagram', 'Facebook'], 'count': 3}
        )
