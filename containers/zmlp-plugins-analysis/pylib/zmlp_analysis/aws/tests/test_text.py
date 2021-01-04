from unittest.mock import patch

from pytest import approx

from zmlp_analysis.aws import RekognitionTextDetection
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_prediction_labels


class RekognitionLabelDetectionProcessorTests(PluginUnitTestCase):
    model_id = "model-id-12345"

    @patch("zmlp_analysis.aws.text.get_proxy_level_path")
    @patch('zmlp_analysis.aws.util.AwsEnv.rekognition')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockAWSClient()

        flower_paths = zorroa_test_path("images/set08/meme.jpg")
        proxy_patch.return_value = flower_paths
        frame = Frame(TestAsset(flower_paths))

        args = expected_results[0][0]

        processor = self.init_processor(RekognitionTextDetection(), args)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = frame.asset.get_analysis('aws-text-detection')
        assert 'HEY' in get_prediction_labels(analysis)
        assert 'GIRL,' in get_prediction_labels(analysis)
        assert analysis['count'] == 2


expected_results = [
    (
        {"model_id": "model-id-12345"},
        [
            ('HEY', approx(0.9992, 0.0001)),
            ('GIRL,', approx(0.9992, 0.0001))
        ]
    )
]


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
