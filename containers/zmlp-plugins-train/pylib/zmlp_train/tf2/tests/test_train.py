import logging
import os
import shutil
from unittest.mock import patch

from zmlp.app import DataSetApp, ModelApp
from zmlp.entity import Model, ModelType, StoredFile, PipelineMod
from zmlp_train.tf2 import TensorflowTransferLearningTrainer
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset

logging.basicConfig()

assets = [
    TestAsset("flowers/daisy/5547758_eea9edfd54_n.jpg", id="5547758_eea9edfd54_n"),
    TestAsset("flowers/daisy/5673551_01d1ea993e_n.jpg", id="5673551_01d1ea993e_n"),
    TestAsset("flowers/daisy/5673551_01d1ea993e_n.jpg", id="5673551_01d1ea993e_n"),
    TestAsset("flowers/daisy/5794835_d15905c7c8_n.jpg", id="5794835_d15905c7c8_n"),
    TestAsset("flowers/daisy/5794839_200acd910c_n.jpg", id="5794839_200acd910c_n"),
    TestAsset("flowers/daisy/11642632_1e7627a2cc.jpg", id="11642632_1e7627a2cc"),

    TestAsset("flowers/roses/102501987_3cdb8e5394_n.jpg", id="102501987_3cdb8e5394_n"),
    TestAsset("flowers/roses/110472418_87b6a3aa98_m.jpg", id="110472418_87b6a3aa98_m"),
    TestAsset("flowers/roses/12240303_80d87f77a3_n.jpg", id="12240303_80d87f77a3_n"),
    TestAsset("flowers/roses/22679076_bdb4c24401_m.jpg", id="22679076_bdb4c24401_m"),
    TestAsset("flowers/roses/24781114_bc83aa811e_n.jpg", id="24781114_bc83aa811e_n"),
    TestAsset("flowers/roses/99383371_37a5ac12a3_n.jpg", id="99383371_37a5ac12a3_n")
]


def download_dataset(ds_id, dst_dir, ratio):
    os.makedirs(dst_dir + "/set_train/daisy/", exist_ok=True)
    os.makedirs(dst_dir + "/set_test/daisy/", exist_ok=True)
    os.makedirs(dst_dir + "/set_train/roses/", exist_ok=True)
    os.makedirs(dst_dir + "/set_test/roses/", exist_ok=True)

    for asset in assets[0:3]:
        shutil.copy(os.path.dirname(__file__) + "/" + asset.get_attr('source.path'),
                    dst_dir + '/set_train/daisy/')

    for asset in assets[4:5]:
        shutil.copy(os.path.dirname(__file__) + "/" + asset.get_attr('source.path'),
                    dst_dir + '/set_test/daisy/')

    for asset in assets[6:9]:
        shutil.copy(os.path.dirname(__file__) + "/" + asset.get_attr('source.path'),
                    dst_dir + '/set_train/roses/')

    for asset in assets[10:11]:
        shutil.copy(os.path.dirname(__file__) + "/" + asset.get_attr('source.path'),
                    dst_dir + '/set_test/roses/')


class TensorflowTransferLearningTrainerTests(PluginUnitTestCase):
    ds_id = "ds-id-12345"
    model_id = "model-id-12345"

    def prep_assets(self):
        for asset in assets:
            asset.set_attr('files', [
                {
                    "id": asset.id + ".jpg",
                    "mimetype": "image/jpeg",
                    "category": "proxy",
                    "attrs": {
                        "width": 100,
                        "height": 100,
                        "path": asset.get_attr('source.path')
                    }
                }
            ])

            asset.set_attr('labels', [
                {
                    "dataSetId": self.ds_id,
                    "label": asset.uri.split("/")[1]
                }
            ])

        return assets

    @patch.object(ModelApp, 'publish_model')
    @patch.object(ModelApp, 'get_model')
    @patch.object(DataSetApp, 'get_label_counts')
    @patch("zmlp_train.tf2.train.download_dataset", download_dataset)
    @patch("zmlp_train.tf2.train.upload_model_directory")
    def test_process(self, upload_patch, labels_patch, model_patch, pub_patch):
        self.prep_assets()
        name = 'custom-flowers-label-detection-tf2-xfer-mobilenet2'
        pub_patch.return_value = PipelineMod({
            'id': "12345",
            'name': name
        })
        model_patch.return_value = Model({
            'id': self.model_id,
            'dataSetId': self.ds_id,
            'type': ModelType.TF2_XFER_MOBILENET2,
            'fileId': 'models/{}/foo/bar'.format(self.model_id),
            'name': name
        })
        labels_patch.return_value = {
            "roses": 6,
            "daisy": 6
        }
        upload_patch.return_value = StoredFile({"id": "12345"})

        args = {
            'model_id': self.model_id,
            'min_examples': 6,
            'epochs': 5
        }

        processor = self.init_processor(TensorflowTransferLearningTrainer(), args)
        processor.process(Frame(TestAsset()))
