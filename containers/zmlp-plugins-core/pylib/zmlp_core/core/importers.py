from zmlp_core.image.importers import ImageImporter
from zmlp_core.office.importers import OfficeImporter
from zmlp_core.video.importers import VideoImporter
from zmlpsdk import AssetProcessor, Argument, ZmlpFatalProcessorException


class FileImportProcessor(AssetProcessor):
    """
    A composite processor made up of:
        * ImageImporter
        * OfficeImporter
        * VideoImporter

    Additionally, the FileImportProcessor checks to ensure media.type has been
    set, which eliminates the need for an assertion processor.
    """
    def __init__(self):
        super(FileImportProcessor, self).__init__()
        self.add_arg(Argument('extract_doc_pages',
                              'bool', default=False, toolTip="True to enable doc page extraction"))
        self.add_arg(Argument('extract_image_pages',
                              'bool', default=False, toolTip="True to image page extraction"))

        self.image_proc = ImageImporter()
        self.doc_proc = OfficeImporter()
        self.video_proc = VideoImporter()
        self.procs = [self.image_proc, self.doc_proc, self.video_proc]

    def set_context(self, context):
        super().set_context(context)
        for proc in self.procs:
            proc.set_context(context)
            proc.init()
        self.image_proc.arg("extract_pages").value = self.arg_value("extract_image_pages")
        self.doc_proc.arg("extract_pages").value = self.arg_value("extract_doc_pages")

    def process(self, frame):
        asset = frame.asset
        ext = asset.get_attr("source.extension").lower()
        for proc in self.procs:
            if ext in proc.file_types:
                proc.process(frame)
                break
        if not frame.asset.get_attr("media.type"):
            raise ZmlpFatalProcessorException(
                "The asset type '{}' is not supported".format(ext, asset.uri))
