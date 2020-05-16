import logging
import os
import shutil
from unittest.mock import patch
import zipfile

from tensorflow.keras.models import load_model
from tensorflow.keras.applications import mobilenet_v2 as mobilenet_v2

from zmlpsdk.storage import FileStorage
from zmlp.app import DataSetApp, ModelApp
from zmlp.entity import Model, StoredFile, PipelineMod
from zmlp_train.tf2 import TensorflowTransferLearningClassifier
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, \
    TestAsset, get_prediction_labels

logging.basicConfig()


class TensorflowTransferLearningClassifierTests(PluginUnitTestCase):
    ds_id = "ds-id-12345"
    model_id = "model-id-12345"

    @patch.object(ModelApp, 'get_model')
    @patch.object(FileStorage, 'localize_file')
    @patch('zmlp_train.tf2.classify.get_proxy_level_path')
    def test_predict(self, proxy_patch, file_patch, model_patch):
        name = 'custom-flowers-label-detection-tf2-xfer-mobilenet2'
        file_patch.return_value = \
            '/Users/ryangaspar/zorroa/zmlp/containers/zmlp-plugins-train/' \
            'pylib/zmlp_train/tf2/tests/{}.zip'.format(name)
        model_patch.return_value = Model({
            'id': self.model_id,
            'dataSetId': self.ds_id,
            'type': "LABEL_DETECTION_MOBILENET2",
            'fileId': 'models/{}/foo/bar'.format(self.model_id),
            'name': name
        })

        args = {
            'model_id': self.model_id,
        }

        flower_paths = [
            '/Users/ryangaspar/zorroa/zmlp/containers/zmlp-plugins-train/'
            'pylib/zmlp_train/tf2/tests/flowers/test_daisy.jpg',
            '/Users/ryangaspar/zorroa/zmlp/containers/zmlp-plugins-train/'
            'pylib/zmlp_train/tf2/tests/flowers/test_rose.png'
        ]
        for paths in flower_paths:
            proxy_patch.return_value = paths
            frame = Frame(TestAsset(paths))

            processor = self.init_processor(
                TensorflowTransferLearningClassifier(), args)
            processor.process(frame)
