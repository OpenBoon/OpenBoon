import pytesseract

from zmlpsdk.base import AssetProcessor, FileTypes
from zmlpsdk.proxy import get_proxy_level_path


class ZviOcrProcessor(AssetProcessor):
    """
    Makes a proxy video for a full video file.  Clip assets will reference
    this video file.
    """
    file_types = FileTypes.images

    namespace = "zvi-text-detection"

    def __init__(self):
        super(ZviOcrProcessor, self).__init__()

    def process(self, frame):
        p_path = get_proxy_level_path(frame.asset, 3)
        data = pytesseract.image_to_string(p_path)
        data = data.replace('\r', '').replace('\n', '')
        frame.asset.add_analysis(self.namespace, {
            'type': 'content',
            'content': data,
            'count': len(data.split())
        })
