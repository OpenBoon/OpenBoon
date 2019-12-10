import json
import os

from pixml import FileImport, Clip
from pixml.analysis import AssetBuilder, Argument, ExpandFrame, PixmlUnrecoverableProcessorException
from pixml.analysis.storage import file_cache, PixmlStorageException
from .oclient import OfficerClient

__all__ = ["OfficeImporter", "_content_sanitizer"]


class OfficeImporter(AssetBuilder):
    file_types = ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx']
    content_extractable_file_types = ['pdf', 'doc', 'docx', 'ppt', 'pptx']
    tmp_loc_attr = "tmp.office_output_dir"

    tool_tips = {
        'extract_pages': 'If True extract each page as a derived asset'
    }

    def __init__(self):
        super(OfficeImporter, self).__init__()
        arguments = [
            Argument('extract_pages', 'boolean', default=True,
                     toolTip=self.tool_tips['extract_pages']),
        ]
        for arg in arguments:
            self.add_arg(arg)

        self.oclient = OfficerClient()

    def _is_content_extractable(self, asset_path):
        """Filters filetypes that result in an unusable amount of content."""
        _, ext = os.path.splitext(asset_path)
        if ext.lstrip('.') in self.content_extractable_file_types:
            return True
        return False

    def _needs_rerender(self, asset, page):
        """Make sure the rendered proxy and metadata still exists."""
        if not page:
            return True
        return not self.oclient.exists(asset, page)

    def get_metadata(self, uri, page):
        """
        Get the rendered metadata blob for given output URI.

        Args:
            uri (str): A previously created output uri.
            page (int): The page number, 0 for the parent page.

        Returns:
            dict: A dict of metadata.

        Raises:
            PixmlUnrecoverableProcessorException: If the file cannot be found

        """
        try:
            pixml_uri = "{}/metadata.{}.json".format(uri, page)
            with open(file_cache.localize_uri(pixml_uri), "r") as fp:
                return json.load(fp)
        except PixmlStorageException as e:
            raise PixmlUnrecoverableProcessorException(
                "Unable to obtain officer metadata, {} {}, {}".format(uri, page, e))

    def get_image_uri(self, uri, page):
        """
        Return the pixml storage URL for the given page.

        Args:
            uri (str):  A previously created output uri.
            page (int): The page number, 0 for parent page.

        Returns:
            str: the pixml URL to the image.
        """
        return "{}/proxy.{}.jpg".format(uri, max(page, 0))

    def process(self, frame):
        """Processes the given frame by sending it to the Officer service for render.

        If a Parent asset is given, it'll be sent to Officer to have all of it's pages
        rendered. The rendered pages are then left cached on disk. The parent will use
        the proxy and metadata from the first page. Assuming the extract_pages arg is
        given, each page of the doc will be expanded into it's own frame and will run
        through the process method again.

        If an expanded child asset is passed in, the cache location will be checked
        to see if the proxy and metadata for that page exists. If not, that single page
        will be sent for rerender. Once the proxy and metadata for that page is
        available, everything is applied ot the page and the process completes.

        Args:
            frame (Frame): The Frame to process

        """
        asset = frame.asset
        clip_start = asset.get_attr('clip.start')
        # Use the page from the clip or page 0 which is the master asset page.
        page = int(clip_start) if clip_start else 0
        is_parent = asset.get_attr('element.name') == "asset"

        # If it's a parent or the previously rendered data is missing, rerender
        if is_parent or self._needs_rerender(asset, page):
            # Use the returned output directory
            output_uri = self.oclient.render(asset, page)
            self.logger.info("Rendered proxy and metadata outputs to: {}".format(output_uri))
        else:
            # Since it exists, Use the previously set output directory
            output_uri = asset.get_attr(self.tmp_loc_attr)
            self.logger.info("Utilizing proxy and metadata outputs: {}".format(output_uri))

        # Set frame.image for ProxyIngestor to pick up
        asset.set_attr("tmp.proxy_source_image", self.get_image_uri(output_uri, page))

        media = self.get_metadata(output_uri, page)
        asset.set_attr("media", media)

        if self.arg_value("extract_pages"):
            # Only assets have media.length
            num_pages = asset.get_attr("media.length")
            if num_pages > 1:
                for page_num in range(1, num_pages + 1):
                    clip = Clip('page', page_num, page_num)
                    child_asset = FileImport(asset.get_attr('source.path'), clip=clip)
                    child_asset.attrs[self.tmp_loc_attr] = output_uri
                    expand = ExpandFrame(child_asset)
                    self.expand(frame, expand)


def _content_sanitizer(metadata):
    if "content" in metadata:
        metadata["content"] = metadata["content"].replace(u"\u0000", " ")
    return metadata
