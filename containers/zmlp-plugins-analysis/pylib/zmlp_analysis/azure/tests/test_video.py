# flake8: noqa
from zmlp_analysis.azure.tests.test_vision import *
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_prediction_labels, \
    zorroa_test_path, get_mock_stored_file
from zmlpsdk import file_storage

patch_path = 'zmlp_analysis.azure.util.ComputerVisionClient'
cred_path = 'zmlp_analysis.azure.util.CognitiveServicesCredentials'

VID_MP4 = "video/ted_talk.mp4"
BORIS_JOHNSON = "video/boris-johnson.mp4"
EIFFEL_TOWER = "video/eiffel-tower.mp4"
MODEL = "video/model.mp4"
MUSTANG = "video/ford.mp4"


class AzureVisionProcessorTests(PluginUnitTestCase):

    def setUp(self):
        os.environ["ZORROA_AZURE_VISION_KEY"] = "abc123"

    def tearDown(self):
        del os.environ["ZORROA_AZURE_VISION_KEY"]

    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_object_detection(self, get_vid_patch, store_patch, store_blob_patch, t_path, p_path,
                              c_path):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-video-object-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(AzureVideoObjectDetector())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'dog' in predictions

    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_label_detection(self, get_vid_patch, store_patch, store_blob_patch, t_path, p_path,
                             c_path):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-video-label-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(AzureVideoLabelDetector())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'bicycle' in predictions

    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_image_description(self, get_vid_patch, store_patch, store_blob_patch, t_path,
                               p_path, c_path):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-video-image-description-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(AzureVideoImageDescriptionDetector())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        description = 'a dog sitting in front of a mirror posing for the camera'
        assert description in predictions

    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_tag_detection(self, get_vid_patch, store_patch, store_blob_patch, t_path, p_path,
                           c_path):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-video-tag-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(AzureVideoImageTagDetector())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'bicycle' in predictions

    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_celeb_detection(self, get_vid_patch, store_patch, store_blob_patch, t_path,
                                p_path, c_path):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-video-celebrity-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(AzureVideoCelebrityDetector())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'Ryan Gosling' in predictions

    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_landmark_detection(self, get_vid_patch, store_patch, store_blob_patch, t_path,
                                p_path, c_path):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-video-landmark-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(AzureVideoLandmarkDetector())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'Eiffel Tower' in predictions

    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_logo_detection(self, get_vid_patch, store_patch, store_blob_patch, t_path, p_path,
                            c_path):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-video-logo-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(AzureVideoLogoDetector())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'Shell' in predictions

    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_category_detection(self, get_vid_patch, store_patch, store_blob_patch, t_path,
                                p_path, c_path):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-video-category-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(AzureVideoCategoryDetector())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'indoor_' in predictions

    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_explicit_detection(self, get_vid_patch, store_patch, store_blob_patch, t_path,
                                p_path, c_path):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-video-explicit-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(AzureVideoExplicitContentDetector())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'racy' in predictions

    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_face_detection(self, get_vid_patch, store_patch, store_blob_patch, t_path, p_path,
                            c_path):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.azure-video-face-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(AzureVideoFaceDetector())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'Male' in predictions

    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    @patch("zmlp_analysis.azure.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.azure.video.proxy.get_video_proxy')
    def test_image_text_detection(self, get_vid_patch, store_patch, store_blob_patch, t_path,
                                  p_path, c_path):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'azure-video-text-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(AzureVideoTextDetector())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_analysis(namespace)
        assert '6050 6400 F N NIRVANA NO OUTLET PASEO PL STOP TAMAYO' in analysis['content']
