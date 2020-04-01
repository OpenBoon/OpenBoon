import os
from unittest.mock import patch

from zmlp import ZmlpClient
from zmlp_analysis.clarifai.processors import ClarifaiPredictGeneralProcessor
from zmlpsdk import Frame
from zmlpsdk.proxy import store_asset_proxy
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_data, \
    TestAsset, get_prediction_labels


class MockClarifaiApp(object):
    """
    Class to handle clarifai responses.
    """
    def __init__(self):

        class GeneralModel(object):
            def predict_by_filename(self, filename):
                with open(os.path.dirname(__file__) + "/clarifai.rsp") as fp:
                    return eval(fp.read())

        class PublicModels(object):
            def __init__(self):
                self.general_model = GeneralModel()

        self.public_models = PublicModels()


class ClarifaiPredictGeneralProcessorTests(PluginUnitTestCase):

    def setUp(self):
        self.image_path = zorroa_test_data('images/detect/dogbike.jpg')
        self.frame = Frame(TestAsset(self.image_path))

    @patch.object(ZmlpClient, 'upload_file')
    @patch('zmlp_analysis.clarifai.processors.get_clarifai_app')
    def test_process(self, get_app_patch, upload_patch):
        get_app_patch.return_value = MockClarifaiApp()
        upload_patch.return_value = {
            'id': 'foo/bar/proxy/proxy_200x200.jpg',
            'name': 'proxy_200x200.jpg',
            'category': 'proxy',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 576,
                'height': 1024
            }
        }

        store_asset_proxy(self.frame.asset, self.image_path, (576, 1024))
        processor = self.init_processor(ClarifaiPredictGeneralProcessor(), {})
        processor.process(self.frame)

        print(self.frame.asset.document)

        analysis = self.frame.asset.get_attr('analysis.clarifai-predict-general')
        assert 'wheel' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 20 == analysis['count']
