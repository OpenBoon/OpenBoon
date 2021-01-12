from unittest.mock import patch

from zmlp import VideoClip
from zmlp.app import AssetApp, VideoClipApp
from zmlp_analysis.utils.simengine import SimilarityEngine
from zmlp_analysis.zvi import TimelineAnalysisProcessor
from zmlpsdk import Frame
from zmlpsdk import file_storage
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset


class TestTimelineAnalysisProcessor(PluginUnitTestCase):

    @patch.object(TimelineAnalysisProcessor, 'add_clips')
    @patch.object(file_storage.projects, 'store_file')
    @patch.object(VideoClipApp, 'scroll_search')
    @patch.object(file_storage, 'localize_file')
    @patch.object(AssetApp, 'get_asset')
    def test_process(self, get_asset_patch, local_patch,
                     search_patch, store_file_patch, store_clips_patch):
        asset = TestAsset("12345")
        clip = VideoClip({"id": "56789", "start": 1.5})
        SimilarityEngine.default_model_path = zorroa_test_path("models/resnet-152")
        get_asset_patch.return_value = TestAsset("12345")
        local_patch.return_value = zorroa_test_path("video/ted_talk.mp4")
        search_patch.return_value = [clip]
        store_file_patch.return_value = {"id": "jonsnow"}

        processor = self.init_processor(TimelineAnalysisProcessor(), {})
        processor.process(Frame(asset))

        args, kwargs = store_clips_patch.call_args
        print(args)
        assert args[0]['56789']['files'][0]['id'] == 'jonsnow'
        assert args[0]['56789']['simhash'].startswith('OKOPPPN')
