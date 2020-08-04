import json
import os
import subprocess
import tempfile

from zmlp import FileImport, Clip
from zmlpsdk import AssetProcessor, Argument, ExpandFrame, ZmlpFatalProcessorException, FileTypes
from zmlpsdk.storage import file_storage, ZmlpStorageException
from .oclient import OfficerClient

__all__ = ['OfficeImporter', '_content_sanitizer']


class OfficeImporter(AssetProcessor):
    file_types = FileTypes.documents

    # The tmp_loc_attribute store the document
    tmp_loc_attr = OfficerClient.tmp_loc_attr

    def __init__(self):
        super(OfficeImporter, self).__init__()
        self.add_arg(Argument('extract_pages', 'bool', default=False,
                              toolTip='Extract all pages from document as separate assets'))
        self.add_arg(Argument('dpi', int, default=150,
                              toolTip='DPI setting for PDF renders'))

        self.oclient = None

    def init(self):
        self.oclient = OfficerClient(dpi=self.arg_value('dpi'))

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
            with open(file_storage.localize_file(zuri), 'r') as fp:
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
        asset.set_attr('media.type', 'document')

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
            # Don't render images for PDF
            disable_image_render = asset.extension == 'pdf'
            cache_loc = self.oclient.get_cache_location(asset, page)

            if cache_loc:
                self.logger.info('CACHED proxy and metadata outputs: {}'.format(cache_loc))
            else:
                if all_pages and self.arg_value('extract_pages'):
                    cache_loc = self.oclient.render(asset, -1, disable_image_render)
                    self.logger.info(
                        'ALL render of proxy and metadata outputs to: {}'.format(cache_loc))
                else:
                    cache_loc = self.oclient.render(asset, page, disable_image_render)
                    self.logger.info(
                        'SINGLE render of proxy and metadata outputs to: {}'.format(cache_loc))

            # Officer only renders metadata for PDFs, the image is rendered here.
            if disable_image_render:
                asset.set_attr('tmp.proxy_source_image', self.render_pdf_page(asset, page))
            else:
                asset.set_attr('tmp.proxy_source_image', self.get_image_uri(cache_loc, page))

            return cache_loc

        except Exception as e:
            raise ZmlpFatalProcessorException(
                'Unable to determine page cache location {}, {}'.format(asset.id, e), e)

    def render_pdf_page(self, asset, page):
        """
        Render a single PDF page image.

        Args:
            asset (Asset): The asset.
            page (int): The page numnber.

        Returns:
            str: The path to the rendered image.
        """
        input_path = file_storage.localize_file(asset)
        dst_path = os.path.join(tempfile.gettempdir(), asset.id + "_pdf_proxy")

        # Overall the png results in a smaller size for most PDF files.
        # Because the compression works better on documents with just
        # black and white.
        cmd = ['pdftoppm',
               '-singlefile',
               '-f', str(page),
               '-r', str(self.arg_value('dpi')),
               '-hide-annotations',
               '-png',
               input_path, dst_path]

        subprocess.check_call(cmd, shell=False)
        return f"{dst_path}.png"


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
