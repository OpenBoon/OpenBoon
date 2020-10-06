from unittest.mock import patch

from zmlp_analysis.google.cloud_doc import CloudDocumentProcessor
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_prediction_labels

IRR_PDF = zorroa_test_path('office/irr.pdf')


class MockDocClient:
    def __init__(self, *args, **kwargs):
        pass

    def process_document(self, request=None):
        return MockDocument()


class CloudDLPDetectEntitiesTests(PluginUnitTestCase):

    @patch('zmlp_analysis.google.cloud_doc.initialize_gcp_client', side_effect=MockDocClient)
    @patch('zmlp_analysis.google.cloud_doc.get_gcp_project_id')
    def test_process(self, project_id_patch, _):
        namespace = 'gcp-document-detection'
        project_id_patch.return_value = 'zorroa-poc-dev'

        asset = TestAsset(IRR_PDF)
        frame = Frame(asset)
        processor = self.init_processor(CloudDocumentProcessor())

        processor.process(frame)
        analysis = frame.asset.get_analysis(namespace)

        preds = analysis['predictions'][0]
        assert preds['label'] == 'FakeDoc M.'
        assert preds['score'] == 1.0
        assert preds['field_value'] == 'FakeDoc M.'
        assert preds['field_value_score'] == 0.9999823570251465


class MockDocument:
    @property
    def pages(self):
        return [MockPages()]

    @property
    def text(self):
        return "FakeDoc M.D.HEALTH INTAKE FORMPlease fill out the questionnaire carefully."

class MockPages:
    @property
    def form_fields(self):
        return [MockFF()]


class MockFF:
    @property
    def field_name(self):
        return MockFields()

    @property
    def field_value(self):
        return MockFields()


class MockFields:
    @property
    def text_anchor(self):
        return MockTextAnchor()

    @property
    def confidence(self):
        return 0.9999823570251465


class MockTextAnchor:
    @property
    def text_segments(self):
        return [MockIndices()]

    @property
    def confidence(self):
        return 0.9999823570251465


class MockIndices:
    @property
    def start_index(self):
        return 0

    @property
    def end_index(self):
        return 10
