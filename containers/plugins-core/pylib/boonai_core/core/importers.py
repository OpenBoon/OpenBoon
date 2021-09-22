import reverse_geocode

from boonai_core.image.importers import ImageImporter
from boonai_core.video.importers import VideoImporter
from boonflow import AssetProcessor, FatalProcessorException


class FileImportProcessor(AssetProcessor):
    """
    A composite processor made up of:
        * ImageImporter
        * OfficeImporter
        * VideoImporter

    Additionally, the FileImportProcessor checks to ensure media.type has been
    set, which eliminates the need for an assertion processor.
    """

    fatal_errors = True
    """All errors from this processor are fatal."""

    def __init__(self):
        super(FileImportProcessor, self).__init__()

        self.image_proc = ImageImporter()
        self.video_proc = VideoImporter()
        self.procs = [self.image_proc, self.doc_proc, self.video_proc]

    def set_context(self, context):
        super().set_context(context)
        for proc in self.procs:
            proc.set_context(context)
            proc.init()

    def process(self, frame):
        asset = frame.asset
        ext = asset.get_attr("source.extension").lower()

        for proc in self.procs:
            if ext in proc.file_types:
                proc.process(frame)
                break

        if not frame.asset.get_attr("media.type"):
            raise FatalProcessorException(
                "The asset type '{}' is not supported. Asset URI: {}".format(ext, asset.uri))

        if asset.attr_exists('location.point'):
            self.apply_reverse_geocode(asset)

    def apply_reverse_geocode(self, asset):
        try:
            lat = asset.get_attr('location.point.lat')
            lon = asset.get_attr('location.point.lon')
            coords = (lat, lon),
            rev = reverse_geocode.search(coords)
            if rev:
                asset.set_attr("location.city", rev[0]['city'])
                asset.set_attr("location.code", rev[0]['country_code'])
                asset.set_attr("location.country", rev[0]['country'])
        except Exception as e:
            self.logger.warning("Failed to apply geo data '%s'", e)
