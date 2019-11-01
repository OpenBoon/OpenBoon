from datetime import datetime
from pathlib2 import Path
from PIL import Image

import zsdk
from zsdk import Asset, Frame
from zsdk.testing import PluginUnitTestCase, zorroa_test_data
from zsdk.util import clean_export_root_dir
from zplugins.image.processors import HSVSimilarityProcessor
from zplugins.image.importers import ImageImporter
from zplugins.image.exporters import ImageExporter
from zplugins.util.proxy import add_proxy_file


TOUCAN = zorroa_test_data("images/set01/toucan.jpg")
OFFICE = zorroa_test_data("office/multipage_tiff_small.tif")
GEO_TAG = zorroa_test_data("images/set05/geo_tag_test.jpg")
LGTS_BTY = zorroa_test_data("sequences/full/lgts_bty.16.png")
RLA_FILE = zorroa_test_data("images/set06/ginsu_a_nc10.rla")


class HSVSimilarityProcessorUnitTestCase(PluginUnitTestCase):
    def test_HsvHash_defaults(self):
        asset = zsdk.Asset(TOUCAN)
        frame = zsdk.Frame(asset)
        add_proxy_file(asset, TOUCAN)
        ih = self.init_processor(HSVSimilarityProcessor(), {})
        ih.process(frame)

        # NOTE: Slightly different hashes are returned when run on MacOS vs Centos7.
        self.assertIn(frame.asset.get_attr("analysis")["hueSimilarity"]["shash"],
                      ["fheieddebaab", "fheieddecaab"])


class ImageImporterUnitTestCase(PluginUnitTestCase):
    def test_process(self):
        frame = zsdk.Frame(zsdk.Asset(TOUCAN))
        processor = self.init_processor(ImageImporter(), {})
        processor.process(frame)
        document = frame.asset
        assert document.get_attr('media.width') == 512
        assert document.get_attr('media.height') == 341
        assert document.get_attr('media.timeCreated') == datetime(2009, 11, 18, 16, 0o4, 43)
        assert document.get_attr('media.attrs.Exif') is None
        assert document.get_attr('media.attrs.JFIF') is None
        assert document.get_attr('media.attrs.JPEG') is None
        assert document.get_attr('media.attrs.IPTC') is None

    def test_process_extended_metadata(self):
        frame = zsdk.Frame(zsdk.Asset(TOUCAN))
        processor = self.init_processor(ImageImporter(),
                                        {'extract_extended_metadata': True})
        processor.process(frame)
        document = frame.asset
        assert document.get_attr('media.width') == 512
        assert document.get_attr('media.height') == 341
        assert document.get_attr('media.attrs.Model') == 'Canon EOS 400D DIGITAL'
        assert document.get_attr('media.attrs.Make') == 'Canon'
        assert document.get_attr('media.attrs.Orientation') == 'normal'
        assert document.get_attr('media.attrs.ExposureBiasValue') == 0.0
        assert document.get_attr('media.attrs.FocalLength') == '220 mm'

    def test_process_multipage_tiff(self):
        frame = zsdk.Frame(zsdk.Asset(OFFICE))
        processor = self.init_processor(ImageImporter(),
                                        {'extract_pages': True})
        processor.process(frame)
        document = frame.asset
        assert document.get_attr('media.pages') == 10
        assert len(processor.reactor.expand_frames) == 10
        for i, expand in enumerate(processor.reactor.expand_frames, 1):
            assert expand[1].asset.get_attr('media.clip.start') == i

    def test_process_geotagged(self):
        frame = zsdk.Frame(zsdk.Asset(GEO_TAG))
        processor = self.init_processor(ImageImporter(), {})
        processor.process(frame)
        document = frame.asset
        assert document.get_attr('media.latitude') == 45.99255
        assert document.get_attr('media.longitude') == 7.754069444444444

    def test_date_metadata_bug(self):
        frame = zsdk.Frame(zsdk.Asset(LGTS_BTY))
        processor = self.init_processor(ImageImporter(), {})
        processor.process(frame)
        document = frame.asset
        assert document.get_attr('media.timeCreated') == datetime(2016, 9, 22, 14, 2, 54)

    def test_bad_media_type(self):
        frame = zsdk.Frame(zsdk.Asset(RLA_FILE))
        processor = self.init_processor(ImageImporter(), {})
        processor.process(frame)
        document = frame.asset
        assert document.get_attr("source.mediaType") == "image/x-rla"
        assert document.get_attr("source.type") == "image"
        assert document.get_attr("source.subType") == "x-rla"


class ImageExporterUnitTestCase(PluginUnitTestCase):
    def setUp(self):
        super(ImageExporterUnitTestCase, self).setUp()
        clean_export_root_dir()
        self.asset = zsdk.Asset(TOUCAN)
        self.frame = zsdk.Frame(self.asset)

    def test_process_larger_source_image(self):
        processor = self.init_processor(ImageExporter(), {'size': 256})
        processor.process(self.frame)
        destination_path = Path(processor.export_root_dir, 'toucan.jpg')
        assert self.frame.asset.get_attr('exported.path') == str(destination_path)
        assert Path(destination_path).exists()
        exported_image = Image.open(str(destination_path))
        assert exported_image.width == 256
        assert exported_image.height == 170

    def test_process_smaller_source_image(self):
        processor = self.init_processor(ImageExporter(), {'size': 1024})
        processor.process(self.frame)
        destination_path = Path(processor.export_root_dir, 'toucan.jpg')
        assert self.frame.asset.get_attr('exported.path') == str(destination_path)
        assert Path(destination_path).exists()
        exported_image = Image.open(str(destination_path))
        assert exported_image.width == 512
        assert exported_image.height == 341

    def test_process_original_image(self):
        processor = self.init_processor(ImageExporter(), {'size': 100, 'export_original': True})
        processor.process(self.frame)
        destination_path = Path(processor.export_root_dir, 'toucan.jpg')
        assert self.frame.asset.get_attr('exported.path') == str(destination_path)
        assert Path(destination_path).exists()
        exported_image = Image.open(str(destination_path))
        assert exported_image.width == 512
        assert exported_image.height == 341
