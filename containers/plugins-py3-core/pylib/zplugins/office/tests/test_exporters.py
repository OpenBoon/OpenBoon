from PyPDF2 import PdfFileReader
from pathlib2 import Path

from zplugins.office.exporters import PdfExporter
from zsdk import Frame, Asset
from zsdk.testing import PluginUnitTestCase, zorroa_test_data
from zsdk.util import clean_export_root_dir


class PdfExporterUnitTestCase(PluginUnitTestCase):
    def setUp(self):
        super(PdfExporterUnitTestCase, self).setUp()
        clean_export_root_dir()
        self.pdf_1 = zorroa_test_data('office/pdfTest.pdf')
        self.pdf_2 = zorroa_test_data('office/irr.pdf')
        self.frame = Frame(Asset(str(self.pdf_1)))

    def test_export_original(self):
        args = {'export_original': True,
                'page_mode': 'separate'}
        processor = self.init_processor(PdfExporter(), args=args)
        processor.process(self.frame)
        destination = self.frame.asset.get_attr('exported.path')
        assert destination == str(Path(processor.export_root_dir, 'pdfTest.pdf'))
        assert Path(destination).exists()

    def test_merge_pdfs(self):
        args = {'file_name': 'merge_test.pdf'}
        self.frame_1 = Frame(Asset(str(self.pdf_1)))
        self.frame_2 = Frame(Asset(str(self.pdf_2)))
        for frame in [self.frame_1, self.frame_2]:
            frame.asset.set_attr('media.clip.start', 1.0)
            frame.asset.set_attr('media.clip.stop', 1.0)
        processor = self.init_processor(PdfExporter(), args=args)
        processor.process(self.frame_1)
        processor.process(self.frame_2)
        destination = Path(processor.export_root_dir, 'merge_test.pdf')
        assert destination.exists()
        assert PdfFileReader(destination.open('rb')).getNumPages() == 2

    def test_separate_pdf(self):
        args = {'page_mode': 'separate'}
        self.frame_1 = Frame(Asset(str(self.pdf_1)))
        self.frame_2 = Frame(Asset(str(self.pdf_2)))
        for frame in [self.frame_1, self.frame_2]:
            frame.asset.set_attr('media.clip.start', 1)
            frame.asset.set_attr('media.clip.stop', 1)
        processor = self.init_processor(PdfExporter(), args=args)
        processor.process(self.frame_1)
        processor.process(self.frame_2)
        assert Path(processor.export_root_dir, 'pdfTest_page_1_to_1.pdf').exists
        assert Path(processor.export_root_dir, 'irr_page_1_to_1.pdf').exists
