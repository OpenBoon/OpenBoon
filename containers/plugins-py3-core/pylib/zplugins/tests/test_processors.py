import json
import os
import pytest
import requests
from mock import patch
from pathlib2 import Path
from requests import Response

import zsdk
from zplugins.metadata import processors
from zsdk.testing import PluginUnitTestCase


class MetadataUnitTests(PluginUnitTestCase):

    def test_filterkeywords(self):
        frame = zsdk.Frame(zsdk.Document())
        frame.asset.set_attr("keywords.all", ["a", "b", "c", "d"])

        processor = self.init_processor(processors.FilterKeywordsProcessor(),
                                        {"dictionary": ["a", "b", "c"]})
        processor.process(frame)

        self.assertEquals(["a", "b", "c"], frame.asset.get_attr("keywords.filtered"))

    def test_mapwords(self):
        frame = zsdk.Frame(zsdk.Document())
        frame.asset.set_attr("keywords.all", ["a", "b", "c", "d"])

        processor = self.init_processor(processors.MapWordsProcessor(),
                                        {"map": [["a", "b"], ["c", "d"]]})
        processor.process(frame)

        self.assertEquals(["b", "b", "d", "d"], frame.asset.get_attr("keywords.filtered"))

    def test_extractwords(self):
        frame = zsdk.Frame(zsdk.Document())
        frame.asset.set_attr("keywords.all", "this is a test")

        processor = self.init_processor(processors.ExtractWordsProcessor())
        processor.process(frame)

        self.assertEquals(["this", "test"], frame.asset.get_attr("keywords.filtered"))

    def test_wordnetwords(self):
        frame = zsdk.Frame(zsdk.Document())
        frame.asset.set_attr("keywords.all", "The quick brown fox jumps over the lazy dog")

        with pytest.warns(RuntimeWarning) as warnings_result:
            processor = self.init_processor(processors.WordNetWordsProcessor())
            processor.process(frame)

        # When the following assertion starts failing, it is because the underlying 3rd party
        # that issued the RuntimeWarning being caught here was upgraded and doesn't warn any more.
        assert len(warnings_result) >= 1

        self.assertEquals(['quick', 'brown', 'fox', 'over', 'dog'],
                          frame.asset.get_attr("keywords.words"))

    def test_expandframecopyattrs(self):
        frame = zsdk.Frame(zsdk.Document())
        processor = processors.ExpandFrameCopyAttributesProcessor()
        processor.set_context(zsdk.Context(None, {'attrs': ['shotgun', 'notes']}, {}))
        processor.process(frame)
        self.assertItemsEqual(frame.asset.get_attr('tmp.expandframe.attrs_to_copy'),
                              ['shotgun', 'notes'])

    def test_contentmanager(self):
        frame = zsdk.Frame(zsdk.Document())
        frame.asset.set_attr('book.description', 'Greatest book ever.')
        frame.asset.set_attr('notes', 'This is not the best book.')
        processor = processors.ContentManagerProcessor()
        args = {'fields': ['book.description', 'notes'],
                'namespace': 'book'}
        processor.set_context(zsdk.Context(None, args, {}))
        processor.process(frame)
        self.assertItemsEqual(frame.asset.get_attr('book.content'),
                              ['Greatest book ever.', 'This is not the best book.'])

    def test_contentmanager_bad_fields(self):
        frame = zsdk.Frame(zsdk.Document())
        frame.asset.set_attr('book.description', 'Greatest book ever.')
        processor = processors.ContentManagerProcessor()
        args = {'fields': ['book.description.author'],
                'namespace': 'book'}
        processor.set_context(zsdk.Context(None, args, {}))
        processor.process(frame)
        self.assertEqual(frame.asset.get_attr('book.content'), set())

    def test_splitstring(self):
        frame = zsdk.Frame(zsdk.Document())

        # Test case when the attr doesn't exist first
        processor = self.init_processor(processors.SplitStringProcessor(),
                                        {'original_field': 'keywords.all', 'delimiter': ','})
        processor.process(frame)

        # Now test the easiest case
        frame.asset.set_attr('keywords.all', 'foo, bar, baz')
        processor = self.init_processor(processors.SplitStringProcessor(),
                                        {'original_field': 'keywords.all', 'delimiter': ','})
        processor.process(frame)
        self.assertEquals(['foo', 'bar', 'baz'], frame.asset.get_attr("keywords.all"))

        # Test a second variation where the original attribute contains a list
        frame.asset.set_attr('keywords.all', ['foo; bar; baz', 'qux', 'quux'])
        processor = self.init_processor(processors.SplitStringProcessor(),
                                        {'original_field': 'keywords.all'})
        processor.process(frame)
        self.assertEquals(['foo', 'bar', 'baz', 'qux', 'quux'],
                          frame.asset.get_attr("keywords.all"))


