
from zplugins.video.processors import SRTReadProcessor
from zsdk import Frame, Document
from zsdk.testing import PluginUnitTestCase, zorroa_test_data


class SRTReadProcessorUnitTest(PluginUnitTestCase):
    def setUp(self):
        self.test_file_directory = zorroa_test_data('srt')
        self.processor = SRTReadProcessor()
        self.init_processor(self.processor)

    def test_process(self):
        frame = Frame(Document())
        frame.asset.set_attr('media.clip', {'type': 'video', 'start': 0.0, 'stop': 7.0})
        frame.asset.set_attr('source', {'directory': self.test_file_directory,
                                        'basename': 'srt_sample'})
        self.processor.process(frame)
        self.assertEqual(frame.asset.get_attr('media.dialog'),
                         [[u"In this lesson, we're going to", u'be talking about finance. And'],
                          [u'one of the most important aspects', u'of finance is interest.']])

        frame = Frame(Document())
        frame.asset.set_attr('media.clip', {'type': 'video', 'start': 0.0, 'stop': 120.0})
        frame.asset.set_attr('source', {'directory': self.test_file_directory,
                                        'basename': 'srt_sample'})
        self.processor.process(frame)
        self.assertEqual(len(frame.asset.get_attr('media.dialog')), 23)
