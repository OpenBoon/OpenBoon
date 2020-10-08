# flake8: noqa
import os
from unittest.mock import patch

from zmlp_analysis.azure import *
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_prediction_labels

patch_path = 'zmlp_analysis.azure.util.ComputerVisionClient'
cred_path = 'zmlp_analysis.azure.util.CognitiveServicesCredentials'

DOGBIKE = zorroa_test_path('images/detect/dogbike.jpg')
STREETSIGN = zorroa_test_path("images/set09/streetsign.jpg")
RYAN_GOSLING = zorroa_test_path('images/set08/meme.jpg')
EIFFEL_TOWER = zorroa_test_path('images/set11/eiffel_tower.jpg')
LOGOS = zorroa_test_path('images/set11/logos.jpg')
NSFW = zorroa_test_path('images/set10/nsfw1.jpg')


class MockCognitiveServicesCredentials:

    def __init__(self, subscription_key=None):
        pass


class MockACVClient:

    def __init__(self, endpoint=None, credentials=None):
        pass

    def detect_objects_in_stream(self, image=None):
        return MockDetectResult()

    def analyze_image_in_stream(self, image=None, visual_features=None):
        return MockImageAnalysis()

    def describe_image_in_stream(self, image=None):
        return MockDetectResult()

    def tag_image_in_stream(self, image=None):
        return MockImageAnalysis()

    def analyze_image_by_domain_in_stream(self, model=None, image=None):
        return MockImageAnalysis()


class AzureVisionProcessorTests(PluginUnitTestCase):

    def setUp(self):
        os.environ["ZORROA_AZURE_KEY"] = "abc123"

    def tearDown(self):
        del os.environ["ZORROA_AZURE_KEY"]

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_object_detection(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(AzureVisionObjectDetection())
        processor.process(frame)

        namespace = 'azure-object-detection'
        analysis = frame.asset.get_analysis(namespace)
        assert 'dog' in get_prediction_labels(analysis)


    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_label_detection(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(AzureVisionLabelDetection())
        processor.process(frame)

        namespace = 'azure-label-detection'
        analysis = frame.asset.get_analysis(namespace)
        assert 'bicycle' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_image_description(self,  p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(AzureVisionImageDescription())
        processor.process(frame)

        namespace = 'azure-image-description-detection'
        analysis = frame.asset.get_analysis(namespace)
        description = 'a dog sitting in front of a mirror posing for the camera'
        assert description in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_tag_detection(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(AzureVisionImageTagsDetection())
        processor.process(frame)

        namespace = 'azure-tag-detection'
        analysis = frame.asset.get_analysis(namespace)
        assert 'bicycle' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch('zmlp_analysis.azure.vision.get_zvi_azure_cv_client')
    def test_celeb_detection(self, client_patch, proxy_patch):
        client_patch.return_value = MockACVClient()
        proxy_patch.return_value = RYAN_GOSLING
        frame = Frame(TestAsset(RYAN_GOSLING))

        processor = self.init_processor(AzureVisionCelebrityDetection())
        processor.process(frame)

        namespace = 'azure-celebrity-detection'
        analysis = frame.asset.get_analysis(namespace)
        assert 'Ryan Gosling' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_landmark_detection(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = EIFFEL_TOWER
        frame = Frame(TestAsset(EIFFEL_TOWER))

        processor = self.init_processor(AzureVisionLandmarkDetection())
        processor.process(frame)

        namespace = 'azure-landmark-detection'
        analysis = frame.asset.get_analysis(namespace)
        assert 'Eiffel Tower' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_logo_detection(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = LOGOS
        frame = Frame(TestAsset(LOGOS))

        processor = self.init_processor(AzureVisionLogoDetection())
        processor.process(frame)

        namespace = 'azure-logo-detection'
        analysis = frame.asset.get_analysis(namespace)
        assert 'Shell' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_category_detection(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(AzureVisionCategoryDetection())
        processor.process(frame)

        namespace = 'azure-category-detection'
        analysis = frame.asset.get_analysis(namespace)
        assert 'indoor_' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_explicit_detection(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(AzureVisionExplicitContentDetection())
        processor.process(frame)

        namespace = 'azure-explicit-detection'
        analysis = frame.asset.get_analysis(namespace)
        assert 'racy' in get_prediction_labels(analysis)


class MockDetectResult:

    @property
    def objects(self):
        return [MockDetectedObjects()]

    @property
    def captions(self):
        return [MockDetectedObjects()]


class MockDetectedObjects:

    @property
    def object_property(self):
        return 'dog'

    @property
    def confidence(self):
        return '0.873'

    @property
    def text(self):
        return 'a dog sitting in front of a mirror posing for the camera'

    @property
    def rectangle(self):
        return MockBoundingBox()


class MockImageAnalysis:

    @property
    def tags(self):
        return [MockTags()]

    @property
    def brands(self):
        return [MockBrands()]

    @property
    def categories(self):
        return [MockCategories()]

    @property
    def adult(self):
        return MockExplicit()

    @property
    def result(self):
        return {
            'celebrities': [{
                'name': 'Ryan Gosling',
                'confidence': 0.995,
                'faceRectangle': {
                    'left': 118,
                    'top': 159,
                    'width': 94,
                    'height': 94
                }
            }],
            'landmarks': [{
                'name': 'Eiffel Tower',
                'confidence': 0.998
            }]
        }


class MockTags:

    @property
    def name(self):
        return 'bicycle'

    @property
    def confidence(self):
        return 0.776


class MockBrands:

    @property
    def name(self):
        return 'Shell'

    @property
    def confidence(self):
        return 0.935

    @property
    def rectangle(self):
        return MockBoundingBox()


class MockBoundingBox:

    @property
    def x(self):
        return 0

    @property
    def y(self):
        return 0

    @property
    def w(self):
        return 1

    @property
    def h(self):
        return 1


class MockCategories:

    @property
    def name(self):
        return 'indoor_'

    @property
    def score(self):
        return 0.935


class MockExplicit:

    @property
    def adult_score(self):
        return 0.935

    @property
    def racy_score(self):
        return 0.935

    @property
    def gore_score(self):
        return 0.935

    @property
    def is_racy_content(self):
        return True if self.racy_score() >= 0.50 else False
