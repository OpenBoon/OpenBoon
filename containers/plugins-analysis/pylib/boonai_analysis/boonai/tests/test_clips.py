from unittest.mock import patch

from boonsdk import VideoClip
from boonsdk.app import AssetApp, VideoClipApp
from boonsdk.client import BoonClient
from boonai_analysis.utils.simengine import SimilarityEngine
from boonai_analysis.zvi import TimelineAnalysisProcessor, ClipAnalysisProcessor, \
    MultipleTimelineAnalysisProcessor
from boonflow import Frame
from boonflow import file_storage
from boonflow.testing import PluginUnitTestCase, test_path, TestAsset


class TestClipAnalysisProcessors(PluginUnitTestCase):

    @patch.object(BoonClient, 'put')
    @patch.object(file_storage.projects, 'store_file')
    @patch.object(VideoClipApp, 'scroll_search')
    @patch.object(file_storage, 'localize_file')
    @patch.object(AssetApp, 'get_asset')
    @patch.object(VideoClipApp, 'get_clip')
    def test_single_clip(self, get_clip_patch, get_asset_patch, local_patch,
                         store_file_patch, store_clips_patch, put_patch):

        get_clip_patch.return_value = VideoClip(
            {'id': '123456', 'start': 1.5, 'assetId': 'abc1234'})
        asset = TestAsset('12345')
        SimilarityEngine.default_model_path = test_path('models/resnet-152')
        get_asset_patch.return_value = TestAsset('12345')
        local_patch.return_value = test_path('video/ted_talk.mp4')
        store_file_patch.return_value = {'id': 'jonsnow'}
        put_patch.return_value = {}
        processor = self.init_processor(ClipAnalysisProcessor(), {})
        processor.process(Frame(asset))

        args, kwargs = put_patch.call_args
        assert args[1]['simhash'].startswith('OKOPPPNPNPPJPPPPPPI')

    @patch('boonai_analysis.zvi.clips.submit_clip_batch')
    @patch.object(file_storage.projects, 'store_file')
    @patch.object(VideoClipApp, 'scroll_search')
    @patch.object(file_storage, 'localize_file')
    @patch.object(AssetApp, 'get_asset')
    def test_single_timeline(self, get_asset_patch, local_patch,
                             search_patch, store_file_patch, store_clips_patch):
        asset = TestAsset('12345')
        clip = VideoClip({'id': '56789', 'start': 1.5})
        SimilarityEngine.default_model_path = test_path('models/resnet-152')
        get_asset_patch.return_value = TestAsset('12345')
        local_patch.return_value = test_path('video/ted_talk.mp4')
        search_patch.return_value = [clip]
        store_file_patch.return_value = {'id': 'jonsnow'}

        processor = self.init_processor(TimelineAnalysisProcessor(), {})
        processor.process(Frame(asset))

        args, kwargs = store_clips_patch.call_args
        assert args[2]['56789']['files'][0]['id'] == 'jonsnow'
        assert args[2]['56789']['simhash'].startswith('OKOPPPN')

    @patch('boonai_analysis.zvi.clips.submit_clip_batch')
    @patch.object(file_storage.projects, 'store_file')
    @patch.object(VideoClipApp, 'scroll_search')
    @patch.object(file_storage, 'localize_file')
    @patch.object(AssetApp, 'get_asset')
    def test_multiple_timeline(self, get_asset_patch, local_patch,
                               search_patch, store_file_patch, store_clips_patch):
        asset = TestAsset('12345')
        clip = VideoClip({'id': '56789', 'start': 1.5})
        SimilarityEngine.default_model_path = test_path('models/resnet-152')
        get_asset_patch.return_value = TestAsset('12345')
        local_patch.return_value = test_path('video/ted_talk.mp4')
        search_patch.return_value = [clip]
        store_file_patch.return_value = {'id': 'jonsnow'}

        processor = self.init_processor(MultipleTimelineAnalysisProcessor(), {
            'timelines': {'12345': ['test-timeline']}
        })
        processor.process(Frame(asset))

        args, kwargs = store_clips_patch.call_args
        assert args[2]['56789']['files'][0]['id'] == 'jonsnow'
        assert args[2]['56789']['simhash'].startswith('OKOPPPN')
