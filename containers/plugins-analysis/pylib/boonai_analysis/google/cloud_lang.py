import re

import backoff
from google.api_core.exceptions import ResourceExhausted
from google.cloud import language

from boonflow import Argument, AssetProcessor

from .gcp_client import initialize_gcp_client


class CloudNaturalLanguageProcessor(AssetProcessor):
    """Use Google Cloud Natural Language API to analyze a text field in the metadata."""

    tool_tips = {'field': 'Metadata field of the asset to submit to the Cloud Natural Language '
                          'API.'}

    def __init__(self):
        super(CloudNaturalLanguageProcessor, self).__init__()
        self.add_arg(Argument('field', 'str', default='media.dialog',
                              toolTip=self.tool_tips['field']))
        self.client = None

    def init(self):
        super(CloudNaturalLanguageProcessor, self).init()
        self.client = initialize_gcp_client(language.LanguageServiceClient)

    def flatten_content(self, content):
        """Recursively flattens list(s) of strings into a single space-delimited string.

        Args:
            content (list or str): List of strings to flatten.

        Returns:
            str: Flattened string of all content.

        """
        flat = ''
        if type(content) == list:
            for elem in content:
                flat += ' ' + self.flatten_content(elem)
            return flat
        if type(content) == str or type(content) == str:
            return content

    def process(self, frame):
        asset = frame.asset
        content = asset.get_attr(self.arg_value('field'))
        self.logger.info('Content: {}'.format(content))
        if content is None:
            self.logger.info('Bailing, no content found')
            return
        content = self.flatten_content(content)

        # Get rid of CC parentheticals
        content = re.sub(r'\[.*?\]', '', content)

        self.logger.info('Content: {}'.format(content))
        document = language.types.Document(content=content,
                                           type=language.enums.Document.Type.PLAIN_TEXT)
        result = self._analyze_entities(document)
        entities = []
        for entity in result.entities:
            entities.append(entity.name)
        if not entities:
            self.logger.info('Bailing, no entities found')
            return
        self.logger.info('Entities: {}'.format(entities))
        asset.add_analysis('google.languageEntities.keywords', entities)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _analyze_entities(self, document):
        """Wraps call to Cloud Natural Language API in exponential backoff decorator."""
        return self.client.analyze_entities(document)
