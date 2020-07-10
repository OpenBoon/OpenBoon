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
            'name': "foo"
        })

        asset = TestAsset()
        asset.set_attr("analysis.zvi-face-detection.predictions", [
            {
                "label": "face0",
                "score": 0.1,
                "simhash": "AAAAAAAA"
            },
            {
                "label": "face1",
                "score": 0.1,
                "simhash": "00000000"
            }
        ])
        frame = Frame(asset)
        processor = self.init_processor(KnnFaceRecognitionClassifier(), {})
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.zvi-face-detection')
        assert 'Gandalf' in get_prediction_labels(analysis)
