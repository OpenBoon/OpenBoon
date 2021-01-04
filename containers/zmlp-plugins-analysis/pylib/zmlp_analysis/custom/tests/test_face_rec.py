from unittest.mock import patch

from zmlp.app import ModelApp
from zmlp.entity import Model
from zmlp_analysis.custom import KnnFaceRecognitionClassifier
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_prediction_labels, zorroa_test_path


class KnnFaceRecognitionClassifierTests(PluginUnitTestCase):

    @patch.object(ModelApp, 'get_model')
    @patch.object(file_storage.models, "install_model")
    def test_process(self, localize_patch, get_model_patch):
        localize_patch.return_value = zorroa_test_path('training')
        get_model_patch.return_value = Model({
            'id': '12345',
            'modelId': '12345',
            'type': "ZVI_FACE_RECOGNITION",
            'fileId': 'models/foo/bar/12345',
            'name': "foo",
            'moduleName': 'foo'
        })

        asset = TestAsset()
        asset.set_attr("analysis.zvi-face-detection.predictions", [
            {
                "label": "face0",
                "score": 0.1,
                "simhash": "AAAAAAAA",
                "bbox": [0, 0, 1, 1]
            },
            {
                "label": "face1",
                "score": 0.1,
                "simhash": "00000000",
                "bbox": [0, 0, 1, 1]
            }
        ])
        frame = Frame(asset)
        processor = self.init_processor(KnnFaceRecognitionClassifier(), {"sensitivity": 100})
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = frame.asset.get_attr('analysis.foo')
        print(analysis)
        assert 'Gandalf' in get_prediction_labels(analysis)
        assert 'Unrecognized' in get_prediction_labels(analysis)
