import os
import re
import requests

from zsdk import DocumentProcessor, Argument
from zsdk.utility.jwt_util import generate_jwt_from_env
from zsdk.utility.serialization import DefaultDocumentSerializer


class FilterKeywordsProcessor(DocumentProcessor):
    """Filter a list attribute using a dictionary.
    Any keywords that are found in 'keywords' that also exist in 'dictionary' are copied
    into 'filtered_keywords'.
    """
    toolTips = {
        'original_field': 'Original set of keywords.',
        'dictionary': 'Dictionary to use to filter.',
        'target_field': 'Destination field for the filtered keywords.'
    }

    def __init__(self):
        super(FilterKeywordsProcessor, self).__init__()
        self.add_arg(Argument("original_field", "string", default="keywords.all",
                              toolTip=self.toolTips['original_field']))
        self.add_arg(Argument("dictionary", "list", default=[],
                              toolTip=self.toolTips['dictionary']))
        self.add_arg(Argument("target_field", "string", default="keywords.filtered",
                              toolTip=self.toolTips['target_field']))

    def _process(self, frame):
        asset = frame.asset

        keywords = asset.get_attr(self.arg_value("original_field"))

        if isinstance(keywords, basestring):
            keywords = [keywords]
        keywords = [x.lower().replace(' ', '') for x in keywords]
        if keywords is not None:
            goodList = self.arg_value("dictionary")
            filtered = []
            asset.set_attr(self.arg_value("target_field"), '')
            for goodkw in goodList:
                if any(goodkw.lower().replace(' ', '') in s for s in keywords):
                    filtered.append(goodkw)
            asset.set_attr(self.arg_value("target_field"), filtered)


class MapWordsProcessor(DocumentProcessor):
    """Read from an object's original_field.
    Use a map of regexes to apply changes.
    Write to object's target_field.

    Arguments:
        original_field: Original set of keywords.
        map: List of regular expression pairs to apply.
        target_field: Destination field for the filtered keywords.
    """
    toolTips = {
        'original_field': 'Original set of keywords.',
        'map': 'List of regular expression pairs to apply.',
        'target_field': 'Destination field for the filtered keywords.'
    }

    def __init__(self):
        super(MapWordsProcessor, self).__init__()
        self.add_arg(Argument("original_field", "string", default="keywords.all",
                              toolTip=self.toolTips['original_field']))
        self.add_arg(Argument("map", "list", default=[],
                              toolTip=self.toolTips['map']))
        self.add_arg(Argument("target_field", "string", default="keywords.filtered",
                              toolTip=self.toolTips['target_field']))

    def _process(self, frame):
        asset = frame.asset
        attrVal = asset.get_attr(self.arg_value("original_field"))
        if isinstance(attrVal, basestring):
            attrVal = [attrVal]

        finalVal = []
        if attrVal is not None:
            for val in attrVal:
                pairs = self.arg_value("map")
                for pair in pairs:
                    val = re.sub(pair[0], pair[1], val)
                finalVal.append(val)

            asset.set_attr(self.arg_value("target_field"), finalVal)


class ExtractWordsProcessor(DocumentProcessor):
    """Extract words longer than three characters and put them into an attribute.
    """
    toolTips = {
        'original_field': 'Original set of keywords.',
        'target_field': 'Destination field for the filtered keywords.'
    }

    def __init__(self):
        super(ExtractWordsProcessor, self).__init__()
        self.add_arg(Argument("original_field", "string", default="keywords.all",
                              toolTip=self.toolTips['original_field']))
        self.add_arg(Argument("target_field", "string", default="keywords.filtered",
                              toolTip=self.toolTips['target_field']))

    def _process(self, frame):
        asset = frame.asset

        sourceAttr = asset.get_attr(self.arg_value("original_field"))
        if isinstance(sourceAttr, basestring):
            sourceAttr = [sourceAttr]

        result = asset.get_attr(self.arg_value("target_field"))
        if result is None:
            result = []

        keywords = [item for sublist in [w.split() for w in sourceAttr] for item in sublist]
        for kw in keywords:
            kw = re.sub('[^0-9a-zA-Z]+', '', kw.lower())

            if kw not in result and len(kw) > 2:
                result.append(kw)

        if result is not []:
            asset.set_attr(self.arg_value("target_field"), result)


