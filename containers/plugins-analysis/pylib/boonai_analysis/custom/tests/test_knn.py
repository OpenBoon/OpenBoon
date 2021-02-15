from unittest.mock import patch

from boonsdk.app import ModelApp
from boonsdk.entity import Model
from boonai_analysis.custom import KnnLabelDetectionClassifier
from boonflow import Frame, file_storage
from boonflow.testing import PluginUnitTestCase, TestAsset, test_path


class KnnLabelDetectionClassifierTests(PluginUnitTestCase):

    @patch.object(ModelApp, 'get_model')
    @patch.object(file_storage.projects, 'localize_file')
    def test_process(self, localize_patch, get_model_patch):
        localize_patch.return_value = test_path('training/model_knn.zip')
        get_model_patch.return_value = Model({
            'id': '12345',
            'type': 'ZVI_LABEL_DETECTION',
            'fileId': 'models/foo/knn/12345',
            'name': 'foo',
            'moduleName': 'foo'
        })

        asset = TestAsset()
        asset.set_attr('analysis.zvi-image-similarity.simhash', 'AAAAAAAA')
        frame = Frame(asset)
        processor = self.init_processor(KnnLabelDetectionClassifier(), {})
        processor.process(frame)

        label = frame.asset.get_attr('analysis.foo.label')
        assert label == 'Gandalf'
