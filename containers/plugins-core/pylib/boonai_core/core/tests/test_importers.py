from unittest.mock import patch

import pytest

from boonai_core.core.importers import FileImportProcessor
from boonai_core.office.importers import OfficerClient, OfficeImporter
from boonflow import Frame, FatalProcessorException
from boonflow.testing import PluginUnitTestCase, TestAsset, test_data
from boonsdk.app import ProjectApp


class FileImportProcessorTests(PluginUnitTestCase):

    @patch.object(ProjectApp, 'get_project')
    def test_process_image(self, _):
        frame = Frame(TestAsset(test_data("images/set01/toucan.jpg")))
        ih = self.init_processor(FileImportProcessor())
        ih.process(frame)

    @patch.object(ProjectApp, 'get_project')
    def test_process_video(self, _):
        frame = Frame(TestAsset(test_data('video/sample_ipad.m4v')))
        ih = self.init_processor(FileImportProcessor())
        ih.process(frame)

    @patch.object(ProjectApp, 'get_project')
    def test_process_gps_point(self, _):
        frame = Frame(TestAsset(test_data('video/sample_ipad.m4v')))
        frame.asset.set_attr('location.point.lat', -37.81)
        frame.asset.set_attr('location.point.lon', 144.96)
        ih = self.init_processor(FileImportProcessor())
        ih.process(frame)

        assert -37.81 == frame.asset.get_attr('location.point.lat')
        assert 144.96 == frame.asset.get_attr('location.point.lon')
        assert 'Melbourne' == frame.asset.get_attr('location.city')
        assert 'AU' == frame.asset.get_attr('location.code')
        assert 'Australia' == frame.asset.get_attr('location.country')

    @patch('boonai_core.office.importers.OfficeImporter.get_metadata', return_value={})
    @patch.object(ProjectApp, 'get_project')
    @patch.object(OfficeImporter, 'get_metadata',
                  return_value={'author': 'Zach', 'content': 'temp'})
    @patch.object(OfficerClient, 'render', return_value='/fake')
    @patch.object(OfficerClient, 'wait_for_rendering', return_value=None)
    @patch.object(OfficerClient, 'get_cache_location', return_value=None)
    def test_process_document(self, _, __, ___, ____, _____, ______):
        frame = Frame(TestAsset(test_data('office/minimal.pdf')))
        frame.asset.set_attr("media.width", 220)
        frame.asset.set_attr("media.height", 170)
        ih = self.init_processor(FileImportProcessor())
        ih.process(frame)

    @patch.object(ProjectApp, 'get_project')
    def test_process_unsupported(self, _):
        frame = Frame(TestAsset(test_data("office/minimal.rar")))
        ih = self.init_processor(FileImportProcessor())
        with pytest.raises(FatalProcessorException):
            ih.process(frame)
