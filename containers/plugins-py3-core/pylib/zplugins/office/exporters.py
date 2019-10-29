from PyPDF2 import PdfFileReader, PdfFileWriter
from pathlib2 import Path

from zsdk import Argument, AbstractExporter
from zsdk.exception import UnrecoverableProcessorException


class OfficeExporter(AbstractExporter):
    """Exporter responsible for exporting Office Document assets.

    Args:
        None at this time

    """
    file_types = ['doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx']

    def __init__(self):
        super(OfficeExporter, self).__init__()
        self.exported_sources = set()

    def export(self, frame):
        # Make sure to call the overridden method below
        self.export_as_source(frame.asset)

    def export_as_source(self, asset):
        # Only export the file with a matching source.path once
        source_path = asset.get_attr('source.path')
        if source_path not in self.exported_sources:
            self.exported_sources.add(source_path)
            super(OfficeExporter, self).export_as_source(asset)


class PdfExporter(AbstractExporter):
    """Exporter responsible for exporting PDF assets.

    Args:
        page_mode(str): If set to "merge" then all pdfs from this export will be merged
          into a single pdf file that has multiple pages. If set to "separate" then each
          pdf will be exported separately.
        file_name(str): File name used for the merged pdf file. Not used when the page_mode
          is "separate".

    """
    toolTips = {
        'page_mode': 'If set to "merge" then all pdfs from this export will be merged' +
                     'into a single pdf file that has multiple pages. If set to "separate" ' +
                     'then each pdf will be exported separately.',
        'file_name': 'File name used for the merged pdf file. Not used when the pageMode' +
                     'is "separate".'
    }

    file_types = ['pdf']

    def __init__(self):
        super(PdfExporter, self).__init__()
        self.add_arg(Argument('page_mode', 'str', default='merge',
                              toolTip=self.toolTips['page_mode']))
        self.add_arg(Argument('file_name', 'str', toolTip=self.toolTips['file_name']))

    def init(self):
        if self.arg_value('page_mode') not in ['merge', 'separate']:
            raise UnrecoverableProcessorException('"page_mode" must be either "merge" or'
                                                  '"separate".')
        if self.arg_value('page_mode') == 'merge' and not self.arg_value('file_name'):
            raise UnrecoverableProcessorException('If the "page_mode" is "merge" then '
                                                  'the "file_name" must be provided.')

    def export(self, frame):
        asset = frame.asset
        if not asset.get_attr('media.clip.start'):
            self.export_as_source(asset)
        else:
            # Get a PDF reader for the source file and a PDF writer for the destination.
            start = int(asset.get_attr('media.clip.start')) - 1
            stop = int(asset.get_attr('media.clip.stop')) - 1
            source_path = self.get_source_path(asset)
            source_pdf_reader = PdfFileReader(source_path.open('rb'))
            exported_pdf_writer = PdfFileWriter()

            # If asked to merge then add the previously exported pdf to the writer.
            if self.arg_value('page_mode') == 'merge':
                destination_path = Path(self.export_root_dir, self.arg_value('file_name'))
                if destination_path.exists():
                    destination_pdf_reader = PdfFileReader(str(destination_path))
                    for i in xrange(destination_pdf_reader.getNumPages()):
                        p = destination_pdf_reader.getPage(i)
                        exported_pdf_writer.addPage(p)
            elif self.arg_value('page_mode') == 'separate':
                destination_path = Path(self.export_root_dir,
                                        '%s_page_%s_to_%s.pdf' % (source_path.stem, start,
                                                                  stop))
            else:
                raise UnrecoverableProcessorException('Invalid value was given for the '
                                                      '"page_mode" argument.')

            # Extract the clip from the source PDF and add it to the writer.
            for i in xrange(start, stop + 1):
                p = source_pdf_reader.getPage(i)
                exported_pdf_writer.addPage(p)

            # Write the pdf to a tmp location and move it.
            exported_pdf_writer.write(destination_path.open('wb'))
            self.set_exported_metadata(destination_path, asset)
