import re

import backoff
from google.api_core.exceptions import ResourceExhausted
from google.cloud import language

from zmlpsdk import Argument, AssetProcessor, ZmlpFatalProcessorException
from zmlpsdk.analysis import LabelDetectionAnalysis

from .gcp_client import initialize_gcp_client


class CloudNaturalLanguageProcessor(AssetProcessor):
    """Use Google Cloud Natural Language API to analyze a text field in the metadata."""

    tool_tips = {'field': 'Metadata field of the asset to submit to the Cloud Natural Language '
                          'API.'}

    namespace = 'gcp-natural-language'

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
        if isinstance(content, list):
            return ' '.join(content)
        elif isinstance(content, str):
            return content
        else:
            raise ZmlpFatalProcessorException('input must be list or str')

    def remove_parentheticals(self, content):
        """ Get rid of CC parentheticals

        Args:
            content (str): content string

        Returns:
            same string with parentheticals removed
        """
        return re.sub(r'\[.*?\]', '', content)

    def process(self, frame):
        asset = frame.asset
        analysis = LabelDetectionAnalysis()

        # get content
        content = asset.get_attr(self.arg_value('field'))
        self.logger.info('Content: {}'.format(content))
        if content is None:
            self.logger.info('Bailing, no content found')
            return

        # pre-processing
        content = self.flatten_content(content)
        content = self.remove_parentheticals(content)
        self.logger.debug('Content: {}'.format(content))

        # analyze entities
        document = language.types.Document(content=content,
                                           type=language.enums.Document.Type.PLAIN_TEXT)
        response = self._analyze_entities(document)
        results = response.entities

        if not results:
            raise ZmlpFatalProcessorException('Bailing, no entities found')

        # add results for analysis
        entities = []
        for entity in results:
            entities.append(
                (entity.name, entity.salience, entity.type)
            )
        self.logger.debug('Entities: {}'.format(entities))

        [analysis.add_label_and_score(ls[0], ls[1], type=ls[2]) for ls in entities]
        asset.add_analysis(self.namespace, analysis)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _analyze_entities(self, document):
        """Wraps call to Cloud Natural Language API in exponential backoff decorator."""
        return self.client.analyze_entities(document)