class PostMetadataToRestApiUnitTestCase(PluginUnitTestCase):
    def setUp(self):
        super(PostMetadataToRestApiUnitTestCase, self).setUp()
        if os.environ.get('GCP_JWT_CREDENTIAL_PATH'):
            del os.environ['GCP_JWT_CREDENTIAL_PATH']
        if os.environ.get('GOOGLE_APPLICATION_CREDENTIALS'):
            del os.environ['GOOGLE_APPLICATION_CREDENTIALS']

    @patch.object(requests, 'post')
    def test_postmetadatatorestapi_200(self, _patch):
        response = Response()
        response.status_code = 200
        _patch.return_value = response
        frame = zsdk.Frame(zsdk.Document())
        frame.asset.set_attr('book.description', 'Greatest book ever.')
        args = {'endpoint': 'https://google.com', 'serializer': 'default'}
        processor = self.init_processor(processors.MetadataRestRequestProcessor(), args)
        processor.process(frame)

    @patch.object(requests, 'post')
    def test_postmetadatatorestapi_200_teardown(self, _patch):
        response = Response()
        response.status_code = 200
        _patch.return_value = response
        frame = zsdk.Frame(zsdk.Document())
        frame.asset.set_attr('book.description', 'Greatest book ever.')
        args = {'endpoint': 'https://google.com', 'serializer': 'default',
                "phases": ["teardown"]}
        processor = self.init_processor(processors.MetadataRestRequestProcessor(), args)
        processor.teardown()
        assert _patch.call_count == 1

    @patch.object(requests, 'post')
    def test_postmetadatatorestapi_200_init(self, _patch):
        response = Response()
        response.status_code = 200
        _patch.return_value = response
        frame = zsdk.Frame(zsdk.Document())
        frame.asset.set_attr('book.description', 'Greatest book ever.')
        args = {'endpoint': 'https://google.com', 'serializer': 'default',
                "phases": ["init"]}
        self.init_processor(processors.MetadataRestRequestProcessor(), args)
        assert _patch.call_count == 1

    @patch.object(requests, 'post')
    def test_postmetadatatorestapi_403(self, _patch):
        response = Response()
        response.status_code = 403
        response._content = 'forbidden'
        _patch.return_value = response
        frame = zsdk.Frame(zsdk.Document())
        frame.asset.set_attr('book.description', 'Greatest book ever.')
        args = {'endpoint': 'http://google.com', 'serializer': 'default'}
        processor = self.init_processor(processors.MetadataRestRequestProcessor(), args)
        with pytest.raises(RuntimeError) as error:
            processor.process(frame)
        assert error.value.args[0] == ('There was an error posting to http://google.com.'
                                       '\n\nStatus Code:403\nContent:forbidden')

    @patch.object(requests, 'post')
    def test_get_auth_header_jwt_user_pass(self, post_patch):
        try:
            response = Response()
            response.status_code = 200
            response._content = json.dumps({'token': 'abcd'})
            post_patch.return_value = response
            os.environ['JWT_USER'] = 'fake_person'
            os.environ['JWT_PASSWORD'] = 'letmein'
            os.environ['JWT_AUTH_URL'] = 'https://studio.com/api/auth/login'
            processor = self.init_processor(processors.MetadataRestRequestProcessor(), {})
            assert processor._get_auth_header() == {'Authorization': 'Bearer abcd'}
        finally:
            del os.environ['JWT_USER']
            del os.environ['JWT_PASSWORD']
            del os.environ['JWT_AUTH_URL']

    def test_get_auth_header_google_credentials(self):
        try:
            google_credentials = {
                u'type': u'service_account',
                u'project_id': u'fake_project',
                u'private_key_id': u'a2e312e14b1a132f3bf42e8c91beeaa624084220',
                u'private_key': u'-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCB' +
                                'KcwggSjAgEAAoIBAQDlLxgvmzmAa8L9\nHDmcAahuFUPTgW83X1JXMDqzPDUaI/' +
                                'R6yNefPjKTXX0FInAJGU+eMCgdBuosQTNp\nFqSriSAPbZOb2qhxbyv+y2/d3SO' +
                                '7RpcqYpxYvWI2aGEue4w7FCyWR1usTVAFsdn9\nfeXbJ1BtGvebjDlnQD35dsRS' +
                                'MMLSWaqE3D2QuuRK87ORY1LXopbrkQfZSH7C2NJ+\nWtUNkiK2VUVJ37uz9uaSe' +
                                'wmU1/9vLVRX2z2lYxE43KKRFsQ58pFXbfQchy2m/G6x\n8klgHyqdX2YcqJ2ugr' +
                                '40GMFPsJFRjserUrTTdu6bHWboxrONwBLXr9PtlRvC7Z9N\n+C9f3QgxAgMBAAE' +
                                'CggEABGnmZTdP8Y5RumJmZR/ejKUt8dcwq0Azk6CHBt/iVWsr\nGIjA500ROAeR' +
                                'Qm2t+uzg/MbIZPu2u4yzZuMT34ZT5twpPzOEMjAOCLmGAV+yghcr\nTtqIkIhwK' +
                                'WRT98tTEWmzbJ7579zqzTtGO5SmvjRY/wrpiRY3nkqNTv43rG4KOHcO\ngv6ka9' +
                                '040ofi9kskQlNec3FH96yKbqR/4oo2BE+Cj71gtAHdmwnB2ABau6eulUOx\n7N3' +
                                '45uGx2mRdF1NIqhUyhbMLL0u+dOOPoG1KohmXWqx9mAbJUlrRl6+lKf0Z/DQd\n' +
                                'TnDD4hzLSrHly/Y+UeStPEE1rF8wLSwgoROM8wAHCQKBgQD/cpKTvSl2vOIgdSx' +
                                's\np2xocYXt6YyNk/lOA/PyIybGKwFPLx6ZHLv06NAx4TWwfVNiQPYrPEbv1u9v' +
                                'iO5b\nGgKukht4pKCwku+78PGUUhjeFwpKwCD98QAFCcjO0P3KwX0tSZ1r5KxLq' +
                                'iiygx5T\n57MN5Xq9Im+h04MrQ8K9d7aL2QKBgQDlrfsvKYO/54avv8cgtk8F7F' +
                                'FZr4k2Y0s3\nStf0Omfwdb0XQ/3V0BOX1OGVzfgB7hFhwvFj4KJ8FO7ZzEWE6hz' +
                                'xrLClO9NfAb2K\nKhypHBNxDpDjYE50vItHP5y5ckwL2CFSD4vkYMXWsOx93Rey' +
                                'ZuUChqqlI9zttG5s\nZG7KaFhgGQKBgBKErMmXCf14ne/6bpkF3h8IM8xWhQDyf' +
                                'D1CnhmsoK/3CCZEZTR2\nV83eXtFu4SSFQVk7Gl1yBjZ23263f4gNL6qWHvW1AY' +
                                '0racO3KNw2w2S+SNm4aeve\n38B9wBf2IJ/lPPzefFIJBtCJal/ej4FHp7xXKXm' +
                                'ymGMj4TtusxxnJE2pAoGBAJWg\nxX4RFqLDq4v9bTWTb2HVdZ3u7H4BK/Qmjw+t' +
                                'qR8PJRPuGITdnJYSerAEUei39pzw\nyh+eLVyWqpA7MqMHfbm4HhBXLDm9wYQsM' +
                                'SW+g6/opF1s1U/ZsQbGm0x4+K/Sqspy\nFFnA7cIS2s6YZaK0w8bkgnE5Lumw3S' +
                                'tCNqcoWUKpAoGAN/dNBIj8wliafoHd7Odv\nulY9bB5qVabcduTrf1mJVQJmnug' +
                                'B34DzXjpu06YL0+wMZo69VxXwyPbiX7vhsbgc\nRiUjbzrBZVYeG5Fh9000/wOP' +
                                'hBdQK3yrFKDkteH8Y61Eu4Wq+7vFhDCwHGuBAhcc\naD5tjheC8VxhQ2bZl6iVY' +
                                '8U=\n-----END PRIVATE KEY-----\n',
                u'client_email': u'fakey@appspot.gserviceaccount.com',
                u'client_id': u'102981946955391808574',
                u'auth_uri': u'https://accounts.google.com/o/oauth2/auth',
                u'token_uri': u'https://accounts.google.com/o/oauth2/token',
                u'auth_provider_x509_cert_url': u'https://www.googleapis.com/oauth2/v1/certs',
                u'client_x509_cert_url': u'https://www.googleapis.com/robot/v1/metadata/x509/' +
                                         'fakey@appspot.gserviceaccount.com'}
            google_credential_path = Path('/tmp/google.json')
            with google_credential_path.open('w') as _file:
                _file.write(json.dumps(google_credentials, ensure_ascii=False))
            os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = str(google_credential_path)
            processor = self.init_processor(processors.MetadataRestRequestProcessor(),
                                            {'endpoint': 'http://localhost/api/v1/dosomething'})
            auth_header = processor._get_auth_header()['Authorization']
            assert auth_header.startswith('Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9')
        finally:
            if google_credential_path.exists():
                google_credential_path.unlink()
            if os.environ.get('GOOGLE_APPLICATION_CREDENTIALS'):
                del os.environ['GOOGLE_APPLICATION_CREDENTIALS']