class WordNetWordsProcessor(DocumentProcessor):
    """Extract all terms in an attribute that are words in Wordnet, put them in another attribute
    Requires Wordnet data to be downloaded using nltk. This is necessary only once:
    python
    >> import nltk
    >> nltk.download('wordnet')

    """
    toolTips = {
        'original_field': 'Original set of keywords.',
        'target_field': 'Destination field for the filtered keywords.'
    }

    def __init__(self):
        super(WordNetWordsProcessor, self).__init__()
        self.add_arg(Argument("original_field", "string", default="keywords.all",
                              toolTip=self.toolTips['original_field']))
        self.add_arg(Argument("target_field", "string", default="keywords.words",
                              toolTip=self.toolTips['target_field']))
        self.nouns = {}

    def init(self):
        from nltk.corpus import wordnet as wn
        self.nouns = {x.name().split('.', 1)[0] for x in wn.all_synsets('n')}

    def _process(self, frame):
        asset = frame.asset

        sourceAttr = asset.get_attr(self.arg_value("original_field"))
        if isinstance(sourceAttr, basestring):
            sourceAttr = [sourceAttr]

        result = asset.get_attr(self.arg_value("target_field"))
        if result is None:
            result = []

        keywords = [item for sublist in [w.split() for w in sourceAttr] for item in sublist]
        for kw in keywords:
            kw = re.sub('[^0-9a-zA-Z]+', '', kw.lower())

            if kw in self.nouns and kw not in result:
                result.append(kw)

        if result is not []:
            asset.set_attr(self.arg_value("target_field"), result)


class ContentManagerProcessor(DocumentProcessor):
    """Processor that populates the content search for a Frame based on a list of it's
    metadata fields.

    """
    toolTips = {
        'namespace': 'Root namespace to add the content field to. For example the giving '
                     '"shotgun" as the argument would result in the "shotgun.content" '
                     'field being created.',
        'fields': 'List of metadata fields to include in the content for text searching.'
    }

    def __init__(self):
        super(ContentManagerProcessor, self).__init__()
        self.add_arg(Argument('namespace', 'string', required=True,
                              toolTip=self.toolTips['namespace']))
        self.add_arg(Argument('fields', 'list[string]', required=True,
                              toolTip=self.toolTips['fields']))

    def _process(self, frame):
        content = []
        for field in self.arg_value('fields'):
            try:
                value = frame.asset.get_attr(field)
            except AttributeError:
                value = None
            if value:
                content.append(str(value))
        frame.asset.add_content(self.arg_value('namespace'), content)


class ExpandFrameCopyAttributesProcessor(DocumentProcessor):
    """Sets the metadata attributess an ExpandFrame should copy from it's parent Frame.
    """
    toolTips = {
        'namespaces': 'List of metadata namespaces to copy to ExpandFrame.'
    }

    def __init__(self):
        super(ExpandFrameCopyAttributesProcessor, self).__init__()
        self.add_arg(Argument("attrs", "list[string]", required=True,
                              toolTip=self.toolTips['namespaces']))

    def _process(self, frame):
        frame.asset.set_attr('tmp.expandframe.attrs_to_copy',
                             self.arg_value('attrs'))


