import logging
import os
import shutil
from unittest.mock import patch

from boonsdk.app import ModelApp
from boonsdk.entity import Model, StoredFile, AnalysisModule
from boonsdk_train.tf2 import TensorflowTransferLearningTrainer
from boonflow import file_storage, Frame
from boonflow.testing import PluginUnitTestCase, TestAsset

import tensorflow
tensorflow.config.set_visible_devices([], 'GPU')

print("Num GPUs Available: ", len(tensorflow.config.experimental.list_physical_devices('GPU')))

logging.basicConfig()

daisy = [
    TestAsset('flowers/daisy/5547758_eea9edfd54_n.jpg', id='5547758_eea9edfd54_n'),
    TestAsset('flowers/daisy/5673551_01d1ea993e_n.jpg', id='5673551_01d1ea993e_n'),
    TestAsset('flowers/daisy/5673551_01d1ea993e_n.jpg', id='5673551_01d1ea993e_n'),
    TestAsset('flowers/daisy/5794835_d15905c7c8_n.jpg', id='5794835_d15905c7c8_n'),
    TestAsset('flowers/daisy/5794839_200acd910c_n.jpg', id='5794839_200acd910c_n'),
    TestAsset('flowers/daisy/11642632_1e7627a2cc.jpg', id='11642632_1e7627a2cc'),
]

rose = [
    TestAsset('flowers/roses/102501987_3cdb8e5394_n.jpg', id='102501987_3cdb8e5394_n'),
    TestAsset('flowers/roses/110472418_87b6a3aa98_m.jpg', id='110472418_87b6a3aa98_m'),
    TestAsset('flowers/roses/12240303_80d87f77a3_n.jpg', id='12240303_80d87f77a3_n'),
    TestAsset('flowers/roses/22679076_bdb4c24401_m.jpg', id='22679076_bdb4c24401_m'),
    TestAsset('flowers/roses/24781114_bc83aa811e_n.jpg', id='24781114_bc83aa811e_n'),
    TestAsset('flowers/roses/99383371_37a5ac12a3_n.jpg', id='99383371_37a5ac12a3_n')
]

assets = daisy + rose


def download_images(ds_id, style, dst_dir):
    os.makedirs(dst_dir + '/train/daisy/', exist_ok=True)
    os.makedirs(dst_dir + '/train/roses/', exist_ok=True)

    for asset in daisy:
        print(asset.get_attr('source.path'))
        shutil.copy(os.path.dirname(__file__) + "/" + asset.get_attr('source.path'),
                    dst_dir + '/train/daisy/')

    for asset in rose:
        print(asset.get_attr('source.path'))
        shutil.copy(os.path.dirname(__file__) + "/" + asset.get_attr('source.path'),
                    dst_dir + '/train/roses/')


class TensorflowTransferLearningTrainerTests(PluginUnitTestCase):

    model_id = "model-id-12345"

    def prep_assets(self):
        for asset in assets:
            asset.set_attr('files', [
                {
                    'id': asset.id + '.jpg',
                    'mimetype': 'image/jpeg',
                    'category': 'proxy',
                    'attrs': {
                        'width': 100,
                        'height': 100,
                        'path': asset.get_attr('source.path')
                    }
                }
            ])

            asset.set_attr('labels', [
                {
                    'modelId': self.model_id,
                    'label': asset.uri.split('/')[1]
                }
            ])

        return assets

    @patch.object(file_storage.models, 'publish_model')
    @patch.object(ModelApp, 'get_model')
    @patch.object(ModelApp, 'get_label_counts')
    @patch('boonai_train.tf2.train.download_labeled_images', download_images)
    @patch.object(file_storage.models, 'save_model')
    @patch.object(file_storage.projects, 'store_file')
    def test_process(self, store_plot_patch, upload_patch, labels_patch, model_patch, pub_patch):
        self.prep_assets()
        name = 'zvi-flowers-label-detection'
        store_plot_patch.side_effect = [{}, {}]
        pub_patch.return_value = AnalysisModule({
            'id': "12345",
            'name': name
        })
        model_patch.return_value = Model({
            'id': self.model_id,
            'type': "ZVI_LABEL_DETECTION",
            'fileId': 'models/{}/foo/bar'.format(self.model_id),
            'moduleName': name,
            'name': name
        })
        labels_patch.return_value = {
            'roses': 6,
            'daisy': 6
        }
        upload_patch.return_value = StoredFile({'id': '12345'})

        args = {
            'model_id': self.model_id,
            'epochs': 5,
            'validation_split': 0.3
        }

        processor = TensorflowTransferLearningTrainer()
        processor.min_examples = 6
        processor = self.init_processor(processor, args)
        processor.process(Frame(TestAsset()))
