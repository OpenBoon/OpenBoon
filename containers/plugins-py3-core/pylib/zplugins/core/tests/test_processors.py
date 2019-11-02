#!/usr/bin/python

from unittest import skip
from unittest.mock import patch
from pathlib2 import Path

from zorroa.zsdk.exception import UnrecoverableProcessorException
from zplugins.core.processors import PythonScriptProcessor, DownloadAssetProcessor, \
    AssertProcessor, SetIdProcessor, AddPermissionProcessor, \
    SetAttributesProcessor
from zorroa.zsdk import Document, Frame
from zorroa.zsdk.testing import PluginUnitTestCase

MOCK_PERMISSION = {
    'authority': 'string',
    'description': 'string',
    'fullName': 'string',
    'id': 'string',
    'name': 'string',
    'type': 'string'
}


class ProcessorsUnitTestCase(PluginUnitTestCase):

    def test_python_script(self):
        frame = Frame(Document())
        ih = self.init_processor(PythonScriptProcessor(), {'script': "_doc.set_attr('foo', 'bar')"})
        ih.process(frame)
        self.assertEquals('bar', frame.asset.get_attr('foo'))

    def test_download_asset(self):
        download_path = Path('./master.zip')
        try:
            frame = Frame(Document())
            frame.asset.set_attr(
                'tmp.download_url',
                'https://github.com/google/google-api-php-client/archive/master.zip')
            processor = self.init_processor(DownloadAssetProcessor(),
                                            {'destination_directory': './'})
            processor.process(frame)
            assert download_path.exists()
            assert frame.asset.get_attr('tmp.download_path') == str(download_path)
        finally:
            if download_path.exists():
                download_path.unlink()

    def test_assert_processor(self):
        frame = Frame(Document())
        ih = self.init_processor(AssertProcessor(), args={'script': "1==1", 'message': "bilbo"})
        ih.process(frame)
        self.assertEquals("error", ih.executor.script["type"])
        self.assertEquals("bilbo", ih.executor.script["payload"]["message"])

    def test_set_attributes_processor_set_attrs(self):
        frame = Frame(Document())
        ih = self.init_processor(SetAttributesProcessor(), args={'attrs': {'foo.bar': 1}})
        ih.process(frame)
        self.assertEquals(1, frame.asset.get_attr("foo.bar"))

    def test_set_attributes_processor_remove_attrs(self):
        frame = Frame(Document({"document": {"foo": "bar"}}))
        self.assertEquals("bar", frame.asset.get_attr("foo"))
        ih = self.init_processor(SetAttributesProcessor(), args={'remove_attrs': ['foo']})
        ih.process(frame)
        self.assertIsNone(frame.asset.get_attr("foo"))

    def test_set_id_processor(self):
        frame = Frame(Document())
        frame.asset.set_attr('pk', 1)
        original_id = frame.asset.id
        processor = self.init_processor(SetIdProcessor(), args={'attribute': 'pk'})
        processor.process(frame)
        assert frame.asset.id != original_id


@patch('archivist.client.Client.post')
@patch('zplugins.core.processors.AddPermissionProcessor.permission_exists')
class TestAddPermissionProcessor(PluginUnitTestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_bad_permission_name_fails(self, mock_get_client, mock_post_client):
        name = 'foobar'
        processor = self.init_processor(AddPermissionProcessor(),
                                        {'name': name, 'create': True, 'access': 1})
        with self.assertRaises(UnrecoverableProcessorException) as cm:
            processor.process(Document())
        self.assertEqual(str(cm.exception), 'Invalid permission name: ' + name)
        self.assertEqual(0, mock_get_client.call_count, 'Check Exists')
        self.assertEqual(0, mock_post_client.call_count, 'Create permission')

    def test_duplicate_name_is_not_checked(self, mock_get_client, mock_post_client):
        # Pre-populate the checked_names property and ensure that
        # the processor doesn't check over and over again.
        name = 'spi::test'
        processor = self.init_processor(AddPermissionProcessor(),
                                        {'name': name, 'create': True, 'access': 1})

        processor.checked_names = set(["spi::test"])
        processor.process(Frame(Document()))
        processor.process(Frame(Document()))

        self.assertEqual(0, mock_get_client.call_count, 'Check Exists')
        self.assertEqual(0, mock_post_client.call_count, 'Create permission')

    def test_existing_permission_not_created(self, mock_get_client, mock_post_client):
        name = 'spi::test'
        processor = self.init_processor(AddPermissionProcessor(),
                                        {'name': name, 'create': True, 'access': 1})
        mock_get_client.return_value = True
        processor.process(Frame(Document()))
        self.assertEqual(1, mock_get_client.call_count, 'Check Exists')
        self.assertEqual(0, mock_post_client.call_count, 'Create permission')

    def test_creates_new_permission_once(self, mock_get_client, mock_post_client):
        name = 'spi::test'
        processor = self.init_processor(AddPermissionProcessor(),
                                        {'name': name, 'create': True, 'access': 1})

        init_mock_return_values(mock_get_client, mock_post_client)
        processor.process(Frame(Document()))
        processor.process(Frame(Document()))

        self.assertEqual(1, mock_post_client.call_count, 'Create permission')

    def test_add_permission(self, mock_get_client, mock_post_client):
        name = 'spi::test'
        access = 6
        processor = self.init_processor(AddPermissionProcessor(),
                                        {'name': name, 'create': True, 'access': access})
        init_mock_return_values(mock_get_client, mock_post_client)
        doc = Document()
        processor.process(Frame(doc))
        self.assertIn(name, doc.permissions)
        self.assertEqual(doc.permissions[name], access)

    def test_add_to_existing_permissions(self, mock_get_client, mock_post_client):
        name = 'spi::test'
        access = 6
        processor = self.init_processor(AddPermissionProcessor(),
                                        {'name': name, 'create': True, 'access': access})
        init_mock_return_values(mock_get_client, mock_post_client)
        doc = Document()
        processor.process(Frame(doc))
        self.assertIn(name, doc.permissions)
        self.assertEqual(doc.permissions[name], access)

        name2 = name+'Foo'
        access2 = access + 1
        processor = self.init_processor(AddPermissionProcessor(),
                                        {'name': name2, 'create': True, 'access': access2})
        processor.process(Frame(doc))

        self.assertIn(name, doc.permissions)
        self.assertEqual(doc.permissions[name], access)

        self.assertIn(name2, doc.permissions)
        self.assertEqual(doc.permissions[name2], access2)

    @skip('Required arguments not enforced')
    def test_required_arguments(self):
        with self.assertRaises(UnrecoverableProcessorException):
            processor = self.init_processor(AddPermissionProcessor(), {})
            processor.process(Document())


def init_mock_return_values(mock_get_client, mock_post_client):
    # AddPermissionProcessor.permission_exists we'll return false by default.
    mock_get_client.return_value = False
    # The response from Client.post should be straight json
    mock_post_client.return_value = MOCK_PERMISSION
