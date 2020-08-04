import os
from unittest.mock import patch

from google.cloud.videointelligence_v1.proto import video_intelligence_pb2

from zmlp_analysis.google.cloud_video import AsyncVideoIntelligenceProcessor
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_prediction_labels

client_las = 'zmlp_analysis.google.cloud_video.initialize_gcp_client'


class MockVideoIntelligenceClient:
    def __init__(self, *args, **kwargs):
        pass


class AsyncVideoIntelligenceProcessorTestCase(PluginUnitTestCase):

    @patch('zmlp_analysis.google.cloud_video.initialize_gcp_client',
           side_effect=MockVideoIntelligenceClient)
    @patch('zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.'
           '_get_video_annotations')
    @patch('zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.'
           'get_video_proxy_uri')
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_timeline')
    def test_detect_logos(self, store_tl_patch,
                           store_blob_patch, proxy_patch, annot_patch, client_patch):
        uri = 'gs://zorroa-dev-data/video/mustang.mp4'
        store_tl_patch.return_value = None
        store_blob_patch.return_value = None
        annot_patch.return_value = self.load_results("detect-logos.dat")
        proxy_patch.return_value = uri
        processor = self.init_processor(
            AsyncVideoIntelligenceProcessor(), {
                'detect_logos': 0.5,
            })

        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        asset.set_attr('clip.track', 'full')
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-logo-detection')
        assert 'labels' == analysis['type']
        assert 'Volvo' in get_prediction_labels(analysis)
        assert 'Ford Motor Company' in get_prediction_labels(analysis)
        assert 16 == analysis['count']

    @patch('zmlp_analysis.google.cloud_video.initialize_gcp_client',
           side_effect=MockVideoIntelligenceClient)
    @patch('zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.'
           '_get_video_annotations')
    @patch('zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.'
           'get_video_proxy_uri')
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_timeline')
    def test_detect_labels(self, store_tl_patch,
                           store_blob_patch, proxy_patch, annot_patch, client_patch):
        uri = 'gs://zorroa-dev-data/video/ted_talk.mp4'
        store_tl_patch.return_value = None
        store_blob_patch.return_value = None
        annot_patch.return_value = self.load_results("detect-labels.dat")
        proxy_patch.return_value = uri

        processor = self.init_processor(
            AsyncVideoIntelligenceProcessor(), {
                'detect_labels': 0.5
            })

        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        asset.set_attr('clip.track', 'full')
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-label-detection')
        assert 'labels' == analysis['type']
        assert 'stage' in get_prediction_labels(analysis)
        assert 14 == analysis['count']

    @patch('zmlp_analysis.google.cloud_video.initialize_gcp_client',
           side_effect=MockVideoIntelligenceClient)
    @patch('zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.'
           '_get_video_annotations')
    @patch('zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.'
           'get_video_proxy_uri')
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_timeline')
    def test_detect_text(self, store_tl_patch,
                         store_blob_patch, proxy_patch, annot_patch, client_patch):
        uri = 'gs://zorroa-dev-data/video/ted_talk.mp4'
        store_tl_patch.return_value = None
        store_blob_patch.return_value = None
        annot_patch.return_value = self.load_results("detect-text.dat")
        proxy_patch.return_value = uri

        processor = self.init_processor(
            AsyncVideoIntelligenceProcessor(), {
                'detect_text': 0.5
            })

        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        asset.set_attr('clip.track', 'full')
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-text-detection')
        assert 'content' == analysis['type']
        assert 'sanitation, toilets and poop' in analysis['content']
        assert 20 == analysis['words']

    @patch('zmlp_analysis.google.cloud_video.initialize_gcp_client',
           side_effect=MockVideoIntelligenceClient)
    @patch('zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.'
           '_get_video_annotations')
    @patch('zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.'
           'get_video_proxy_uri')
    @patch.object(file_storage.assets, 'store_blob')
    def test_detect_objects(self, blob_patch, proxy_patch, annot_patch, client_patch):
        uri = 'gs://zorroa-dev-data/video/ted_talk.mp4'
        blob_patch.return_value = None
        annot_patch.return_value = self.load_results("detect-objects.dat")
        proxy_patch.return_value = uri

        processor = self.init_processor(AsyncVideoIntelligenceProcessor(), {
            'detect_objects': 0.25,
        })

        uri = 'gs://zorroa-dev-data/video/model.mp4'
        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        asset.set_attr('clip.track', 'full')
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-object-detection')
        assert 'labels' == analysis['type']
        assert 'swimwear' in get_prediction_labels(analysis)
        assert 'person' in get_prediction_labels(analysis)
        assert 4 == analysis['count']

    @patch('zmlp_analysis.google.cloud_video.initialize_gcp_client',
           side_effect=MockVideoIntelligenceClient)
    @patch('zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.'
           '_get_video_annotations')
    @patch('zmlp_analysis.google.cloud_video.AsyncVideoIntelligenceProcessor.'
           'get_video_proxy_uri')
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_timeline')
    def test_detect_explicit(self, store_tl_patch,
                             blob_patch, proxy_patch, annot_patch, client_patch):
        uri = 'gs://zorroa-dev-data/video/model.mp4'
        store_tl_patch.return_value = None
        blob_patch.return_value = None
        annot_patch.return_value = self.load_results("detect-explicit.dat")
        proxy_patch.return_value = uri

        processor = self.init_processor(AsyncVideoIntelligenceProcessor(), {
            'detect_explicit': 3,
        })

        asset = TestAsset(uri)
        asset.set_attr('media.length', 15.0)
        asset.set_attr('clip.track', 'full')
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-video-explicit-detection')
        assert 'labels' == analysis['type']
        assert 'very_unlikely' in get_prediction_labels(analysis)
        assert 'unlikely' in get_prediction_labels(analysis)
        assert 2 == analysis['count']
        assert not analysis['explicit']

    def load_results(self, name):
        rsp = video_intelligence_pb2.AnnotateVideoResponse()
        with open(os.path.dirname(__file__) + "/mock-data/{}".format(name), 'rb') as fp:
            rsp.ParseFromString(fp.read())
        return rsp.annotation_results[0]
