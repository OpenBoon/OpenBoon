import os
import shutil
from unittest.mock import patch

from boonai_analysis.deployed.function import BoonFunctionProcessor
from boonflow.base import Frame
from boonflow.testing import PluginUnitTestCase, TestAsset, get_prediction_labels
from boonsdk.app import ModelApp
from boonsdk.entity import Model


class BoonFunctionTests(PluginUnitTestCase):
    model_id = "model-id-34568"
    base_dir = os.path.dirname(__file__)

    def setUp(self):
        try:
            shutil.rmtree("/tmp/boonai/model-cache")
        except FileNotFoundError:
            print("Didn't clear out model cache, this is ok.")

    @patch.object(ModelApp, "get_model")
    @patch.object(BoonFunctionProcessor, "predict")
    def test_boon_func_label_analysis(self, predict_patch, model_patch):
        model_patch.return_value = Model(
            {
                'id': self.model_id,
                'type': 'BOON_FUNCTION',
                'fileId': 'models/{}/foo/bar'.format(self.model_id),
                'name': 'foo',
                'moduleName': 'foo'
            }
        )
        predict_patch.return_value = {
            'analysis': {
                "__MAIN__": {
                    'type': 'labels',
                    'predictions': [
                        {
                            'label': 'cat',
                            'score': 0.99
                        }
                    ]
                },
                "caption": {
                    'section': 'caption',
                    'type': 'content',
                    'content': 'I can has cheeseburger'
                }
            },
            'custom-fields': {
                'test': '12345',
                'cat_name': 'snowball'
            }
        }

        args = {
            'model_id': self.model_id,
            'tag': 'latest',
            'endpoint': 'http://127.0.0.1:8080'
        }

        frame = Frame(TestAsset())
        processor = self.init_processor(
            BoonFunctionProcessor(), args
        )
        processor.process(frame)

        analysis = frame.asset.get_analysis('foo')
        labels = get_prediction_labels(analysis)
        assert 'cat' in labels

        content = frame.asset.get_analysis('foo-caption')
        assert content['content'] == 'I can has cheeseburger'

        assert frame.asset.get_attr('custom.test') == '12345'
        assert frame.asset.get_attr('custom.cat_name') == 'snowball'
