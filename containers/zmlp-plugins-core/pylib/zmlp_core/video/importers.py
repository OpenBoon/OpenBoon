import tempfile
import os
from pathlib2 import Path

from zmlp.asset import Clip
from zmlp.analysis.storage import file_storage
from zmlp.analysis.base import AssetBuilder, ZmlpProcessorException
from ..util.media import get_video_metadata, create_video_thumbnail, set_resolution_attrs


class VideoImporter(AssetBuilder):
    """Processor that handles ingestion of video files. This processor will add video
    file related metadata to the the media namespace. Additionally it will handle breaking
    the video into individual clips and creating web proxies.
    """

    file_types = ['mov', 'mp4', 'mpg', 'mpeg', 'm4v', 'webm', 'ogv', 'ogg', 'mxf']

    def __init__(self):
        super(VideoImporter, self).__init__()

    def process(self, frame):
        asset = frame.asset
        self._set_media_metadata(asset)
        self._create_proxy_source_image(asset)

    def _set_media_metadata(self, asset):
        """Introspects a video file and adds metadata to the media namespace on the asset.

        Args:
            asset (Asset): Asset to add metadata to.

        """
        has_clip = asset.attr_exists('clip')

        # If we don't have a clip or its the full video timeline
        # then we can make a proxy.  Otherwise, don't make a proxy file.
        # Other clip will re-use use this proxy and all the metadata.
        if not has_clip or asset.get_attr('clip.timelime' == 'full'):

            path = file_storage.localize_remote_file(asset)
            probe = get_video_metadata(path)
            asset.set_attr('media.type', 'video')

            for key in ('description', 'title', 'width', 'height', 'length', 'createdTime'):
                asset.set_attr("media.{}".format(key), probe[key])

            if probe.get('width') and probe.get('height'):
                set_resolution_attrs(asset, probe.get('width'), probe.get('height'))

            # Everything has a clip, even if it's the whole movie.
            # Only add the clip if we didn't have have it.
            if not has_clip:
                # Since there is no clip, then set a clip, as all pages
                # need to have a clip.
                asset.set_attr('clip', Clip.scene(0.0, probe['length'], 'full'))

    def _create_proxy_source_image(self, asset):
        """Creates a source image to be used by the ProxyIngestor.

        Args:
            asset (Asset): Asset to create a thumbnail for.

        """
        if not asset.attr_exists('clip.start') or not asset.attr_exists('clip.stop'):
            raise ZmlpProcessorException('Cannot make image proxy, no clip defined')

        # Determine the second at which to pull the thumbnail from the video.
        # Takes the screenshot from middle of movie.
        # Cannot use clip.length because it might not be set at this point.
        seconds = round(
            max(0, (asset.get_attr('clip.stop') - asset.get_attr('clip.start')) / 2.0), 2)
        source_path = Path(file_storage.localize_remote_file(asset))
        destination_path = Path(tempfile.mkdtemp('video_ingestor'), asset.id + '.jpg')

        # If the thumbnail fails to generate at the specified point, then
        # fallback to 0.
        final_error = None
        for try_seconds in [seconds, 0]:
            try:
                create_video_thumbnail(source_path, destination_path, try_seconds)
                break
            except IOError as e:
                final_error = e

        if not os.path.exists(destination_path):
            raise ZmlpProcessorException(
                'Unable to extract video proxy image for {}, unexpected {}'.format(
                    destination_path, final_error))

        asset.set_attr('tmp.proxy_source_image', str(destination_path))
        asset.set_attr('tmp.proxy_source_attrs', {'time_offset': seconds})
