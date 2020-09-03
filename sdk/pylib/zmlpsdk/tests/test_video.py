import os
import tempfile

import zmlpsdk.video as video
from zmlpsdk.testing import zorroa_test_path

VIDEO_M4V = zorroa_test_path('video/sample_ipad.m4v')
VIDEO_MOV = zorroa_test_path('video/1324_CAPS_23.0_030.00_15_MISC.mov')


def test_extract_thumbnail_from_video():
    dst = tempfile.gettempdir() + '/something.jpg'
    video.extract_thumbnail_from_video(VIDEO_M4V, tempfile.gettempdir() + '/something.jpg', 1)
    assert os.path.exists(dst)


def test_extract_thumbnail_from_video_single():
    dst = tempfile.gettempdir() + "/something.jpg"
    video.extract_thumbnail_from_video(VIDEO_MOV, tempfile.gettempdir() + "/something.jpg", 0)
    assert os.path.exists(dst)
