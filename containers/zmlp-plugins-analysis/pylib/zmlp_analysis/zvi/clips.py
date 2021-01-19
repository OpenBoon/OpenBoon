import tempfile

import zmlpsdk.proxy as proxy
from zmlpsdk import AssetProcessor, Argument, file_storage
from zmlpsdk.video import extract_thumbnail_from_video
from zmlpsdk.media import media_size, get_output_dimension

from zmlp_analysis.utils import simengine


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

        query = {
            "query": {
                "bool": {
                    "must": [
                        {"term": {"clip.assetId": asset_id}},
                        {"term": {"clip.timeline": timeline}}
                    ]
                }
            },
            "sort": [
                {"clip.start": "asc"}
            ],
            "size": 40
        }

        asset = self.app.assets.get_asset(asset_id)
        video_path = file_storage.localize_file(proxy.get_video_proxy(asset))

        size = media_size(video_path)
        psize = get_output_dimension(768, size[0], size[1])
        jpg_file = tempfile.mkstemp(".jpg")[1]

        simhash = None
        current_time = None
        prx = None
        batch = {}

        for clip in self.app.clips.scroll_search(query, timeout="2m"):

            if clip.start != current_time:
                current_time = clip.start

                extract_thumbnail_from_video(video_path, jpg_file, current_time, psize)
                simhash = self.sim.calculate_hash(jpg_file)

            # Always store the file
            prx = file_storage.projects.store_file(jpg_file, clip, "proxy", "proxy.jpg",
                                                   {"width": psize[0], "height": psize[1]})
            if prx:
                batch[clip.id] = {'files': [prx], 'simhash': simhash}

            if len(batch) >= 50:
                self.add_clips(batch)
                batch = {}

        # Add final batch
        self.add_clips(batch)

    def add_clips(self, batch):
        if batch:
            asset_id = self.arg_value('asset_id')
            req = {
                "assetId": asset_id,
                "updates": batch
            }
            self.app.client.put("/api/v1/clips/_batch_update_proxy", req)
