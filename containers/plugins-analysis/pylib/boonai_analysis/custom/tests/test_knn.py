import os
import shutil
from unittest.mock import patch

from boonsdk.app import ModelApp
from boonsdk.entity import Model
from boonai_analysis.custom import KnnLabelDetectionClassifier
from boonai_analysis.utils.simengine import SimilarityEngine
from boonflow import Frame, file_storage
from boonflow.testing import PluginUnitTestCase, TestAsset, test_path, get_prediction_labels


class KnnLabelDetectionClassifierTests(PluginUnitTestCase):

    def setUp(self):
        if not os.path.exists("/models"):
            SimilarityEngine.default_model_path = test_path("models/resnet-152")

        try:
            shutil.rmtree("/tmp/model-cache")
        except FileNotFoundError:
            print("Didn't clear out model cache, this is ok.")

    @patch.object(file_storage.models, 'model_exists', return_value=False)
    @patch.object(ModelApp, 'get_model')
    @patch.object(file_storage.projects, 'localize_file')
    def test_process_image(self, localize_patch, get_model_patch, _):
        localize_patch.return_value = test_path('training/model_knn.zip')
        get_model_patch.return_value = Model({
            'id': '12345',
            'type': 'TF_CLASSIFIER',
            'fileId': 'models/foo/knn/12345',
            'name': 'foo',
            'moduleName': 'foo'
        })

        asset = TestAsset()
        asset.set_attr('analysis.boonai-image-similarity.simhash', 'AAAAAAAA')
        frame = Frame(asset)
        processor = self.init_processor(KnnLabelDetectionClassifier(), {'tag': 'latest'})
        processor.process(frame)

        analysis = frame.asset.get_analysis('foo')
        predictions = get_prediction_labels(analysis)
        assert 'Gandalf' in predictions

    @patch.object(file_storage.models, 'model_exists', return_value=False)
    @patch("boonai_analysis.custom.knn.save_timeline", return_value={})
    @patch.object(ModelApp, 'get_model')
    @patch.object(file_storage.projects, 'localize_file')
    @patch('boonai_analysis.custom.knn.get_video_proxy')
    def test_process_video(self, proxy_patch, localize_patch, get_model_patch, tl_patch, _):
        proxy_patch.return_value = test_path('video/cats2.mp4')
        localize_patch.return_value = test_path('training/knn_pets.zip')
        get_model_patch.return_value = Model({
            'id': '12345',
            'type': 'TF_CLASSIFIER',
            'fileId': 'models/foo/knn/abc123a',
            'name': 'foo',
            'moduleName': 'foo'
        })

        asset = TestAsset()
        asset.set_attr('media.type', 'video')
        asset.set_attr('media.length', 805)
        frame = Frame(asset)

        processor = self.init_processor(KnnLabelDetectionClassifier(), {'tag': 'latest'})
        processor.process(frame)

        analysis = frame.asset.get_analysis('foo')
        predictions = get_prediction_labels(analysis)
        assert 'cat' in predictions
        assert 'Unrecognized' in predictions

        timeline = tl_patch.call_args_list[0][0][1]
        jtl = timeline.for_json()
        assert jtl['tracks'][0]['name'] == 'Unrecognized'
