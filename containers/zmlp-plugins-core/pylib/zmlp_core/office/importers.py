import json
import os
import subprocess
import tempfile

from zmlp import FileImport
from zmlpsdk import AssetProcessor, Argument, \
    ExpandFrame, ZmlpFatalProcessorException, FileTypes, StopWatch
from zmlpsdk.storage import file_storage, ZmlpStorageException
from .oclient import OfficerClient
from ..util.media import media_size

__all__ = ['OfficeImporter', '_content_sanitizer']


class OfficeImporter(AssetProcessor):
    file_types = FileTypes.documents

    # The tmp_loc_attribute store the document
    tmp_loc_attr = OfficerClient.tmp_loc_attr

    default_dpi = 150
    """The default DPI for PDF rendering"""

    max_pdf_res = 10000
    """Max PDF render resolution.  Azure max is 10k"""

    def __init__(self):
        super(OfficeImporter, self).__init__()
        self.add_arg(Argument('extract_doc_pages', 'bool', default=False,
                              toolTip='Extract all pages from document as separate assets'))
        self.add_arg(Argument('ocr', 'bool', default=False,
                              toolTip='Outputs will be for OCR purposes.'))
        self.add_arg(Argument('dpi', 'int', default=150,
                              toolTip='The default image render DPI, where applicable.'))
        self.oclient = None

    def init(self):
        # Reset the value of DPI to 300 if OCR is enabled.
        if self.arg_value('ocr'):
            self.arg('dpi').value = 300
        self.oclient = OfficerClient(dpi=self.arg_value('dpi'))

    def get_metadata(self, asset, page):
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
            file_id = self.oclient.get_metadata_file_id(asset, page)
            with open(file_storage.localize_file(file_id), 'r') as fp:
                return json.load(fp, object_hook=_content_sanitizer)
        except ZmlpStorageException as e:
            raise ZmlpFatalProcessorException(
                'Unable to obtain officer metadata, {} {}, {}'.format(asset, page, e))

    def process(self, frame):
        """Processes the given frame by sending it to the Officer service for render.

        Args:
            frame (Frame): The Frame to process
        """
        asset = frame.asset
        page = max(int(asset.get_attr('media.pageNumber') or 1), 1)

        self.logger.info("IRON DEBUG Extracting DOC PAGES : {}".format(self.arg_value('extract_doc_pages')))
        self.logger.info("IRON DEBUG Media Length : {}".format(asset.get_attr('media.length')))
        self.logger.info("IRON DEBUG Media page Number : {}".format(asset.get_attr('media.pageNumber')))
        self.logger.info("IRON DEBUG AssetExtension : {}".format(asset.extension))

        self.render_pages(asset, page, page == 1)

        # If we're on page 1 and extract_doc_pages is true.
        if page == 1 and self.arg_value('extract_doc_pages'):
            # Iterate the pages and expand
            num_pages = int(asset.get_attr('media.length') or 1)
            self.logger.info("IRON DEBUG NUMBER OF PAGES : {}".format(num_pages))
            if num_pages > 1:
                # Start on page 2 since we just processed page 1
                for page_num in range(2, num_pages + 1):
                    self.logger.info("IRON DEBUG Wait for Rendering page : {}".format(page_num))
                    self.oclient.wait_for_rendering(asset, page_num)
                    file_import = FileImport("asset:{}".format(asset.id), page=page_num)
                    expand = ExpandFrame(file_import)
                    self.expand(frame, expand)

    def render_pages(self, asset, page, all_pages):
        """
        Render the specific page image and metadata if it is not already cached.
        Also applies the 'tmp.proxy_source_image' attribute to the rendered page.
        If the asset containers no clip the extract_doc_pages is enabled, then all
        pages will be rendered.

        Args:
            asset (Asset): The Asset
            page (int): The page number to render
            all_pages (bool): Set to true if the request should render all pages.
                This assumes extract_doc_pages is enabled.

        Returns:
            str: The base output URI.

        Raises:
            ZmlpFatalProcessorException if no files can be rendered or found.

        """
        try:
            # Don't render images for PDF
            is_pdf = asset.extension == 'pdf'
            cache_loc = self.oclient.get_cache_location(asset, page)

            if cache_loc:
                self.logger.info('CACHED proxy and metadata outputs: {}'.format(cache_loc))
            else:
                if all_pages and self.arg_value('extract_doc_pages'):
                    cache_loc = self.oclient.render(asset, -1, is_pdf)
                    self.logger.info(
                        'ALL render of proxy and metadata outputs to: {}'.format(cache_loc))
                else:
                    cache_loc = self.oclient.render(asset, page, is_pdf)
                    self.logger.info(
                        'SINGLE render of proxy and metadata outputs to: {}'.format(cache_loc))

            # Need these set in order to render PDFs
            media = asset.get_attr('media') or {}
            self.oclient.wait_for_rendering(asset, page)
            media.update(self.get_metadata(asset, page))
            asset.set_attr('media', media)
            asset.set_attr('media.type', 'document')

            # Officer only renders metadata for PDFs, the image is rendered here.
            if is_pdf:
                full_size_render = self.render_pdf_page(asset, page)
                asset.set_attr('tmp.proxy_source_image', full_size_render)
                if self.arg_value('ocr'):
                    self.store_ocr_proxy(asset, full_size_render)
            else:
                asset.set_attr('tmp.proxy_source_image',
                               self.oclient.get_image_file_id(asset, page))
            return cache_loc

        except Exception as e:
            self.logger.exception("Unable to determine page cache location")
            raise ZmlpFatalProcessorException(
                'Unable to determine page cache location {}, {}'.format(asset.id, e), e)

    def render_pdf_page(self, asset, page):
        """
        Render a single PDF page image.

        Args:
            asset (Asset): The asset.
            page (int): The page number.

        Returns:
            str: The path to the rendered image.
        """
        input_path = file_storage.localize_file(asset)
        dst_path = os.path.join(tempfile.gettempdir(), asset.id + "_pdf_proxy")
        dpi = self.arg_value('dpi')

        # Attempt to figure out scale.  PDFs use 'pts' and not 'dpi'.
        # Standard 'pts' value is 72
        scale = None
        w_res = (asset.get_attr('media.width') / float(72)) * dpi
        h_res = (asset.get_attr('media.height') / float(72)) * dpi
        if w_res > self.max_pdf_res or h_res > self.max_pdf_res:
            scale = ['-scale-to', str(self.max_pdf_res)]

        # Overall the png results in a smaller size for most PDF files.
        # Because the compression works better on documents with just
        # black and white.
        cmd = ['pdftoppm',
               '-singlefile',
               '-f', str(page),
               '-r', str(self.arg_value('dpi')),
               '-hide-annotations']

        if scale:
            cmd.extend(scale)

        cmd.extend([
            '-jpegopt', 'quality=100',
            '-jpegopt', 'optimize=y',
            '-jpeg',
            input_path, dst_path])

        with StopWatch("Render PDF"):
            self.logger.debug(cmd)
            subprocess.check_call(cmd, shell=False)

        return f"{dst_path}.jpg"

    def store_ocr_proxy(self, asset, src_path):
        """
        Store a full size render for OCR purposes.

        Args:
            asset (Asset): The asset.
            src_path (str): The path to the source image.

        Returns:
            StoredFile: The full size PDF proxy, PNG format.
        """

        if not src_path:
            self.logger.warning("There was no proxy_source_image to store as an OCR proxy.")
            return

        # Note the OCR image for PDF is a jpg
        size = media_size(src_path)
        attrs = {"width": size[0], "height": size[1]}
        prx = file_storage.assets.store_file(src_path, asset, "ocr-proxy", "ocr-proxy.jpg", attrs)
        return prx


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
