import os
import pytest
from unittest.mock import patch

from zmlp_analysis.azure import vision
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_prediction_labels


DOGBIKE = zorroa_test_path('images/detect/dogbike.jpg')
STREETSIGN = zorroa_test_path("images/set09/streetsign.jpg")
RYAN_GOSLING = zorroa_test_path('images/set08/meme.jpg')
EIFFEL_TOWER = zorroa_test_path('images/set11/eiffel_tower.jpg')
LOGOS = zorroa_test_path('images/set11/logos.jpg')
NSFW = zorroa_test_path('images/set10/nsfw1.jpg')
FACES = zorroa_test_path('images/set01/faces.jpg')


@pytest.mark.skip(reason='dont run automatically')
class ComputerVisionProcessorTestCase(PluginUnitTestCase):

    def setUp(self):
        cred_location = os.path.dirname(__file__) + '/azure-creds'
        with open(cred_location, 'rb') as f:
            key = f.read().decode()
        os.environ['ZORROA_AZURE_VISION_KEY'] = key

    def tearDown(self):
        del os.environ['ZORROA_AZURE_VISION_KEY']

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    def test_object_detection_processor(self, proxy_patch):
        namespace = 'azure-object-detection'
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(vision.AzureVisionObjectDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(namespace)
        assert 'dog' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    def test_label_detection_processor(self, proxy_patch):
        namespace = 'azure-label-detection'
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(vision.AzureVisionLabelDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(namespace)
        assert 'bicycle' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    def test_image_description_processor(self, proxy_patch):
        namespace = 'azure-image-description-detection'
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(vision.AzureVisionImageDescription())
        processor.process(frame)

        analysis = frame.asset.get_analysis(namespace)
        description = 'a dog sitting in front of a mirror posing for the camera'
        assert description in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    def test_tag_detection_processor(self, proxy_patch):
        namespace = 'azure-tag-detection'
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(vision.AzureVisionImageTagsDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(namespace)
        assert 'bicycle' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    def test_celebrity_detection_processor(self, proxy_patch):
        namespace = 'azure-celebrity-detection'
        proxy_patch.return_value = RYAN_GOSLING
        frame = Frame(TestAsset(RYAN_GOSLING))

        processor = self.init_processor(vision.AzureVisionCelebrityDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(namespace)
        assert 'Ryan Gosling' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    def test_landmark_detection_processor(self, proxy_patch):
        namespace = 'azure-landmark-detection'
        proxy_patch.return_value = EIFFEL_TOWER
        frame = Frame(TestAsset(EIFFEL_TOWER))

        processor = self.init_processor(vision.AzureVisionLandmarkDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(namespace)
        assert 'Eiffel Tower' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    def test_logo_detection_processor(self, proxy_patch):
        namespace = 'azure-logo-detection'
        proxy_patch.return_value = LOGOS
        frame = Frame(TestAsset(LOGOS))

        processor = self.init_processor(vision.AzureVisionLogoDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(namespace)
        assert 'Shell' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    def test_category_detection_processor(self, proxy_patch):
        namespace = 'azure-category-detection'
        proxy_patch.return_value = EIFFEL_TOWER
        frame = Frame(TestAsset(EIFFEL_TOWER))

        processor = self.init_processor(vision.AzureVisionCategoryDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(namespace)
        assert 'building_' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    def test_explicit_detection_processor(self, proxy_patch):
        namespace = 'azure-explicit-detection'
        proxy_patch.return_value = NSFW
        frame = Frame(TestAsset(NSFW))

        processor = self.init_processor(vision.AzureVisionExplicitContentDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(namespace)
        assert 'racy' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    def test_face_detection_processor(self, proxy_patch):
        namespace = 'azure-face-detection'
        proxy_patch.return_value = FACES
        frame = Frame(TestAsset(FACES))

        processor = self.init_processor(vision.AzureVisionFaceDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(namespace)
        assert 'Male' in get_prediction_labels(analysis)

    @patch("zmlp_analysis.azure.vision.file_storage.localize_file")
    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    def test_text_detection_processor(self, proxy_patch, localize_patch):
        namespace = 'azure-image-text-detection'
        proxy_patch.return_value = STREETSIGN
        localize_patch.return_value = STREETSIGN
        frame = Frame(TestAsset(STREETSIGN))

        processor = self.init_processor(vision.AzureVisionTextDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(namespace)
        assert 'content' in analysis['type']
        assert 12 == analysis['words']
        assert 'N PASEO TAMAYO 6050 F NIRVANA PL 6400 N NO OUTLET STOP' in analysis['content']
