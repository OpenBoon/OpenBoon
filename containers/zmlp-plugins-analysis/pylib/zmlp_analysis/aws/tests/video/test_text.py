import os

from unittest.mock import patch

from .conftest import MockS3Client
from zmlp_analysis.aws.videos.text import RekognitionVideoTextDetection
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_prediction_labels, \
    zorroa_test_path, get_mock_stored_file
from zmlpsdk import file_storage

rek_patch_path = 'zmlp_analysis.aws.util.AwsEnv.rekognition'
s3_patch_path = 'zmlp_analysis.aws.util.AwsEnv.s3'

VID_MP4 = "video/ted_talk.mp4"


class MockAWSClient:

    def detect_text(self, Image=None):
        return {
            'TextDetections': [
                {
                    'DetectedText': 'HEY',
                    'Type': 'LINE',
                    'Id': 0,
                    'Confidence': 99.9248046875,
                    'Geometry': {
                        'BoundingBox': {
                            'Width': 0.1433333307504654,
                            'Height': 0.08666666597127914,
                            'Left': 0.009999999776482582,
                            'Top': 0.03999999910593033
                        },
                        'Polygon': [
                            {'X': 0.009999999776482582, 'Y': 0.03999999910593033},
                            {'X': 0.15333333611488342, 'Y': 0.03999999910593033},
                            {'X': 0.15333333611488342, 'Y': 0.12666666507720947},
                            {'X': 0.009999999776482582, 'Y': 0.12666666507720947}
                        ]
                    }
                },
                {
                    'DetectedText': 'GIRL,',
                    'Type': 'LINE',
                    'Id': 1,
                    'Confidence': 99.91860961914062,
                    'Geometry': {
                        'BoundingBox': {
                            'Width': 0.20000000298023224,
                            'Height': 0.10000000149011612,
                            'Left': 0.15666666626930237,
                            'Top': 0.036666665226221085
                        },
                        'Polygon': [
                            {'X': 0.15666666626930237, 'Y': 0.036666665226221085},
                            {'X': 0.3566666543483734, 'Y': 0.036666665226221085},
                            {'X': 0.3566666543483734, 'Y': 0.1366666704416275},
                            {'X': 0.15666666626930237, 'Y': 0.1366666704416275}
                        ]
                    }
                },
            ]
        }


class RekognitionVideoTextDetectionProcessorTests(PluginUnitTestCase):

    @patch(s3_patch_path, side_effect=MockS3Client)
    def setUp(self, s3_patch):
        os.environ['ZMLP_PROJECT_ID'] = '00000000-0000-0000-0000-000000000001'
        os.environ['ZORROA_AWS_BUCKET'] = 'zorroa-unit-tests'

    @patch(s3_patch_path, side_effect=MockS3Client)
    @patch(rek_patch_path, side_effect=MockAWSClient)
    @patch("zmlp_analysis.aws.videos.text.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.videos.text.proxy.get_video_proxy')
    def test_text_detection(self, get_vid_patch, store_patch, store_blob_patch, _, __, ___):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.aws-text-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(RekognitionVideoTextDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'HEY' in predictions
        assert 'GIRL,' in predictions
        assert analysis['count'] == 2
