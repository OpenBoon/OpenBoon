import json

from pixml import FileImport, Clip
from pixml.analysis import AssetBuilder, ExpandFrame, PixmlUnrecoverableProcessorException
from pixml.analysis.storage import file_cache, PixmlStorageException
from .oclient import OfficerClient

__all__ = ['OfficeImporter', '_content_sanitizer']


class OfficeImporter(AssetBuilder):
    file_types = ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx']

    # The tmp_loc_attribute store the pixml
    tmp_loc_attr = OfficerClient.tmp_loc_attr

    def __init__(self):
        super(OfficeImporter, self).__init__()
        self.oclient = OfficerClient()

    def _needs_rerender(self, asset, page):
        """Make sure the rendered proxy and metadata still exists."""
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
            pixml_uri = '{}/metadata.{}.json'.format(uri, page)
            with open(file_cache.localize_uri(pixml_uri), 'r') as fp:
                return json.load(fp, object_hook=_content_sanitizer)
        except PixmlStorageException as e:
            raise PixmlUnrecoverableProcessorException(
                'Unable to obtain officer metadata, {} {}, {}'.format(uri, page, e))

    def get_image_uri(self, uri, page):
        """
        Return the pixml storage URL for the given page.

        Args:
            uri (str):  A previously created output uri.
            page (int): The page number, 0 for parent page.

        Returns:
            str: the pixml URL to the image.
        """
        return '{}/proxy.{}.jpg'.format(uri, max(page, 0))

    def process(self, frame):
        """Processes the given frame by sending it to the Officer service for render.

        Args:
            frame (Frame): The Frame to process
        """
        asset = frame.asset

        has_clip = asset.attr_exists('clip')
        page = max(int(asset.get_attr('clip.start') or 1), 1)
        output_uri = self.render_pages(asset, has_clip, page)

        media = self.get_metadata(output_uri, page)
        asset.set_attr('media', media)

        if not has_clip:
            # Since there is no clip, then set a clip, as all pages
            # need to have a clip.
            asset.set_attr('clip', Clip('page', 1, 1))

            # Iterate the pages and expand
            num_pages = int(asset.get_attr('media.length') or 1)
            if num_pages > 1:
                # Start on page 2 since we just processed page 1
                for page_num in range(2, num_pages + 1):
                    clip = Clip('page', page_num, page_num)
                    new_page = FileImport(asset.get_attr('source.path'), clip=clip)
                    new_page.attrs[self.tmp_loc_attr] = output_uri
                    expand = ExpandFrame(new_page)
                    self.expand(frame, expand)

    def render_pages(self, asset, has_clip, page):
        """
        Render the given pages to for the given asset and clip settings.  Utilize
        cached pages from previous renders if necessary.

        Args:
            asset (Asset): The asset
            has_clip (bool): True if the asset provided a clip.
            page (int): The page number to render.

        Returns:
            str: The output URI

        Raises:
            PixmlUnrecoverableProcessorException: upon invalid arguments.

        """

        # If we don't have a clip, then render whole thing.
        if not has_clip:
            output_uri = self.oclient.render(asset, -1)
            self.logger.info('FULL render of proxy and metadata outputs to: {}'.format(
                page, output_uri))
        # checking wrong url
        elif self._needs_rerender(asset, page):
            # If the page doesn't exist in cache, maybe it was cleared out
            # so re-render just the page.
            output_uri = self.oclient.render(asset, page)
            self.logger.info('SINGLE render page "{}" proxy and metadata outputs to: {}'.format(
                page, output_uri))
        elif asset.get_attr(self.tmp_loc_attr):
            output_uri = asset.get_attr(self.tmp_loc_attr)
            self.logger.info('CACHED proxy and metadata outputs: {}'.format(output_uri))
        else:
            raise PixmlUnrecoverableProcessorException("Unable to determine page number or output")

        asset.set_attr('tmp.proxy_source_image', self.get_image_uri(output_uri, page))
        return output_uri


def _content_sanitizer(metadata):
    """
    A json deserializer object hook for cleaning up invalid characters
    from the extracted metdata

    Args:
        metadata (dict): A metadata dictionary

    Returns:
        dict: The cleaned up metdata.
    """
    if "content" in metadata:
        metadata["content"] = metadata["content"].replace(u"\u0000", " ")
    return metadata
