
from pathlib2 import Path

from zplugins.video.importers import VideoImporter
from zorroa.zsdk import Frame, Asset
from zorroa.zsdk.exception import ProcessorException
from zorroa.zsdk.testing import PluginUnitTestCase, zorroa_test_data


class VideoImporterUnitTestCase(PluginUnitTestCase):

    def setUp(self):
        self.movie_path = zorroa_test_data('video/sample_ipad.m4v')
        self.frame = Frame(Asset(self.movie_path))
        self.processor = self.init_processor(VideoImporter(), {})

    def test_set_media_metadata(self):
        asset = self.frame.asset
        self.processor._set_media_metadata(asset)
        expected_media = {'audioBitRate': u'107313',
                          'description': u'A short description of luke sledding in winter.',
                          'title': u'Luke crashes sled', 'duration': 15.048367,
                          'frameRate': 29.97, 'height': 360, 'width': 640,
                          'audioChannels': 2, 'frames': 450, 'orientation': 'landscape',
                          'aspect': 1.78}
        assert asset.get_attr('media') == expected_media

    def test_create_proxy_source_image(self):
        asset = self.frame.asset
        self.processor._create_proxy_source_image(asset)
        assert Path(asset.get_attr('tmp.proxy_source_image')).suffix == '.jpg'

    def test_create_proxy_source_failure(self):
        path = zorroa_test_data('office/simple.pdf')
        asset = Frame(Asset(path)).asset
        self.assertRaises(ProcessorException, self.processor._create_proxy_source_image, asset)

    def test_disable_extract_proxy_source(self):
        asset = self.frame.asset
        processor = self.init_processor(VideoImporter(),
                                        {'enable_proxy_transcoder': False,
                                         'enable_clipifier': False,
                                         'enable_extract_proxy_image': False})

        processor.process(self.frame)
        assert not asset.get_attr('tmp.proxy_source_image')

    def test_process(self):
        self.processor.process(self.frame)

        # Verify proxy source is created.
        assert Path(self.frame.asset.get_attr('tmp.proxy_source_image')).suffix == '.jpg'

    def test_process_with_clipify(self):
        self.processor.arg("enable_clipifier").value = True
        self.processor.process(self.frame)

        # Verify clips are correct.
        assert len(self.processor.reactor.expand_frames) == 6

    def test_single_frame_movie(self):
        movie_path = zorroa_test_data('video/1324_CAPS_23.0_030.00_15_MISC.mov')
        frame = Frame(Asset(movie_path))
        self.processor._create_proxy_source_image(frame.asset)
        path = Path(frame.asset.get_attr('tmp.proxy_source_image'))
        assert path.exists()
        assert path.suffix == '.jpg'

    def test_process_mxf(self):
        movie_path = zorroa_test_data('mxf/freeMXF-mxf1.mxf')
        asset = Asset(movie_path)
        frame = Frame(asset)
        self.processor.process(frame)

        assert "video/mxf" == asset.get_attr("source.mediaType")
        assert "video" == asset.get_attr("source.type")
        assert "mxf" == asset.get_attr("source.subType")
        assert 720 == asset.get_attr("media.width")
        assert 576 == asset.get_attr("media.height")
        assert 25.0 == asset.get_attr("media.frameRate")
        assert 10.72 == asset.get_attr("media.duration")
        assert 1.25 == asset.get_attr("media.aspect")
        assert 268 == asset.get_attr("media.frames")

        # Verify proxy source is created.
        assert Path(asset.get_attr('tmp.proxy_source_image')).suffix == '.jpg'
