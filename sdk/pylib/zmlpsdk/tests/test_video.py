import os
import tempfile
import unittest
from unittest.mock import patch

import zmlpsdk.video as video
from zmlpsdk.testing import zorroa_test_path, TestAsset, get_mock_stored_file

VIDEO_M4V = zorroa_test_path('video/sample_ipad.m4v')
VIDEO_MOV = zorroa_test_path('video/1324_CAPS_23.0_030.00_15_MISC.mov')


class ShowBasedClipGeneratorTests(unittest.TestCase):

    def test_shot_based_clip_generator(self):
        gen = video.ShotBasedClipGenerator(VIDEO_M4V)
        count = 0
        for _ in gen:
            count += 1
        assert 2 == count

    def test_shot_based_clip_generator_with_min(self):
        gen = video.ShotBasedClipGenerator(zorroa_test_path(VIDEO_M4V), 1)
        count = 0
        for _ in gen:
            count += 1
        assert 6 == count


class TimeBasedClipGeneratorTests(unittest.TestCase):

    def test_time_based_clip_generator(self):
        gen = video.TimeBasedClipGenerator(zorroa_test_path(VIDEO_M4V), 1)
        count = 0
        for _ in gen:
            count += 1
        assert count == 16


class VideoFrameIteratorTests(unittest.TestCase):

    def test_video_frame_iterator(self):
        path = zorroa_test_path('video/FatManOnABike1914.mp4')
        gen = video.ShotBasedClipGenerator(path, 10)
        iter = video.VideoFrameIterator(gen)
        for clip, path in iter:
            print(clip)
            print(path)


def test_extract_thumbnail_from_video():
    dst = tempfile.gettempdir() + '/something.jpg'
    video.extract_thumbnail_from_video(VIDEO_M4V, tempfile.gettempdir() + '/something.jpg', 1)
    assert os.path.exists(dst)


def test_extract_thumbnail_from_video_single():
    dst = tempfile.gettempdir() + "/something.jpg"
    video.extract_thumbnail_from_video(VIDEO_MOV, tempfile.gettempdir() + "/something.jpg", 0)
    assert os.path.exists(dst)


@patch('zmlpsdk.video.get_proxy_level_path')
def test_check_video_clip_preconditions_true(proxy_patch):
    stored_file = get_mock_stored_file('proxy', 'video/mp4')
    asset = TestAsset(VIDEO_M4V)
    asset.set_attr('files', [stored_file._data])
    proxy_patch.return_value = stored_file

    asset.set_attr('media.type', 'video')
    asset.set_attr('clip.timeline', 'full')
    assert video.check_video_clip_preconditions(asset)

    asset.set_attr('media.type', 'image')
    asset.set_attr('clip.timeline', 'full')
    assert not video.check_video_clip_preconditions(asset)

    asset.set_attr('media.type', 'video')
    asset.set_attr('clip.timeline', 'shot')
    assert not video.check_video_clip_preconditions(asset)


@patch('zmlpsdk.video.get_proxy_level_path')
def test_check_video_clip_preconditions_no_proxy(proxy_patch):
    asset = TestAsset(VIDEO_M4V)
    proxy_patch.return_value = None

    asset.set_attr('media.type', 'video')
    asset.set_attr('clip.timeline', 'full')
    assert not video.check_video_clip_preconditions(asset)


def test_make_video_clip_expand_frame():
    asset = TestAsset(VIDEO_M4V)
    frame = video.make_video_clip_expand_frame(asset, 100, 120, 'shot')
    assert 100.0 == frame.asset.clip.start
    assert 120.0 == frame.asset.clip.stop
    assert 'shot' == frame.asset.clip.timeline
