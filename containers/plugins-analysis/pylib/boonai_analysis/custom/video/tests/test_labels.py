import logging
import os
import shutil
from unittest.mock import patch

from boonsdk.app import ModelApp
from boonsdk.entity import Model
from boonai_analysis.custom.video import VideoTensorflowTransferLearningClassifier
from boonflow.base import Frame
from boonflow.storage import file_storage
from boonflow.testing import PluginUnitTestCase, TestAsset, test_path, get_prediction_labels

logging.basicConfig()


class VideoTensorflowTransferLearningClassifierTests(PluginUnitTestCase):
    model_id = "model-id-12345"
    base_dir = os.path.dirname(__file__)
    name = "custom-flowers-label-detection-tf2-xfer-mobilenet2"

    def setUp(self):
        try:
            shutil.rmtree("/tmp/model-cache/models_model-id-12345_foo_bar")
        except FileNotFoundError:
            print("Didn't clear out model cache, this is ok.")

        self.video_path = test_path('video/flower.mp4')
        asset = TestAsset(self.video_path)
        asset.set_attr('media.length', 15.0)
        self.frame = Frame(asset)

    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.projects, "localize_file")
    @patch("boonai_analysis.custom.video.labels.video.save_timeline", return_value={})
    @patch('boonai_analysis.custom.video.labels.proxy.get_video_proxy')
    def test_predict(self, proxy_path_patch, _, file_patch, model_patch):
        proxy_path_patch.return_value = self.video_path
        model_file = test_path("training/{}.zip".format(self.name))
        file_patch.return_value = model_file
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "BOONAI_LABEL_DETECTION",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": self.name,
                "moduleName": self.name
            }
        )

        args = {
            "model_id": self.model_id,
        }

        processor = self.init_processor(
            VideoTensorflowTransferLearningClassifier(), args
        )
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis(self.name)
        predictions = get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 1 == analysis['count']
        assert 'roses' in predictions
