import os
import pytest
from unittest.mock import patch

from zmlp_analysis.azure import video
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_prediction_labels, \
    zorroa_test_path, get_mock_stored_file

VID_MP4 = "video/ted_talk.mp4"
BORIS_JOHNSON = "video/boris-johnson.mp4"
EIFFEL_TOWER = "video/eiffel-tower.mp4"
MODEL = "video/model.mp4"
MUSTANG = "video/ford.mp4"


@pytest.mark.skip(reason='dont run automatically')
class AzureVideoDetectorTestCase(PluginUnitTestCase):

    def setUp(self):
        cred_location = os.path.dirname(__file__) + '/azure-creds'
        with open(cred_location, 'rb') as f:
            key = f.read().decode()
        os.environ['ZORROA_AZURE_VISION_KEY'] = key

    def tearDown(self):
        del os.environ['ZORROA_AZURE_VISION_KEY']

    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_object_detection_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-video-object-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoObjectDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'person' in predictions

    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_label_detection_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-video-label-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoLabelDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'person' in predictions

    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_image_description_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-video-image-description-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoImageDescriptionDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'a person sitting in front of a laptop' in predictions

    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_tag_detection_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-video-tag-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoImageTagDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'person' in predictions

    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_celebrity_detection_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(BORIS_JOHNSON)
        namespace = 'analysis.azure-video-celebrity-detection'

        get_vid_patch.return_value = zorroa_test_path(BORIS_JOHNSON)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoCelebrityDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'Boris Johnson' in predictions

    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_landmark_detection_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(EIFFEL_TOWER)
        namespace = 'analysis.azure-video-landmark-detection'

        get_vid_patch.return_value = zorroa_test_path(EIFFEL_TOWER)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoLandmarkDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'Eiffel Tower' in predictions

    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_logo_detection_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(MUSTANG)
        namespace = 'analysis.azure-video-logo-detection'

        get_vid_patch.return_value = zorroa_test_path(MUSTANG)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoLogoDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'Ford' in predictions

    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_category_detection_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(EIFFEL_TOWER)
        namespace = 'analysis.azure-video-category-detection'

        get_vid_patch.return_value = zorroa_test_path(EIFFEL_TOWER)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoCategoryDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'building_' in predictions

    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_explicit_detection_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(MODEL)
        namespace = 'analysis.azure-video-explicit-detection'

        get_vid_patch.return_value = zorroa_test_path(MODEL)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoExplicitContentDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'racy' in predictions

    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_face_detection_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(BORIS_JOHNSON)
        namespace = 'analysis.azure-video-face-detection'

        get_vid_patch.return_value = zorroa_test_path(BORIS_JOHNSON)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.AzureVideoFaceDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'Male' in predictions

    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_text_detection_processor(self, get_vid_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'azure-video-text-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.zureVideoTextDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_analysis(namespace)
        assert 9 == analysis['words']
        assert 'and into of poop, sanitation sanitation, the toilets world' in analysis['content']
