import os
from unittest.mock import patch

from zmlp_analysis.clarifai.labels import ClarifaiPredictGeneralProcessor
from zmlpsdk import Frame
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

    @patch('zmlpsdk.proxy.get_proxy_level_path')
    @patch('zmlp_analysis.clarifai.labels.get_clarifai_app')
    def test_process(self, get_app_patch, proxy_path_patch):
        get_app_patch.return_value = MockClarifaiApp()
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(ClarifaiPredictGeneralProcessor(), {})
        processor.process(self.frame)

        analysis = self.frame.asset.get_attr('analysis.clarifai-predict-general')
        assert 'wheel' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 20 == analysis['count']
