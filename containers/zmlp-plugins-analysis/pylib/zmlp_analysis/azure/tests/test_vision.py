import os
from unittest.mock import patch

from azure.cognitiveservices.vision.computervision.models import OperationStatusCodes

import zmlp_analysis.azure.vision as vision
from zmlpsdk import file_storage
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


class AzureVisionProcessorTests(PluginUnitTestCase):

    def setUp(self):
        os.environ["ZORROA_AZURE_VISION_KEY"] = "abc123"

    def tearDown(self):
        del os.environ["ZORROA_AZURE_VISION_KEY"]

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_object_detection(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(vision.AzureVisionObjectDetection())
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

        processor = self.init_processor(vision.AzureVisionLabelDetection())
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

        processor = self.init_processor(vision.AzureVisionImageDescriptionDetection())
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

        processor = self.init_processor(vision.AzureVisionImageTagsDetection())
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

        processor = self.init_processor(vision.AzureVisionCelebrityDetection())
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

        processor = self.init_processor(vision.AzureVisionLandmarkDetection())
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

        processor = self.init_processor(vision.AzureVisionLogoDetection())
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

        processor = self.init_processor(vision.AzureVisionCategoryDetection())
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

        processor = self.init_processor(vision.AzureVisionExplicitContentDetection())
        processor.process(frame)

        namespace = 'azure-explicit-detection'
        analysis = frame.asset.get_analysis(namespace)
        assert 'racy' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_face_detection(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = FACES
        frame = Frame(TestAsset(FACES))

        processor = self.init_processor(vision.AzureVisionFaceDetection())
        processor.process(frame)

        namespace = 'azure-face-detection'
        analysis = frame.asset.get_analysis(namespace)
        assert 'Male' in get_prediction_labels(analysis)

    @patch.object(file_storage, 'localize_file')
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_image_text_detection(self,  p_path, c_path, proxy_patch):
        proxy_patch.return_value = STREETSIGN
        frame = Frame(TestAsset(STREETSIGN))

        processor = self.init_processor(vision.AzureVisionTextDetection())
        processor.process(frame)

        namespace = 'azure-image-text-detection'
        analysis = frame.asset.get_analysis(namespace)
        assert 'N PASEO TAMAYO 6050 F NIRVANA PL 6400 N NO OUTLET STOP' in analysis['content']

    def test_not_a_quota_exception(self):
        assert vision.not_a_quota_exception(Exception('Foo'))
        assert not vision.not_a_quota_exception('Rate limit')


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
        return OperationStatusCodes.succeeded

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
                    'left': 0.397,
                    'top': 0.273,
                    'width': 0.773,
                    'height': 0.65
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
