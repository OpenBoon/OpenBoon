from unittest.mock import patch

from zmlp.app import ModelApp
from zmlp.entity import Model
from zmlp_analysis.custom import KnnLabelDetectionClassifier
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path


class KnnLabelDetectionClassifierTests(PluginUnitTestCase):

    @patch.object(ModelApp, 'get_model')
    @patch.object(file_storage.projects, 'localize_file')
    def test_process(self, localize_patch, get_model_patch):
        localize_patch.return_value = zorroa_test_path('training/model_knn.zip')
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
        self.mock_record_analysis_metric.assert_called_once()

        label = frame.asset.get_attr('analysis.foo.label')
        assert label == 'Gandalf'
