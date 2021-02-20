import logging
from unittest.mock import patch

from boonsdk.app import ModelApp, AssetApp
from boonsdk.entity import Model, StoredFile, AnalysisModule, Asset
from boonai_train.face_rec import KnnFaceRecognitionTrainer
from boonflow import Frame, file_storage
from boonflow.testing import PluginUnitTestCase, TestAsset

logging.basicConfig()

model_id = "model-id-12345"


class KnnFaceRecognitionTrainerTests(PluginUnitTestCase):

    @patch.object(file_storage.models, 'publish_model')
    @patch.object(ModelApp, 'get_model')
    @patch.object(AssetApp, 'scroll_search')
    @patch.object(file_storage.projects, "store_file_by_id")
    def test_process(self, upload_patch, search_patch, model_patch, pub_patch):

        name = 'custom-flowers-label-detection-tf2-xfer-mobilenet2'
        pub_patch.return_value = AnalysisModule({
            'id': "12345",
            'name': name
        })
        model_patch.return_value = Model({
            'id': model_id,
            'type': "FACE_RECOGNITION_KNN",
            'fileId': 'models/{}/foo/bar'.format(model_id),
            'name': name
        })
        search_patch.return_value = mock_search_result
        upload_patch.return_value = StoredFile({"id": "12345"})

        args = {
            'model_id': model_id
        }

        processor = self.init_processor(KnnFaceRecognitionTrainer(), args)
        processor.process(Frame(TestAsset()))


mock_search_result = [
    Asset.from_hit({
        '_id': 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
        '_score': 0.2876821,
        '_source': {
            'source': {
                'path': 'https://i.imgur.com/SSN26nN.jpg'
            },
            "labels": [
                {
                    "modelId": model_id,
                    "label": "Gandalf",
                    "bbox": [0, 0, 1, 1],
                    "simhash": "AAAAAAAA"
                },
                {
                    "modelId": model_id,
                    "label": "Bilbo",
                    "bbox": [0.5, 0.5, 1, 1],
                    "simhash": "CCCCCCCC"
                }
            ]
        }
    }),
    Asset.from_hit({
        '_index': 'litvqrkus86sna2w',
        '_type': 'asset',
        '_id': 'aabbccddec48n1q1fginVMV5yllhRRGx2WKyKLjDphg',
        '_score': 0.2876821,
        '_source': {
            'source': {
                'path': 'https://i.imgur.com/foo.jpg'
            },
            "labels": [
                {
                    "modelId": model_id,
                    "label": "Glion",
                    "bbox": [0, 0, 1, 1],
                    "simhash": "BBBBBBBB"
                },
                {
                    "modelId": model_id,
                    "label": "Frodo",
                    "bbox": [0.5, 0.5, 1, 1],
                    "simhash": "DDDDDDDD"
                }
            ]
        }
    })
]
