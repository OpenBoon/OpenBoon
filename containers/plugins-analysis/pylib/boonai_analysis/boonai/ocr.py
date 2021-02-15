import pytesseract

from boonflow import file_storage
from boonflow.base import AssetProcessor, FileTypes
from boonflow.proxy import get_proxy_level_path
from boonflow.analysis import ContentDetectionAnalysis


class ZviOcrProcessor(AssetProcessor):
    """
    Makes a proxy video for a full video file.  Clip assets will reference
    this video file.
    """
    file_types = FileTypes.images | frozenset(['pdf'])
    """Allows PDFs to be processed by OCR which is a required common case."""

    namespace = "zvi-text-detection"

    # Tesseract uses a lot of CPU
    use_threads = False

    def __init__(self):
        super(ZviOcrProcessor, self).__init__()

    def process(self, frame):
        p_path = self.get_proxy_image(frame.asset)
        data = pytesseract.image_to_string(p_path)
        data = data.replace('\r', ' ').replace('\n', ' ')

        analysis = ContentDetectionAnalysis()
        analysis.add_content(data)

        frame.asset.add_analysis(self.namespace, analysis)

    def get_proxy_image(self, asset):
        """
        Choose a proper proxy image effort OCR.

        Args:
            asset (Asset): The asset to look at.

        Returns:
            StoredFile: A StoredFile instance.
        """
        ocr_proxy = asset.get_files(category='ocr-proxy')
        if ocr_proxy:
            self.logger.info("OCR proxy detected")
            return file_storage.localize_file(ocr_proxy[0])
        else:
            return get_proxy_level_path(asset, 3)