class MetadataRestRequestProcessor(DocumentProcessor):
    toolTips = {
        'endpoint': 'Fully qualified url to post json data to.',
        'serializer': 'Serializer to use for creating json data to post. '
                      'Options are: [default].',
        'phases': 'The processing phases where the endpoint is called'
                  'Options are: [init, process, teardown].',
        'method': 'Http method to use when making the request.',
        'verify_ssl': 'If True the request made to the endpoint will verify the SSL is '
                      'valid. Default is True.'
    }
    serializer_map = {
        'default': DefaultDocumentSerializer
    }

    def __init__(self):
        super(MetadataRestRequestProcessor, self).__init__()
        self.add_arg(Argument('endpoint', 'string', required=True,
                              toolTip=self.toolTips['endpoint']))
        self.add_arg(Argument('phases', 'list', default=['process'], required=True,
                              toolTip=self.toolTips['phases']))
        self.add_arg(Argument('serializer', 'string', default='default',
                              toolTip=self.toolTips['serializer']))
        self.add_arg(Argument('method', 'string', default='post',
                              toolTip=self.toolTips['method']))
        self.add_arg(Argument('verify_ssl', 'bool', default=True,
                              toolTip=self.toolTips['verify_ssl']))

    def init(self):
        super(MetadataRestRequestProcessor, self).init()
        if type(self.arg_value('phases')) is not list:
            raise TypeError('"phases" must be a list of phases.')
        if 'init' in self.arg_value('phases'):
            self._do_request(None)

    def teardown(self):
        super(MetadataRestRequestProcessor, self).teardown()
        if 'teardown' in self.arg_value('phases'):
            self._do_request(None)

    def _process(self, frame):
        if 'process' in self.arg_value('phases'):
            self._do_request(frame)

    def _do_request(self, frame):
        document_data = {}
        if frame is not None:
            serializer_class = self.serializer_map[self.arg_value('serializer')]
            serializer = serializer_class(frame.asset)
            document_data = serializer.get_dict()

        endpoint = self.arg_value('endpoint')
        self.logger.info('POSTing %s to %s.' % (document_data, endpoint))
        auth_header = self._get_auth_header()

        request_function = getattr(requests, self.arg_value('method').lower())
        response = request_function(endpoint, json=document_data, headers=auth_header,
                                    verify=self.arg_value('verify_ssl'))
        if response.status_code != 200:
            raise RuntimeError('There was an error posting to %s.\n\nStatus Code:%s\nContent:%s' %
                               (endpoint, response.status_code, response.content))

    def _get_auth_header(self):
        """Inspects environment variables and determines if any authentication needs to
        happen. If so, the authentication occurs and the correct header is returned.

        Returns:
            dict: Dictionary representing authorization headers.

        """
        jwt_user = os.environ.get('JWT_USER')
        jwt_password = os.environ.get('JWT_PASSWORD')
        jwt_auth_url = os.environ.get('JWT_AUTH_URL')

        # Send a User/Password to an endpoint to get a JWT token.
        if jwt_auth_url and jwt_user and jwt_password:
            response = requests.post(jwt_auth_url, auth=(jwt_user, jwt_password))
            response.raise_for_status()
            jwt_token = response.json()['token']
            return {'Authorization': 'Bearer %s' % jwt_token}

        # Checks env vars and attempts to create a signed JWT token.
        jwt_token = generate_jwt_from_env(self.arg_value('endpoint'))
        if jwt_token:
            return {'Authorization': 'Bearer %s' % jwt_token}

        else:
            return None


class SplitStringProcessor(DocumentProcessor):
    """Splits a string or list of strings based on a provided delimiter.
    """
    toolTips = {
        'original_field': 'Field to split. Needs to be specified.',
        'delimiter': 'Delimiter to use when splitting the string values. Default is ";"',
        'target_field': 'Target field to write the resulting values into. ' +
                        'Defaults to original_field'
    }

    def __init__(self):
        super(SplitStringProcessor, self).__init__()
        self.add_arg(Argument('original_field', 'string', required=True,
                              toolTip=self.toolTips['original_field']))
        self.add_arg(Argument('delimiter', 'string', default=";",
                              toolTip=self.toolTips['delimiter']))
        self.add_arg(Argument('target_field', 'string',
                              toolTip=self.toolTips['target_field']))

    def _process(self, frame):
        asset = frame.asset

        original_value = asset.get_attr(self.arg_value("original_field"))

        # If the attribute is not present then there's nothing to do
        if original_value is not None:
            if isinstance(original_value, basestring):
                original_value = [original_value]

            delimiter = self.arg_value("delimiter")

            # Target field is either the field specified, or the original one
            target_field = self.arg_value("target_field") or self.arg_value("original_field")

            target_value = []
            for string in original_value:
                split = [x.strip() for x in string.split(delimiter)]
                target_value.extend(split)

            asset.set_attr(target_field, target_value)
