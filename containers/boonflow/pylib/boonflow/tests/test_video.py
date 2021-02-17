import os
import tempfile
import unittest
import logging
from unittest.mock import patch

import boonflow.video as video
import boonflow.media as media
from boonflow.testing import test_path, TestAsset
from boonsdk import TimelineBuilder, BoonClient


logging.basicConfig(level=logging.NOTSET)

VIDEO_M4V = test_path('video/sample_ipad.m4v')
VIDEO_MOV = test_path('video/1324_CAPS_23.0_030.00_15_MISC.mov')


def test_extract_thumbnail_from_video():
    dst = tempfile.gettempdir() + '/something.jpg'
    video.extract_thumbnail_from_video(VIDEO_M4V, tempfile.gettempdir() + '/something.jpg', 1)
    assert os.path.exists(dst)


def test_extract_thumbnail_from_video_single():
    dst = tempfile.gettempdir() + "/something.jpg"
    video.extract_thumbnail_from_video(VIDEO_MOV, tempfile.gettempdir() + "/something.jpg", 0)
    assert os.path.exists(dst)


def test_extract_thumbnail_from_video_with_size():
    dst = tempfile.gettempdir() + '/100x100.jpg'
    video.extract_thumbnail_from_video(
        VIDEO_M4V, tempfile.gettempdir() + '/100x100.jpg', 1, size=(100, 100))
    assert os.path.exists(dst)
    assert media.media_size(dst) == (100, 100)


def test_webvtt_builder():
    with video.WebvttBuilder() as vtt:
        vtt.append(0, 10, "hello, you bastard")
    data = open(vtt.path, "r").read()
    assert "00:00:00.000 --> 00:00:10.000" in data
    assert "hello, you bastard" in data


@patch.object(BoonClient, 'post')
def test_save_timeline(post_patch):
    post_patch.return_value = {}
    asset = TestAsset('12345')
    tl = TimelineBuilder(asset, 'zvi-timeline')
    video.save_timeline(asset, tl)

    # Make sure the TL list is added to tmp.
    tmp_tl = asset.get_attr('tmp.timelines')
    assert tmp_tl == ['zvi-timeline']


class FrameExtractors(unittest.TestCase):
    """
    Test the different frame extractors
    """
    def test_time_based_iterate(self):
        iter = video.TimeBasedFrameExtractor(VIDEO_M4V)
        frames = list(iter)
        assert 17 == len(frames)
        assert os.path.exists(frames[0][1])

    def test_shot_based_iterate(self):
        iter = video.ShotBasedFrameExtractor(VIDEO_M4V)
        iter.clean()
        frames = list(iter)
        assert len(frames) > 5
        assert os.path.exists(frames[0][1])
