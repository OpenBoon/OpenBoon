import backoff
from google.api_core.exceptions import ResourceExhausted
from google.cloud import language

from zmlpsdk import Argument, AssetProcessor, ZmlpFatalProcessorException
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlp_analysis.utils.preprocessing import flatten_content, remove_parentheticals

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
        content = flatten_content(content)
        content = remove_parentheticals(content)
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


class CloudNaturalLanguageSentimentProcessor(AssetProcessor):
    """Use Google Cloud Natural Language API to analyze a text field in the metadata."""

    tool_tips = {'field': 'Metadata field of the asset to submit to the Cloud Natural Language '
                          'API.'}

    namespace = 'gcp-sentiment-analysis'

    def __init__(self):
        super(CloudNaturalLanguageSentimentProcessor, self).__init__()
        self.add_arg(Argument('field', 'str', default='media.dialog',
                              toolTip=self.tool_tips['field']))
        self.client = None

    def init(self):
        super(CloudNaturalLanguageSentimentProcessor, self).init()
        self.client = initialize_gcp_client(language.LanguageServiceClient)

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
        content = flatten_content(content)
        content = remove_parentheticals(content)
        self.logger.debug('Content: {}'.format(content))

        # analyze entities
        document = language.types.Document(content=content,
                                           type=language.enums.Document.Type.PLAIN_TEXT)
        response = self._analyze_sentiment(document)

        if not response:
            raise ZmlpFatalProcessorException('Bailing, no sentiment found')

        # add results for analysis
        document_analysis = (
            'overall_sentiment',
            response.document_sentiment.score,
            response.document_sentiment.magnitude
        )
        entities = [document_analysis]
        for sentence in response.sentences:
            entities.append(
                (sentence.text.content, sentence.sentiment.score, sentence.sentiment.magnitude)
            )

        [analysis.add_label_and_score(ls[0], ls[1], magnitude=ls[2]) for ls in entities]
        asset.add_analysis(self.namespace, analysis)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _analyze_sentiment(self, document):
        """Wraps call to Cloud Natural Language API in exponential backoff decorator."""
        return self.client.analyze_sentiment(document)
