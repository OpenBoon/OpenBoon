import re
import os
import pysrt

from zsdk import DocumentProcessor, Argument


class SRTReadProcessor(DocumentProcessor):
    """Reads an SRT file associated with a clip and adds dialogue metadata.

    Optional Args:
        time_offset (float): Offsets the dialog timecodes by this many seconds.

    """
    tool_tips = {
        'time_offset': 'Offsets the dialog timecodes by this many seconds.'
    }

    def __init__(self):
        super(SRTReadProcessor, self).__init__()
        self.add_arg(Argument("time_offset", "float", default=0, required=False,
                              toolTip=self.tool_tips['time_offset']))

    def cleanhtml(self, raw_html):
        cleanr = re.compile('<.*?>')
        cleantext = re.sub(cleanr, '', raw_html)
        return cleantext

    def _process(self, frame):
        asset = frame.asset
        required_metadata = ['media.clip.start', 'media.clip.stop', 'media.clip.type']
        for key in required_metadata:
            if asset.get_attr(key) is None:
                return
        if asset.get_attr("media.clip.type") != "video":
            return
        srt_file = "%s/%s.srt" % (asset.get_attr("source.directory"),
                                  asset.get_attr("source.basename"))
        self.logger.info("SRT FILE: " + srt_file)
        if not os.path.exists(srt_file):
            return
        subs = pysrt.open(srt_file, encoding='iso-8859-1')
        subs.shift(seconds=self.arg_value("time_offset"))
        start = asset.get_attr('media.clip.start')
        stop = asset.get_attr('media.clip.stop')
        subs_thisclip = subs.slice(ends_after={'seconds': start + 0.5},
                                   starts_before={'seconds': stop - 0.5})
        dialog = []
        for s in subs_thisclip:
            dialog.append(self.cleanhtml(s.text).split('\n'))
        asset.set_attr('media.dialog', dialog)
