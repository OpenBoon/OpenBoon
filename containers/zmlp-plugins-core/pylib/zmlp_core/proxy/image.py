import collections
import logging
import os
import shutil
import subprocess
import tempfile
from pathlib import Path

from zmlpsdk import AssetProcessor, Argument, StopWatch
from zmlpsdk.storage import file_storage
from ..util.media import get_output_dimension, media_size, store_media_proxy

logger = logging.getLogger(__file__)


class ImageProxyProcessor(AssetProcessor):
    toolTips = {
        'sizes': 'Sizes of the proxies to create.',
        'web_proxy_size': 'Size of web proxy'
    }

    ml_quality = "95"
    web_quality = "85"

    multipage_formats = ['tiff', 'tif']
    """Multipage image formats that have to be handled specifically for web proxies."""

    use_threads = False

    def __init__(self):
        super(ImageProxyProcessor, self).__init__()
        self.created_proxy_count = 0
        # Make sure largest proxy comes first.
        self.add_arg(Argument('sizes', 'list[int]', default=[1280, 512],
                              toolTip=self.toolTips['sizes']))
        self.add_arg(Argument('web_proxy_size', 'int', default=1024,
                              toolTip=self.toolTips['web_proxy_size']))

    def process(self, frame):
        # Inherits parent docstring.
        asset = frame.asset
        source_path = self._get_source_path(asset)

        # If we can't make a proxy just log it and move on.  Proxies
        # can be made with ExistingProxyIngestor as well.
        # We want to avoid generating error by trying to use oiio
        # on word docs and stuff like that.
        # In the future the server side will detect missing proxy images.
        if not source_path:
            self.logger.info('No suitable proxy path found for asset "{}"'
                             .format(asset.get_attr('source.path')))
            return

        proxy_paths = self._create_proxy_images(asset)
        for width, height, path in proxy_paths:
            store_media_proxy(asset, path, 'image', (width, height))

        # Make web optimized
        width, height, _ = proxy_paths[0]
        self.make_web_optimized_proxy(asset, source_path, (width, height))

    def _create_proxy_images(self, asset):
        """
        Creates the proxy images.

        Args:
            asset(Document): Document that is being processed.

        Returns:
            list<str>: Paths to the proxies that were created.

        """
        proxy_descriptors = self._get_proxy_descriptors(asset)
        oiiotool_command = self._get_oiio_command_line(asset, proxy_descriptors)
        # Create the parent directory of the tmp output location of each proxy.
        for (_, _, output_path) in proxy_descriptors:
            if not output_path.parent.exists():
                output_path.parent.mkdir(parents=True)

        # If we detect only 1 proxy and the original file is a JPG, our newly made proxy
        # is usually going to be bigger than the source, so we just copy it.
        if len(proxy_descriptors) == 1 and asset.get_attr("source.extension").lower() == "jpg":
            output_path = str(proxy_descriptors[0][2])
            shutil.copy(file_storage.localize_file(asset), output_path)
        elif proxy_descriptors:
            self.logger.debug('create proxies: %s' % oiiotool_command)
            with StopWatch("Create ML proxies"):
                subprocess.check_call(oiiotool_command,
                                      stdout=subprocess.DEVNULL,
                                      stderr=subprocess.DEVNULL)
            self.created_proxy_count += len(proxy_descriptors)
        else:
            self.logger.info('All proxies already exist. No proxies will be created.')

        return proxy_descriptors

    def _get_oiio_command_line(self, asset, proxy_descriptors):
        """
        Compose the OIIO tool command line for generating all the necessary proxy files.

        Args:
            asset: The asset currently being processed.
            proxy_descriptors: A list of (width, height, source_path) tuples describing the
                proxies that need to be computed.

        Returns:
            A list with all the command line parameters necessary to launch the OIIO tool
            to generate proxy files.
        """
        source_path = self._get_source_path(asset)

        # Crete the base of the oiiotool shell command.
        oiiotool_command = ['oiiotool', '-q', '-native', '-wildcardoff', source_path,
                            '--cache', '1024', '--clear-keywords',
                            '--nosoftwareattrib', '--eraseattrib', 'thumbnail_image',
                            '--eraseattrib', 'Exif:.*', '--eraseattrib', 'IPTC:.*']

        if not asset.get_attr('tmp.proxy_source_image') and asset.get_attr('clip.type') == 'page':
            start = asset.get_attr('clip.start')
            if start:
                page = start - 1
                oiiotool_command.extend(['--subimage', str(int(page))])
        for (width, height, output_path) in proxy_descriptors:
            # We need to create a proxy so add an output to the oiiotool command.
            oiiotool_command.extend([
                '--resize:filter=lanczos3',
                '%sx%s' % (width, height),
                '--autocc', '--quality', self.ml_quality
            ])
            oiiotool_command.extend(['-o', str(output_path)])
        return oiiotool_command

    def _get_proxy_descriptors(self, asset):
        """
        Make a list of (width, height, path) tuples of the proxies to be computed.

        The algorithm examines the asset for existing proxies and does not put descriptors
        in the list if a matching proxy already exists.

        Args:
            asset: The asset for which the proxy sizes are to be determined.

        Returns:
            A list of (width, height) pairs with the proxy sizes.
        """
        source_width, source_height = self._get_source_dimensions(asset)
        tmp_dir = Path(tempfile.mkdtemp(prefix="proxy-"))
        # Determine list of (width, height) for proxies to be made.
        proxy_sizes = []
        for size in self._get_valid_sizes(source_width, source_height, self.arg_value('sizes')):
            width, height = get_output_dimension(size, source_width, source_height)
            output_path = tmp_dir.joinpath('%s_%sx%s.jpg' % (asset.id, width, height))
            proxy_sizes.append((width, height, output_path))

        return proxy_sizes

    def _get_source_dimensions(self, asset):
        """
        Return the source dimensions for the given asset.  If the asset
        is an image and has a width/height, then return those values.  Otherwise
        try to detect and use the image rendering of the asset.

        Args:
            asset (Asset): The asset to check

        Returns (list): A list of two integers, width and height

        """
        # If the source is an image then oiio has already setup the settings we need.
        if asset.get_attr("media.type") == "image" \
                and asset.attr_exists("media.width") \
                and asset.attr_exists("media.height"):
            return [asset.get_attr("media.width"), asset.get_attr("media.height")]
        else:
            # If the source is not an image, try to figure it out by looking at
            # the tmp image representation of the asset.
            return media_size(self._get_source_path(asset))

    def _get_source_path(self, asset):

        proxy_source_file = asset.get_attr("tmp.proxy_source_image")
        if proxy_source_file:
            return file_storage.localize_file(proxy_source_file)

        # Handles pulling the actual source.path or a files source.
        # If the source file type is not an image, this processor
        # has no chance of making a proxy, so we're going to skip
        # generating an error.
        if asset.get_attr("media.type") == "image":
            return file_storage.localize_file(asset)
        return None

    def _get_valid_sizes(self, width, height, sizes):
        """Based on the sizes provided to the processor the valid sizes to create proxies
        of are returned.

        Args:
            width(int): The width of the image to use for determining valid proxy sizes.
            height(int): The height of the image to use for determining valid proxy sizes.
        Returns:
            list<int>: List of valid proxy sizes to create.

        """
        valid_sizes = []
        longest_edge = max(width, height)
        for size in sizes:
            # Can't use a set here, maintaining order
            _size = min(size, longest_edge)
            if _size not in valid_sizes:
                valid_sizes.append(_size)

        if not valid_sizes:
            valid_sizes.append(longest_edge)
        return sorted(valid_sizes, reverse=True)

    def make_web_optimized_proxy(self, asset, src_path, src_size):
        """
        Make a web optimized proxy the same size as the largest proxy.

        References:
            https://developers.google.com/speed/docs/insights/OptimizeImages

        Args:
            asset (Asset): The asset to use

        Returns:
            StoredFile: The proxy stored file.
        """
        tmp_dir = tempfile.mkdtemp()
        output_path = os.path.join(tmp_dir, "web-optimized-proxy.jpg")

        valid_size = self._get_valid_sizes(
            src_size[0], src_size[1], [self.arg_value('web_proxy_size')])[0]
        width, height = get_output_dimension(valid_size,
                                             src_size[0], src_size[1])

        cmd = [
            'oiiotool',
            '-q', '-native', '-wildcardoff',
            src_path,
            '--cache', '1024',
            '--resize:filter=lanczos3',
            '%dx%d' % (width, height),
            '--quality', str(self.web_quality),
            '-o',
            output_path
        ]

        logger.debug("Running cmd: {}".format(" ".join(cmd)))
        with StopWatch("Create web proxy"):
            subprocess.check_call(cmd, shell=False,
                                  stdout=subprocess.DEVNULL,
                                  stderr=subprocess.DEVNULL)
        attrs = {"width": width, "height": height}
        prx = file_storage.assets.store_file(output_path, asset, "web-proxy",
                                             "web-proxy.jpg", attrs)
        return prx


ProxySelection = collections.namedtuple('name', '')
