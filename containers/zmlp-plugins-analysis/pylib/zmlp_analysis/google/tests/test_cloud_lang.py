import logging
from unittest.mock import patch
import pytest

from zmlp_analysis.google.cloud_lang import CloudNaturalLanguageProcessor, \
    CloudNaturalLanguageSentimentProcessor
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset

logging.basicConfig()


class TestCloudNaturalLanguageProcessor(PluginUnitTestCase):

    clp_namespace = 'analysis.gcp-natural-language'

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


class TestCloudNaturalLanguageSentimentProcessor(PluginUnitTestCase):

    clp_namespace = 'analysis.gcp-sentiment-analysis'

    @patch("zmlp_analysis.google.cloud_lang.language.LanguageServiceClient")
    def test_process(self, client_patch):
        client_patch.return_value = MockLanguageServiceClientPatch()
        media_dialog = "Big Boi cooler than a polar bear's toenails"

        test_field = {'media.dialog': media_dialog}
        test_asset = TestAsset(attrs=test_field)
        frame = Frame(test_asset)

        processor = self.init_processor(CloudNaturalLanguageSentimentProcessor())
        processor.process(frame)

        results = test_asset.get_attr(self.clp_namespace)

        for result in results['predictions']:
            assert result['label'] == media_dialog or result['label'] == 'overall_sentiment'
            assert result['score'] == 0.9
            assert result['magnitude'] == 1.0


class MockLanguageServiceClientPatch:

    def analyze_entities(self, document=None):
        return MockAnalyzeEntitiesResponse()

    def analyze_sentiment(self, document=None):
        return MockAnalyzeSentimentReponse()


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


class MockAnalyzeSentimentReponse:
    @property
    def document_sentiment(self):
        return MockText()

    @property
    def sentences(self):
        return [MockSentenceSentiment()]


class MockSentenceSentiment:
    @property
    def text(self):
        return MockText()

    @property
    def sentiment(self):
        return MockText()


class MockText:
    @property
    def content(self):
        return "Big Boi cooler than a polar bear's toenails"

    @property
    def score(self):
        return 0.9

    @property
    def magnitude(self):
        return 1.0
