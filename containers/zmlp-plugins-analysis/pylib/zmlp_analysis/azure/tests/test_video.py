import os
from unittest.mock import patch

import zmlp_analysis.azure.tests.test_vision as test_vision
from zmlpsdk import file_storage
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_prediction_labels, \
    zorroa_test_path, get_mock_stored_file

import zmlp_analysis.azure.video as video

patch_path = 'zmlp_analysis.azure.util.ComputerVisionClient'
cred_path = 'zmlp_analysis.azure.util.CognitiveServicesCredentials'

VID_MP4 = "video/ted_talk.mp4"
BORIS_JOHNSON = "video/boris-johnson.mp4"
EIFFEL_TOWER = "video/eiffel-tower.mp4"
MODEL = "video/model.mp4"
MUSTANG = "video/ford.mp4"


class AzureVisionProcessorTests(PluginUnitTestCase):

    def setUp(self):
        super(AzureVisionProcessorTests, self).setUp()
        os.environ["ZORROA_AZURE_VISION_KEY"] = "abc123"

    def tearDown(self):
        super(AzureVisionProcessorTests, self).tearDown()
        del os.environ["ZORROA_AZURE_VISION_KEY"]

    @patch(cred_path, side_effect=test_vision.MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=test_vision.MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_object_detection(self, get_vid_patch, store_patch, store_blob_patch,
                              _, __, ___):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-object-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoObjectDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'dog' in predictions

    @patch(cred_path, side_effect=test_vision.MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=test_vision.MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_label_detection(self, get_vid_patch, store_patch, store_blob_patch,
                             _, __, ___):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-label-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoLabelDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'bicycle' in predictions

    @patch(cred_path, side_effect=test_vision.MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=test_vision.MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_image_description(self, get_vid_patch, store_patch, store_blob_patch, _, __, ___):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-image-description-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoImageDescriptionDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        description = 'a dog sitting in front of a mirror posing for the camera'
        assert description in predictions

    @patch(cred_path, side_effect=test_vision.MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=test_vision.MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_tag_detection(self, get_vid_patch, store_patch, store_blob_patch, _, __, ___):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-tag-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoImageTagDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'bicycle' in predictions

    @patch(cred_path, side_effect=test_vision.MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=test_vision.MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_celeb_detection(self, get_vid_patch, store_patch, store_blob_patch,
                             _, __, ___):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-celebrity-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoCelebrityDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'Ryan Gosling' in predictions

    @patch(cred_path, side_effect=test_vision.MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=test_vision.MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_landmark_detection(self, get_vid_patch, store_patch, store_blob_patch,
                                _, __, ___):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-landmark-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoLandmarkDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'Eiffel Tower' in predictions

    @patch(cred_path, side_effect=test_vision.MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=test_vision.MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_logo_detection(self, get_vid_patch, store_patch, store_blob_patch,
                            _, __, ___):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-logo-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoLogoDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'Shell' in predictions

    @patch(cred_path, side_effect=test_vision.MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=test_vision.MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_category_detection(self, get_vid_patch, store_patch, store_blob_patch,
                                _, __, ___):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-category-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoCategoryDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'indoor_' in predictions

    @patch(cred_path, side_effect=test_vision.MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=test_vision.MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_explicit_detection(self, get_vid_patch, store_patch, store_blob_patch, _,
                                __, ___):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-explicit-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoExplicitContentDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'racy' in predictions

    @patch(cred_path, side_effect=test_vision.MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=test_vision.MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_face_detection(self, get_vid_patch, store_patch, store_blob_patch,
                            _, __, ___):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-face-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoFaceDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'Male' in predictions

    @patch(cred_path, side_effect=test_vision.MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=test_vision.MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_image_text_detection(self, get_vid_patch, store_patch, store_blob_patch,
                                  _, __, ___):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'azure-text-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoTextDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = frame.asset.get_analysis(namespace)
        assert '6050 6400 F N NIRVANA NO OUTLET PASEO PL STOP TAMAYO' in analysis['content']
