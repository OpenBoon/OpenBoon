import tempfile
import os
from pathlib import Path

from zmlp import Clip
from zmlpsdk.storage import file_storage
from zmlpsdk.base import AssetProcessor, ZmlpProcessorException, FileTypes, \
    ZmlpFatalProcessorException
import zmlpsdk.video as video
from ..util.media import get_video_metadata, set_resolution_attrs


class VideoImporter(AssetProcessor):
    """Processor that handles ingestion of video files. This processor will add video
    file related metadata to the the media namespace. Additionally it will handle breaking
    the video into individual clips and creating web proxies.
    """

    file_types = FileTypes.videos

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
        has_media_type = asset.attr_exists('media.type')

        # If there is no media type, then we have to
        # fetch metadata for this file.
        if not has_media_type:
            path = file_storage.localize_file(asset)
            probe = get_video_metadata(path)

            # Required attributes
            for key in ['width', 'height', 'length']:
                try:
                    asset.set_attr("media.{}".format(key), probe[key])
                except KeyError:
                    raise ZmlpFatalProcessorException(
                        'Unable to determine failure for {}'.format(key))

            for key in ['description', 'title', 'timeCreated']:
                asset.set_attr("media.{}".format(key), probe.get(key))

            set_resolution_attrs(asset, probe.get('width'), probe.get('height'))

            # Everything has a clip, even if it's the whole movie.
            # Only add the clip if we didn't have have it.
            if not has_clip:
                # Since there is no clip, then set a clip, as all pages
                # need to have a clip.
                asset.set_attr('clip', Clip.scene(0.0, probe['length'], 'full'))

            # Set this last.
            asset.set_attr('media.type', 'video')

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
        source_path = Path(file_storage.localize_file(asset))
        destination_path = Path(tempfile.mkdtemp('video_ingestor'), asset.id + '.jpg')

        # If the thumbnail fails to generate at the specified point, then
        # fallback to 0.
        final_error = None
        for try_seconds in [seconds, 0]:
            try:
                video.extract_thumbnail_from_video(source_path, destination_path, try_seconds)
                break
            except IOError as e:
                final_error = e

        if not os.path.exists(destination_path):
            raise ZmlpProcessorException(
                'Unable to extract video proxy image for {}, unexpected {}'.format(
                    destination_path, final_error))

        asset.set_attr('tmp.proxy_source_image', str(destination_path))
        asset.set_attr('tmp.image_proxy_source_attrs', {'time_offset': seconds})
