from unittest.mock import patch

from zmlp_analysis.azure.vision import (
    ComputerVisionObjectDetection,
    ComputerVisionLabelDetection
)
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_prediction_labels


image_path = zorroa_test_path('images/detect/dogbike.jpg')


class AzureObjectDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-object-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch('zmlp_analysis.azure.vision.get_zvi_azure_cv_client')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockACVClient()
        proxy_patch.return_value = image_path
        frame = Frame(TestAsset(image_path))

        processor = self.init_processor(ComputerVisionObjectDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'dog' in get_prediction_labels(analysis)


class AzureLabelDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-label-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch('zmlp_analysis.azure.vision.get_zvi_azure_cv_client')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockACVClient()
        proxy_patch.return_value = image_path
        frame = Frame(TestAsset(image_path))

        processor = self.init_processor(ComputerVisionLabelDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'bicycle' in get_prediction_labels(analysis)


class MockACVClient:

    def detect_objects_in_stream(self, image=None):
        return MockDetectResult()

    def analyze_image_in_stream(self, image=None, visual_features=None):
        return MockImageAnalysis()


class MockDetectResult:

    @property
    def objects(self):
        return [MockDetectedObjects()]


class MockDetectedObjects:

    @property
    def object_property(self):
        return 'dog'

    @property
    def confidence(self):
        return '0.873'


class MockImageAnalysis:

    @property
    def tags(self):
        return [MockTags()]


class MockTags:

    @property
    def name(self):
        return 'bicycle'

    @property
    def confidence(self):
        return '0.776'
