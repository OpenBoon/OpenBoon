import tempfile
import logging

import boonflow.proxy as proxy
from boonflow import AssetProcessor, Argument, file_storage
from boonflow.video import extract_thumbnail_from_video
from boonflow.media import media_size, get_output_dimension

from boonai_analysis.utils import simengine

logger = logging.getLogger(__name__)


class ClipAnalysisProcessor(AssetProcessor):
    """
    Creates simhashes and thumbnails for a given array of clips.
    """
    file_types = None

    def __init__(self):
        super(ClipAnalysisProcessor, self).__init__()
        self.add_arg(Argument('clip_id', 'str', required=True))
        self.sim = None

    def init(self):
        self.sim = simengine.SimilarityEngine()

    def process(self, frame):
        clip = self.app.clips.get_clip(self.arg_value('clip_id'))
        asset = self.app.assets.get_asset(clip.asset_id)

        video_path = file_storage.localize_file(proxy.get_video_proxy(asset))
        size = media_size(video_path)
        psize = get_output_dimension(768, size[0], size[1])

        jpg_file = tempfile.mkstemp(".jpg")[1]
        extract_thumbnail_from_video(video_path, jpg_file, clip.start, psize)
        simhash = self.sim.calculate_hash(jpg_file)

        prx = file_storage.projects.store_file(jpg_file, clip, "proxy", "proxy.jpg",
                                               {"width": psize[0], "height": psize[1]})

        req = {'files': [prx], 'simhash': simhash}
        self.app.client.put(f'/api/v1/clips/{clip.id}/_proxy', req)


class MultipleTimelineAnalysisProcessor(AssetProcessor):
    """
    Creates simhashes and thumbnails for all the timelines.
    """
    file_types = None

    def __init__(self):
        super(MultipleTimelineAnalysisProcessor, self).__init__()
        self.add_arg(Argument('timelines', 'dict', required=True))
        self.sim = None

    def init(self):
        self.sim = simengine.SimilarityEngine()

    def process(self, frame):
        timelines = self.arg_value('timelines')
        for asset_id, tls in timelines.items():
            analyze_timelines(self.app, self.sim, asset_id, tls)


class TimelineAnalysisProcessor(AssetProcessor):
    """
    Creates simhashes and thumbnails for the given timeline.
    """

    file_types = None

    def __init__(self):
        super(TimelineAnalysisProcessor, self).__init__()
        self.add_arg(Argument('asset_id', 'str', required=True))
        self.add_arg(Argument('timeline', 'str', required=True))
        self.sim = None

    def init(self):
        self.sim = simengine.SimilarityEngine()

    def process(self, frame):
        asset_id = self.arg_value('asset_id')
        timeline = self.arg_value('timeline')

        analyze_timelines(self.app, self.sim, asset_id, [timeline])


def submit_clip_batch(app, asset_id, batch):
    """
    Submit a batch of clips.

    Args:
        app (BoonApp): An app instance.
        asset_id (str): The asset Id.
        batch (list): A batch of timeline data.

    """
    if batch:
        req = {
            "assetId": asset_id,
            "updates": batch
        }
        app.client.put("/api/v1/clips/_batch_update_proxy", req)


def analyze_timelines(app, sim, asset_id, timelines):
    """
    Analyze all the timelines for the given asset.

    Args:
        app (BoonApp): An App instance.
        sim (SimilarityEngine): The similarity engine for hashes.
        asset_id (str): The asset Id.
        timelines (list): A list of timelines
    """
    query = {
        "query": {
            "bool": {
                "must": [
                    {"term": {"clip.assetId": asset_id}},
                    {"terms": {"clip.timeline": timelines}}
                ]
            }
        },
        "sort": [
            {"clip.start": "asc"}
        ],
        "size": 50
    }

    asset = app.assets.get_asset(asset_id)
    video_path = file_storage.localize_file(proxy.get_video_proxy(asset))

    size = media_size(video_path)
    psize = get_output_dimension(768, size[0], size[1])
    jpg_file = tempfile.mkstemp(".jpg")[1]

    simhash = None
    current_time = None
    batch = {}

    logger.info('Performing deep analysis "{}" for asset "{}"'.format(timelines, asset_id))

    for clip in app.clips.scroll_search(query, timeout="5m"):

        if clip.start != current_time:
            current_time = clip.start

            extract_thumbnail_from_video(video_path, jpg_file, current_time, psize)
            simhash = sim.calculate_hash(jpg_file)

        # Always store the file
        prx = file_storage.projects.store_file(jpg_file, clip, "proxy", "proxy.jpg",
                                               attrs={"width": psize[0], "height": psize[1]},
                                               precache=False)
        if prx:
            batch[clip.id] = {'files': [prx], 'simhash': simhash}

        if len(batch) >= 20:
            submit_clip_batch(app, asset_id, batch)
            batch = {}

    # Add final batch
    submit_clip_batch(app, asset_id, batch)
