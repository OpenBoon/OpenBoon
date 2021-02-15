import pytest
from pathlib import Path
from unittest.mock import patch

from boonai_core.video.importers import VideoImporter
from boonflow import Frame, ProcessorException, FatalProcessorExceptionfrom boonflow.testing import TestAsset, PluginUnitTestCase, test_data
from boonsdk.app import ProjectApp
from boonsdk import Project


class VideoImporterUnitTestCase(PluginUnitTestCase):

    @patch.object(ProjectApp, 'get_project')
    def setUp(self, get_project_patch):
        get_project_patch.return_value = Project(
            {"id": "1234", "name": "foo", "tier": "PREMIER"})
        self.movie_path = test_data('video/sample_ipad.m4v')
        self.frame = Frame(TestAsset(self.movie_path))
        self.processor = self.init_processor(VideoImporter(), {})

    def test_set_media_metadata(self):
        asset = self.frame.asset
        self.processor._set_media_metadata(asset)
        expected_media = {
            'description': u'A short description of luke sledding in winter.',
            'title': u'Luke crashes sled', 'length': 15.048367,
            'height': 360, 'width': 640,
            'orientation': 'landscape',
            'aspect': 1.78, 'type': 'video',
            'videoCodec': 'h264',
            'timeCreated': '2016-04-08T15:02:31.000000Z'
        }

        assert asset.get_attr('media') == expected_media

    def test_skip_set_media_metadata(self):
        # If media.type is set then extracting
        # media attrs is skipped.
        asset = self.frame.asset
        asset.set_attr('media.type', 'video')
        self.processor._set_media_metadata(asset)
        expected_media = {
            'type': 'video'
        }
        assert asset.get_attr('media') == expected_media

    def test_create_proxy_source_image(self):
        asset = self.frame.asset
        asset.set_attr('media.length', 10)
        self.processor._create_proxy_source_image(asset)
        assert Path(asset.get_attr('tmp.proxy_source_image')).suffix == '.jpg'

    def test_create_proxy_source_failure(self):
        path = test_data('office/simple.pdf')
        asset = Frame(TestAsset(path)).asset
        self.assertRaises(ProcessorException, self.processor._create_proxy_source_image, asset)

    def test_process(self):
        self.processor.process(self.frame)
        print(self.frame.asset.document)
        # Verify proxy source is created.
        assert Path(self.frame.asset.get_attr('tmp.proxy_source_image')).suffix == '.jpg'

    def test_single_frame_movie(self):
        movie_path = test_data('video/1324_CAPS_23.0_030.00_15_MISC.mov')
        frame = Frame(TestAsset(movie_path))
        self.processor.process(frame)

        path = Path(frame.asset.get_attr('tmp.proxy_source_image'))
        assert path.exists()
        assert path.suffix == '.jpg'

    def test_process_mxf(self):
        movie_path = test_data('mxf/freeMXF-mxf1.mxf')
        asset = TestAsset(movie_path)
        frame = Frame(asset)
        self.processor.process(frame)

        assert 'video' == asset.get_attr('media.type')
        assert 'mxf' == asset.get_attr('source.extension')
        assert 720 == asset.get_attr('media.width')
        assert 576 == asset.get_attr('media.height')
        assert 10.72 == asset.get_attr('media.length')
        assert 1.25 == asset.get_attr('media.aspect')

        # Verify proxy source is created.
        assert Path(asset.get_attr('tmp.proxy_source_image')).suffix == '.jpg'

    @patch.object(ProjectApp, 'get_project')
    def test_process_fail_on_premier_check(self, get_project_patch):
        get_project_patch.return_value = Project(
            {"id": "1234", "name": "foo", "tier": "ESSENTIALS"})

        movie_path = test_data('mxf/freeMXF-mxf1.mxf')
        asset = TestAsset(movie_path)
        frame = Frame(asset)
        processor = self.init_processor(VideoImporter(), {})

        with pytest.raises(FatalProcessorException):
            processor.process(frame)
