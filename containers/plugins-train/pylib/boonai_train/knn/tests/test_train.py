import logging
from unittest.mock import patch

from boonsdk.app import ModelApp, AssetApp
from boonsdk.entity import Model, StoredFile, AnalysisModule, Asset
from boonai_train.knn import KnnLabelDetectionTrainer
from boonflow import Frame, file_storage
from boonflow.testing import PluginUnitTestCase, TestAsset

logging.basicConfig()

model_id = "model-id-12345"


class KnnLabelDetectionTrainerTests(PluginUnitTestCase):

    @patch.object(file_storage.models, 'publish_model')
    @patch.object(ModelApp, 'get_model')
    @patch.object(AssetApp, 'scroll_search')
    @patch.object(file_storage.projects, "store_file_by_id")
    def test_process(self, upload_patch, search_patch, model_patch, pub_patch):

        name = 'custom-knn-labels-detect'
        pub_patch.return_value = AnalysisModule({
            'id': "12345",
            'name': name
        })
        model_patch.return_value = Model({
            'id': model_id,
            'type': "KNN_CLASSIFIER",
            'fileId': 'models/{}/foo/bar'.format(model_id),
            'name': name,
            'datasetId': '12345'
        })
        search_patch.return_value = mock_search_result
        upload_patch.return_value = StoredFile({"id": "12345"})

        args = {
            'model_id': model_id,
            'tag': 'latest',
            'post_action': 'none'
        }

        processor = self.init_processor(KnnLabelDetectionTrainer(), args)
        processor.process(Frame(TestAsset()))


mock_search_result = [
    Asset.from_hit({
        '_id': 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
        '_score': 0.2876821,
        '_source': {
            'source': {
                'path': 'https://i.imgur.com/SSN26nN.jpg'
            },
            "analysis": {"boonai-image-similarity": {"simhash": "AAAAAAAA"}},
            "labels": [
                {
                    "datasetId": '12345',
                    "label": "Gandalf",
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
            "analysis": {"boonai-image-similarity": {"simhash": "BBBBBBBB"}},
            "labels": [
                {
                    "datasetId": '12345',
                    "label": "Glion",
                }
            ]
        }
    }),
    Asset.from_hit({
        '_index': 'litvqrkus86sna2w',
        '_type': 'asset',
        '_id': 'aabbccddec48n1q1fgenVMV5yllhRRGx2WKyKLjDphg',
        '_score': 0.2876821,
        '_source': {
            'source': {
                'path': 'https://i.imgur.com/foo.jpg'
            },
            "labels": [
                {
                    "datasetId": "12345",
                    "label": "Glion",
                }
            ]
        }
    })
]
