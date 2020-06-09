from zmlpsdk import Frame
from zmlpsdk.testing import TestAsset, PluginUnitTestCase, zorroa_test_data
from zmlp_core.image.importers import ImageImporter

TOUCAN = zorroa_test_data("images/set01/toucan.jpg")
OFFICE = zorroa_test_data("office/multipage_tiff_small.tif")
GEO_TAG = zorroa_test_data("images/set05/geo_tag_test.jpg")
LGTS_BTY = zorroa_test_data("sequences/full/lgts_bty.16.png")
RLA_FILE = zorroa_test_data("images/set06/ginsu_a_nc10.rla")


class ImageImporterUnitTestCase(PluginUnitTestCase):
    def test_process(self):
        frame = Frame(TestAsset(TOUCAN))
        processor = self.init_processor(ImageImporter(), {})
        processor.process(frame)
        document = frame.asset
        assert document.get_attr('media.width') == 512
        assert document.get_attr('media.height') == 341
        assert document.get_attr('media.timeCreated') == "2009-11-18T16:04:43"
        assert document.get_attr('media.attrs.Exif') is None
        assert document.get_attr('media.attrs.JFIF') is None
        assert document.get_attr('media.attrs.JPEG') is None
        assert document.get_attr('media.attrs.IPTC') is None

    def test_extract_date(self):
        frame = Frame(TestAsset(GEO_TAG))
        processor = self.init_processor(ImageImporter())
        processor.process(frame)
        asset = frame.asset
        assert "2018-05-24T14:56:02" == asset.get_attr("media.timeCreated")

    def test_extact_date_alt_format(self):
        frame = Frame(TestAsset(LGTS_BTY))
        processor = self.init_processor(ImageImporter(), {})
        processor.process(frame)
        document = frame.asset
        assert document.get_attr('media.timeCreated') == "2016-09-22T14:02:54"

    def test_process_multipage_tiff(self):
        frame = Frame(TestAsset(OFFICE))
        processor = self.init_processor(ImageImporter(),
                                        {'extract_pages': True})
        processor.process(frame)
        document = frame.asset
        assert document.get_attr('media.length') == 10
        assert document.get_attr('clip.start') == 1
        assert document.get_attr('clip.stop') == 1
        assert document.get_attr('clip.type') == 'page'
        assert len(processor.reactor.expand_frames) == 9
        for i, expand in enumerate(processor.reactor.expand_frames, 1):
            assert expand[1].asset.clip.start == float(i + 1)

    def test_process_geotagged(self):
        frame = Frame(TestAsset(GEO_TAG))
        processor = self.init_processor(ImageImporter(), {})
        processor.process(frame)
        document = frame.asset
        assert document.get_attr('location.point.lat') == 45.99255
        assert document.get_attr('location.point.lon') == 7.754069444444444

    def test_media_type_set(self):
        frame = Frame(TestAsset(RLA_FILE))
        processor = self.init_processor(ImageImporter(), {})
        processor.process(frame)
        assert frame.asset["media.type"] == "image"
