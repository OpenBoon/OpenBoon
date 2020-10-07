# flake8: noqa
from unittest.mock import patch

from azure.cognitiveservices.vision.computervision.models import OperationStatusCodes

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
FACES = zorroa_test_path('images/set01/faces.jpg')


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

    def read_in_stream(self, image=None, raw=True):
        return MockRawResult()

    def get_read_result(self, operation_id=None):
        return MockReadResult()


class AzureObjectDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-object-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(AzureVisionObjectDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'dog' in get_prediction_labels(analysis)


class AzureLabelDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-label-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(AzureVisionLabelDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'bicycle' in get_prediction_labels(analysis)


class AzureImageDescriptionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-image-description-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self,  p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(AzureVisionImageDescription())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        description = 'a dog sitting in front of a mirror posing for the camera'
        assert description in get_prediction_labels(analysis)


class AzureTagDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-tag-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(AzureVisionImageTagsDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'bicycle' in get_prediction_labels(analysis)


class AzureCelebrityDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-celebrity-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch('zmlp_analysis.azure.vision.get_zvi_azure_cv_client')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockACVClient()
        proxy_patch.return_value = RYAN_GOSLING
        frame = Frame(TestAsset(RYAN_GOSLING))

        processor = self.init_processor(AzureVisionCelebrityDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'Ryan Gosling' in get_prediction_labels(analysis)


class AzureLandmarkDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-landmark-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = EIFFEL_TOWER
        frame = Frame(TestAsset(EIFFEL_TOWER))

        processor = self.init_processor(AzureVisionLandmarkDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'Eiffel Tower' in get_prediction_labels(analysis)


class AzureLogoDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-logo-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = LOGOS
        frame = Frame(TestAsset(LOGOS))

        processor = self.init_processor(AzureVisionLogoDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'Shell' in get_prediction_labels(analysis)


class AzureCategoryDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-category-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(AzureVisionCategoryDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'indoor_' in get_prediction_labels(analysis)


class AzureExplicitContentDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-explicit-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(AzureVisionExplicitContentDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'racy' in get_prediction_labels(analysis)


class AzureFaceDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-face-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = FACES
        frame = Frame(TestAsset(FACES))

        processor = self.init_processor(AzureVisionFaceDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'Male' in get_prediction_labels(analysis)


class AzureImageTextProcessorTests(PluginUnitTestCase):
    namespace = 'azure-image-text-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self,  p_path, c_path, proxy_patch):
        proxy_patch.return_value = STREETSIGN
        frame = Frame(TestAsset(STREETSIGN))

        processor = self.init_processor(AzureVisionTextDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'N PASEO TAMAYO 6050 F NIRVANA PL 6400 N NO OUTLET STOP' in analysis['content']


class MockDetectResult:

    @property
    def objects(self):
        return [MockDetectedObjects()]

    @property
    def captions(self):
        return [MockDetectedObjects()]


class MockRawResult:

    @property
    def headers(self):
        return {
          "Content-Length": "0",
          "Operation-Location": "https://eastus.api.cognitive.microsoft.com/vision/v3.0/read/"
                                "analyzeResults/4b934ad6-c104-4549-971e-4a822ac5d69e",
          "x-envoy-upstream-service-time": "66",
          "CSP-Billing-Usage": "CognitiveServices.ComputerVision.Transaction=1",
          "apim-request-id": "4b934ad6-c104-4549-971e-4a822ac5d69e",
          "Strict-Transport-Security": "max-age=31536000; includeSubDomains; preload",
          "x-content-type-options": "nosniff",
          "Date": "Wed, 07 Oct 2020 18:28:56 GMT"
        }


class MockReadResult:

    @property
    def status(self):
        return  OperationStatusCodes.succeeded

    @property
    def analyze_result(self):
        return ReadResults()


class ReadResults:

    @property
    def read_results(self):
        return [MockLines()]


class MockLines:

    @property
    def lines(self):
        return [MockText()]

class MockText:

    @property
    def text(self):
        return 'N PASEO TAMAYO 6050 F NIRVANA PL 6400 N NO OUTLET STOP'


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
    def faces(self):
        return [MockFaces()]

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


class MockCategories:

    @property
    def name(self):
        return 'indoor_'

    @property
    def score(self):
        return 0.935


class MockFaces:

    @property
    def gender(self):
        return 'Male'

    @property
    def age(self):
        return '5'

    @property
    def face_rectangle(self):
        return MockBoundingBox()


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

    @property
    def left(self):
        return 0

    @property
    def top(self):
        return 0

    @property
    def width(self):
        return 1

    @property
    def height(self):
        return 1
