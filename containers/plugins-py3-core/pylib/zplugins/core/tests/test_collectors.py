#!/usr/bin/python
import shutil
import uuid
from zipfile import ZipFile

from mock import patch

from archivist.export import Export
from zplugins.core.collectors import ImportCollector, ExportCollector
from zsdk import Document, Frame, Asset
from zsdk.exception import UnrecoverableProcessorException
from zsdk.testing import PluginUnitTestCase, zorroa_test_data
from zsdk.util import get_export_file_path, get_export_root_dir, clean_export_root_dir


class Consumer:
    def __init__(self):
        self.count = 0

    def accept(self, frame):
        self.count += 1


class ImportCollectorUnitTestCase(PluginUnitTestCase):

    @patch('zplugins.core.collectors.Client.post')
    def testCollect(self, post_patch):
        post_patch.return_value = {}

        frames = [
            Frame(Document({'id': '1', 'document':
                {'foo': 'bar'}, 'permissions': {'zorroa::foo': 1}}))
        ]

        collector = self.init_processor(ImportCollector())
        collector.collect(frames)

        asset = post_patch.call_args_list[0][0][1]['sources'][0]
        assert asset['links'] is None
        assert asset['replace'] is False
        assert asset['id'] == "1"
        assert asset['document'] == {'foo': 'bar'}
        assert asset['permissions'] == {'zorroa::foo': 1}


@patch('zplugins.core.collectors.get_export', autospec=Export)
class ExportCollectorUnitTestCase(PluginUnitTestCase):

    def setUp(self):
        super(ExportCollectorUnitTestCase, self).setUp()
        self.export_root_dir = get_export_root_dir()
        clean_export_root_dir()

        self.toucan = zorroa_test_data("images/set01/toucan.jpg")
        self.hyena = zorroa_test_data("images/set01/hyena.jpg")

        asset = Asset()
        self.frame = Frame(asset)

        asset2 = Asset()
        self.frame2 = Frame(asset2)

    def test_export(self, mock_get_export):
        collector = self.init_processor(ExportCollector(),
                                        global_args={'exportArgs': {'exportId': 1}, 'batchSize': 1})

        mock_export = mock_get_export.return_value
        mock_export.name = 'TextExport'
        mock_export.id = uuid.uuid4()

        shutil.copy(self.toucan, get_export_file_path("toucan.jpg"))

        collector.teardown()

        self.assertEqual(1, mock_get_export.call_count)
        self.assertEqual(1, mock_export.add_file.call_count)

        (storage_id, filename) = mock_export.add_file.call_args[0]

        object_file = collector.ofs.get(storage_id)
        self.assertTrue(object_file.exists())
        self.assertEqual('%s.zip' % mock_export.name, filename)

        self.assertEqual(['%s/toucan.jpg' % mock_export.name], ZipFile(object_file.path).namelist())

    def test_multi_asset_export(self, mock_get_export):
        collector = self.init_processor(ExportCollector(),
                                        global_args={'exportArgs': {'exportId': 1}, 'batchSize': 1})

        mock_export = mock_get_export.return_value
        mock_export.name = 'TextExport'
        mock_export.id = uuid.uuid4()

        shutil.copy(self.toucan, get_export_file_path("toucan.jpg"))
        shutil.copy(self.hyena, get_export_file_path("hyena.jpg"))

        collector.teardown()

        self.assertEqual(1, mock_get_export.call_count)
        self.assertEqual(1, mock_export.add_file.call_count)

        (storage_id, filename) = mock_export.add_file.call_args[0]

        object_file = collector.ofs.get(storage_id)
        self.assertTrue(object_file.exists())
        self.assertEqual('%s.zip' % mock_export.name, filename)

        self.assertEqual({'%s/toucan.jpg' % mock_export.name, '%s/hyena.jpg' % mock_export.name},
                         set(ZipFile(object_file.path).namelist()))

    def test_missing_export(self, mock_get_export):
        collector = self.init_processor(ExportCollector(),
                                        global_args={'exportArgs': {'exportId': 1}, 'batchSize': 1})
        self.frame.asset.set_attr('exported', {})
        self.assertRaises(UnrecoverableProcessorException, collector.teardown)

    def test_duplicate_exports(self, mock_get_export):
        collector = self.init_processor(ExportCollector(),
                                        global_args={'exportArgs': {'exportId': 1}, 'batchSize': 1})

        mock_export = mock_get_export.return_value
        mock_export.name = 'TextExport'
        mock_export.id = uuid.uuid4()

        # Copy 3 of same file
        shutil.copy(self.toucan, get_export_file_path("toucan.jpg"))
        shutil.copy(self.toucan, get_export_file_path("toucan.jpg"))
        shutil.copy(self.toucan, get_export_file_path("toucan.jpg"))

        collector.teardown()

        self.assertEqual(1, mock_get_export.call_count)
        self.assertEqual(1, mock_export.add_file.call_count)

        (storage_id, filename) = mock_export.add_file.call_args[0]

        object_file = collector.ofs.get(storage_id)
        self.assertTrue(object_file.exists())
        self.assertEqual('%s.zip' % mock_export.name, filename)

        files = ZipFile(object_file.path).namelist()
        self.assertEquals(3, len(files))
        self.assertTrue('TextExport/duplicate(1)_toucan.jpg' in files)
        self.assertTrue('TextExport/duplicate(2)_toucan.jpg' in files)
        self.assertTrue('TextExport/toucan.jpg' in files)
