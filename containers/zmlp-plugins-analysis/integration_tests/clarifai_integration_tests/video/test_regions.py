import os
from unittest.mock import patch
import pytest

from zmlp_analysis.clarifai.video import regions
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset, get_prediction_labels


@pytest.mark.skip(reason='dont run automatically')
class ClarifaiColorDetectionPublicModelsProcessorIntegrationTests(PluginUnitTestCase):

    def setUp(self):
        super(ClarifaiColorDetectionPublicModelsProcessorIntegrationTests, self).setUp()
        cred_location = os.path.join(os.path.dirname(__file__), '..', 'clarifai-creds')
        with open(cred_location, 'rb') as f:
            key = f.read().decode()
        os.environ['CLARIFAI_KEY'] = key

    def tearDown(self):
        super(ClarifaiColorDetectionPublicModelsProcessorIntegrationTests, self).tearDown()
        del os.environ['CLARIFAI_KEY']

    @patch("zmlp_analysis.clarifai.video.regions.video.save_timeline", return_value={})
    @patch('zmlp_analysis.clarifai.video.regions.proxy.get_video_proxy')
    def run_process(self, proxy_path_patch, _, video_path, detector, attr, assertions):
        proxy_path_patch.return_value = video_path

        processor = self.init_processor(detector)
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)

        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = frame.asset.get_analysis(attr)
        predictions = get_prediction_labels(analysis)
        for label in assertions['labels']:
            assert label in predictions
        assert 'labels' in analysis['type']
        assert assertions['count'] == analysis['count']

    def test_celebrity_process(self):
        self.run_process(
            video_path=zorroa_test_path('video/julia_roberts.mp4'),
            detector=regions.ClarifaiVideoCelebrityDetectionProcessor(),
            attr='clarifai-celebrity-detection',
            assertions={'labels': ['julia roberts'], 'count': 32}
        )

    def test_demographics_process(self):
        self.run_process(
            video_path=zorroa_test_path('video/julia_roberts.mp4'),
            detector=regions.ClarifaiVideoDemographicsDetectionProcessor(),
            attr='clarifai-demographics-detection',
            assertions={'labels': ['feminine', 'white'], 'count': 32}
        )
