from pathlib2 import Path

from zplugins.core.processors import PythonScriptProcessor, DownloadAssetProcessor, \
    AssertProcessor, SetIdProcessor, SetAttributesProcessor
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
        download_path = Path('/tmp/master.zip')
        try:
            frame = Frame(Document())
            frame.asset.set_attr(
                'tmp.download_url',
                'https://github.com/google/google-api-php-client/archive/master.zip')
            processor = self.init_processor(DownloadAssetProcessor(),
                                            {'destination_directory': '/tmp'})
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


def init_mock_return_values(mock_get_client, mock_post_client):
    # AddPermissionProcessor.permission_exists we'll return false by default.
    mock_get_client.return_value = False
    # The response from Client.post should be straight json
    mock_post_client.return_value = MOCK_PERMISSION
