import os
from unittest.mock import patch

import pytest

from zmlp_analysis.google import cloud_vision
from zmlp_analysis.google.cloud_vision import file_storage
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset, \
    get_prediction_labels, get_mock_stored_file


@pytest.mark.skip(reason='dont run automatically')
class CloudVisionProcessorTestCase(PluginUnitTestCase):

    def setUp(self):
        os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = os.path.dirname(__file__) + '/gcp-creds.json'

    def tearDown(self):
        del os.environ['GOOGLE_APPLICATION_CREDENTIALS']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_image_text_processor(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/set01/visa.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()
        frame = Frame(TestAsset(path))
        processor = self.init_processor(cloud_vision.CloudVisionDetectImageText())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-image-text-detection')
        assert 'content' == analysis['type']
        assert 'Giants Franchise History' in analysis['content']
        assert 41 == analysis['words']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_document_text_processor(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()
        frame = Frame(TestAsset(path))
        processor = self.init_processor(cloud_vision.CloudVisionDetectDocumentText())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-doc-text-detection')
        assert 'HEY GIRL' in analysis['content']
        assert 'content' in analysis['type']
        assert 12 == analysis['words']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_landmark_detection(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/set08/eiffel_tower.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()
        frame = Frame(TestAsset(path))
        processor = self.init_processor(cloud_vision.CloudVisionDetectLandmarks())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-landmark-detection')
        assert 'labels' == analysis['type']
        assert 'Eiffel Tower' in get_prediction_labels(analysis)
        assert 'Champ de Mars' in get_prediction_labels(analysis)
        assert 2 == analysis['count']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_explicit_detection(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()

        frame = Frame(TestAsset(path))
        processor = self.init_processor(cloud_vision.CloudVisionDetectExplicit())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-content-moderation')
        assert 'labels' == analysis['type']
        assert not analysis['explicit']
        assert 'spoof' in get_prediction_labels(analysis)
        assert 'violence' in get_prediction_labels(analysis)
        assert 3 == analysis['count']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_face_detection(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()
        asset = TestAsset(path)
        frame = Frame(asset)
        processor = self.init_processor(cloud_vision.CloudVisionDetectFaces())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-face-detection')
        assert 1 == analysis['count']
        assert 'labels' == analysis['type']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_logo_detection(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/set01/visa.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()
        frame = Frame(TestAsset(path))
        processor = self.init_processor(cloud_vision.CloudVisionDetectLogos())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-logo-detection')
        assert 'Visa Inc.' in get_prediction_labels(analysis)
        assert 7 == analysis['count']
        assert 'labels' == analysis['type']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_label_detection(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()
        frame = Frame(TestAsset(path))
        processor = self.init_processor(cloud_vision.CloudVisionDetectLabels())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-label-detection')
        assert 'Hair' in get_prediction_labels(analysis)
        assert 10 == analysis['count']
        assert 'labels' == analysis['type']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_object_detection(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/detect/dogbike.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()
        frame = Frame(TestAsset(path))
        processor = self.init_processor(cloud_vision.CloudVisionDetectObjects())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-object-detection')
        assert 'Dog' in get_prediction_labels(analysis)
        assert 5 == analysis['count']
        assert 'labels' == analysis['type']
