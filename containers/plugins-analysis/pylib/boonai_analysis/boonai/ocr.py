import pytesseract

from boonflow.base import AssetProcessor, FileTypes
from boonflow.proxy import get_ocr_proxy_image
from boonflow.analysis import ContentDetectionAnalysis


class ZviOcrProcessor(AssetProcessor):
    """
    Makes a proxy video for a full video file.  Clip assets will reference
    this video file.
    """
    file_types = FileTypes.images | frozenset(['pdf'])
    """Allows PDFs to be processed by OCR which is a required common case."""

    namespace = "boonai-text-detection"

    # Tesseract uses a lot of CPU
    use_threads = False

    def __init__(self):
        super(ZviOcrProcessor, self).__init__()

    def process(self, frame):
        p_path = get_ocr_proxy_image(frame.asset)
        data = pytesseract.image_to_string(p_path)
        data = data.replace('\r', ' ').replace('\n', ' ')

        analysis = ContentDetectionAnalysis()
        analysis.add_content(data)

        frame.asset.add_analysis(self.namespace, analysis)
