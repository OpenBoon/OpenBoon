import os
import random
import logging
from unittest.mock import patch

from zmlp import ZmlpClient
from zmlp.app import ModelApp
from zmlp.entity import Model, PipelineMod
from zmlpsdk.storage import file_storage
from zmlpsdk.testing import PluginUnitTestCase
from zmlpsdk.training import id_generator
from zmlp_train.trainClassifier import run

logging.basicConfig()


class TrainClassifierTests(PluginUnitTestCase):
    ds_id = "ds-id-12345"
    model_id = "model-id-12345"
    base_dir = os.path.dirname(__file__)
    class_names = ["dog", "cat"]

    def setUp(self):
        # A mock search result used for asset search tests
        self.mock_search_result = {
            "took": 4,
            "timed_out": False,
            "hits": {
                "total": {"value": 2},
                "max_score": 0.2876821,
                "hits": [
                    {
                        "_id": "dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg",
                        "_source": {
                            "shash": id_generator(size=2048),
                            "label": self.class_names[random.randint(0, 1)],
                        },
                    }
                    for _ in range(100)
                ],
            },
        }

    @patch.object(ModelApp, "publish_model")
    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.projects, "localize_file")
    @patch.object(ZmlpClient, "post")
    def test_main(
        self, post_patch, file_patch, model_patch, pub_patch
    ):
        name = "custom-flowers-label-detection-tf2-xfer-mobilenet2"
        file_patch.return_value = "{}/{}.zip".format(self.base_dir, name)
        pub_patch.return_value = PipelineMod({"id": "12345", "name": name})
        post_patch.return_value = self.mock_search_result
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "dataSetId": self.ds_id,
                "type": "LABEL_DETECTION_MOBILENET2",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": name,
            }
        )

        main_args = [
            "--model_id",
            self.model_id,
            "--folder",
            "pets",
            "--attr",
            "shash",
        ]
        model_dir = run(main_args)

        assert os.path.exists(model_dir)
        assert os.path.exists(os.path.join(model_dir, "assets"))
        assert os.path.exists(os.path.join(model_dir, "_labels.txt"))
