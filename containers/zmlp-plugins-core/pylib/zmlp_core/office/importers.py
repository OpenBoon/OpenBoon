import json

from zmlp import FileImport, Clip
from zmlp.analysis import AssetBuilder, Argument, ExpandFrame, ZmlpFatalProcessorException
from zmlp.analysis.storage import file_storage, ZmlpStorageException
from .oclient import OfficerClient

__all__ = ['OfficeImporter', '_content_sanitizer']


class OfficeImporter(AssetBuilder):
    file_types = ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx']

    # The tmp_loc_attribute store the document
    tmp_loc_attr = OfficerClient.tmp_loc_attr

    def __init__(self):
        super(OfficeImporter, self).__init__()
        self.add_arg(Argument('extract_pages', 'bool', default=False,
                              toolTip='Extract all pages from document as separate assets'))
        self.oclient = OfficerClient()

    def get_metadata(self, uri, page):
        """
        Get the rendered metadata blob for given output URI.

        Args:
            uri (str): A previously created output uri.
            page (int): The page number, 0 for the parent page.

        Returns:
            dict: A dict of metadata.

        Raises:
            ZmlpFatalProcessorException: If the file cannot be found

        """
        try:
            zuri = '{}/metadata.{}.json'.format(uri, page)
            with open(file_storage.localize_uri(zuri), 'r') as fp:
                return json.load(fp, object_hook=_content_sanitizer)
        except ZmlpStorageException as e:
            raise ZmlpFatalProcessorException(
                'Unable to obtain officer metadata, {} {}, {}'.format(uri, page, e))

    def get_image_uri(self, uri, page):
        """
        Return the ZMLP storage URL for the given page.

        Args:
            uri (str):  A previously created output uri.
            page (int): The page number, 0 for parent page.

        Returns:
            str: the ZMLP URL to the image.
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

        output_uri = self.render_pages(asset, page, not has_clip)
        media = self.get_metadata(output_uri, page)
        asset.set_attr('media', media)

        if not has_clip:
            # Since there is no clip, then set a clip
            asset.set_attr('clip', Clip.page(1))

            if self.arg_value('extract_pages'):
                # Iterate the pages and expand
                num_pages = int(asset.get_attr('media.length') or 1)
                if num_pages > 1:
                    # Start on page 2 since we just processed page 1
                    for page_num in range(2, num_pages + 1):
                        clip = Clip('page', page_num, page_num)
                        file_import = FileImport("asset:{}".format(asset.id), clip=clip)
                        file_import.attrs[self.tmp_loc_attr] = output_uri
                        expand = ExpandFrame(file_import)
                        self.expand(frame, expand)

    def render_pages(self, asset, page, all_pages):
        """
        Render the specific page image and metadata if it is not already cached.
        Also applies the 'tmp.proxy_source_image' attribute to the rendered page.
        If the asset containers no clip the extract_pages is enabled, then all
        pages will be rendered.

        Args:
            asset (Asset): The Asset
            page (int): The page number to render
            all_pages (bool): Set to true if the request should render all pages.
                This assumes extract_pages is enabled.

        Returns:
            str: The base output URI.

        Raises:
            ZmlpFatalProcessorException if no files can be rendered or found.

        """
        try:
            cache_loc = self.oclient.get_cache_location(asset, page)
            if cache_loc:
                self.logger.info('CACHED proxy and metadata outputs: {}'.format(cache_loc))
            else:
                if all_pages and self.arg_value('extract_pages'):
                    cache_loc = self.oclient.render(asset, -1)
                    self.logger.info(
                        'ALL render of proxy and metadata outputs to: {}'.format(cache_loc))
                else:
                    cache_loc = self.oclient.render(asset, page)
                    self.logger.info(
                        'SINGLE render of proxy and metadata outputs to: {}'.format(cache_loc))
            asset.set_attr('tmp.proxy_source_image', self.get_image_uri(cache_loc, page))
            return cache_loc
        except Exception as e:
            raise ZmlpFatalProcessorException('Unable to determine page cache location {}'
                                                       .format(asset.id), e)


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
