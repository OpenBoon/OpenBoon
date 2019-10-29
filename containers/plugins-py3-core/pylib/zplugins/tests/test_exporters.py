import json

from pathlib2 import Path

from zplugins.metadata.exporters import JsonExporter, CsvExporter
from zsdk import Frame, Asset
from zsdk.testing import PluginUnitTestCase, zorroa_test_data
from zsdk.util import clean_export_root_dir


class JsonExporterUnitTestCase(PluginUnitTestCase):

    def setUp(self):
        super(JsonExporterUnitTestCase, self).setUp()
        clean_export_root_dir()

        self.toucan = Path(zorroa_test_data('images/set01/toucan.jpg'))
        self.frame = Frame(Asset(str(self.toucan)))

    def test_export(self):
        fields = ['source.filename', 'source.mediaType', 'source.fileSize', 'not.existing',
                  'source.filename.seriously?']
        processor = self.init_processor(JsonExporter(), {'fields': fields})
        processor.process(self.frame)
        processor.teardown()
        dst = Path(processor.export_root_dir, 'assets.json')
        assert dst.exists()

        with open(str(dst)) as json_file:
            json_data = json.load(json_file)[0]
        self.assertTrue('source' in json_data)
        self.assertTrue('fileSize' in json_data['source'])
        self.assertTrue(json_data['source']['filename'] == 'toucan.jpg')


class CsvExporterTestUnitTestCase(PluginUnitTestCase):
    def setUp(self):
        super(CsvExporterTestUnitTestCase, self).setUp()
        clean_export_root_dir()

        self.toucan = Path(zorroa_test_data('images/set01/toucan.jpg'))
        self.frame = Frame(Asset(str(self.toucan)))

    def test_export(self):
        fields = ['source.filename', 'source.mediaType', 'source.fileSize']
        processor = self.init_processor(CsvExporter(), {'fields': fields})
        processor.process(self.frame)
        processor.teardown()
        dst = Path(processor.export_root_dir, 'assets.csv')
        assert dst.exists()
        csv_data = open(str(dst)).read()
        self.assertTrue(csv_data.startswith('source.filename,source.mediaType,source.fileSize'))
        self.assertTrue('toucan.jpg' in csv_data)
        self.assertTrue('image/jpeg' in csv_data)
        self.assertTrue('97221' in csv_data)

    def test_ascii_encode_error(self):
        # Encountered production data with this value and it caused failures.
        self.frame.asset.set_attr(u'analysis.metadata.239566.Ag\xc3\xaancia', 'a')

        fields = [u'analysis.metadata.239566.Ag\xc3\xaancia']
        processor = self.init_processor(CsvExporter(), {'fields': fields})
        processor.process(self.frame)
        processor.teardown()
        dst = Path(processor.export_root_dir, 'assets.csv')
        assert dst.exists()

    def test_ascii_no_metadata_error(self):
        # Encountered production data with this value and it caused failures.
        self.frame.asset.set_attr(u'basename', u'Ag\xc3\xaancia')

        fields = [u'basename']
        processor = self.init_processor(CsvExporter(), {'fields': fields})
        processor.process(self.frame)
        processor.teardown()
        dst = Path(processor.export_root_dir, 'assets.csv')
        assert dst.exists()

    def test_list_output(self):
        self.frame.asset.set_attr(u'keywords.list', [u'boat', 5])
        fields = [u'keywords.list']
        processor = self.init_processor(CsvExporter(), {'fields': fields})
        processor.process(self.frame)
        processor.teardown()
        dst = Path(processor.export_root_dir, 'assets.csv')
        assert dst.exists()
        csv_data = open(str(dst)).read()
        self.assertEquals(csv_data, 'keywords.list\nboat 5\n')
