import logging
from unittest.mock import patch
import pytest

from zmlp_analysis.google.cloud_lang import CloudNaturalLanguageProcessor
from zmlpsdk import Frame, ZmlpFatalProcessorException
from zmlpsdk.testing import PluginUnitTestCase, TestAsset

logging.basicConfig()


class TestCloudNaturalLanguageProcessor(PluginUnitTestCase):

    clp_namespace = 'analysis.gcp-natural-language'

    def test_flatten_content(self):
        nl = CloudNaturalLanguageProcessor()

        test_list = ['this', 'is', 'a', 'test']
        result = nl.flatten_content(test_list)
        assert result == 'this is a test'

        test_str = 'this is a test'
        result = nl.flatten_content(test_str)
        assert result == 'this is a test'

        with pytest.raises(ZmlpFatalProcessorException):
            nl.flatten_content(1)

    def test_remove_parentheticals(self):
        nl = CloudNaturalLanguageProcessor()

        test_str = '[this is a test] not between brackets [new string test]'
        result = nl.remove_parentheticals(test_str)

        assert result == ' not between brackets '

    @patch("zmlp_analysis.google.cloud_lang.language.LanguageServiceClient")
    def test_process(self, client_patch):
        client_patch.return_value = MockLanguageServiceClientPatch()

        test_field = {'media.dialog': "Big Boi cooler than a polar bear's toenails"}
        test_asset = TestAsset(attrs=test_field)
        frame = Frame(test_asset)

        processor = self.init_processor(CloudNaturalLanguageProcessor())
        processor.process(frame)

        results = test_asset.get_attr(self.clp_namespace)

        for result in results['predictions']:
            assert result['label'] == 'Big Boi'
            assert result['score'] == 0.99
            assert result['type'] == 'PERSON'


class MockLanguageServiceClientPatch:

    def analyze_entities(self, document=None):
        return MockAnalyzeEntitiesResponse()


class MockAnalyzeEntitiesResponse:
    @property
    def entities(self):
        return [MockEntities()]


class MockEntities:
    @property
    def name(self):
        return 'Big Boi'

    @property
    def salience(self):
        return 0.99

    @property
    def type(self):
        return 'PERSON'
