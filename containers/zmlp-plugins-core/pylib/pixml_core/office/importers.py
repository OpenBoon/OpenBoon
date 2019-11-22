import json
import os
import backoff
import requests

from requests import RequestException

from pixml import AssetSpec, Clip
from pixml.analysis import AssetBuilder, Argument, ExpandFrame, PixmlUnrecoverableProcessorException


__all__ = ["OfficeImporter", "_content_sanitizer"]


class OfficeImporter(AssetBuilder):

    file_types = ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx']
    content_extractable_file_types = ['pdf', 'doc', 'docx', 'ppt', 'pptx']
    tmp_loc_attr = "tmp.office_output_dir"

    tool_tips = {
        'extract_pages': 'If True extract each page as a derived asset',
        'extract_content': 'If True the text content of PDF is extracted and stored in '
                           'the metadata as a searchable field',
        'proxy_dpi': 'Desired DPI for extracted page proxy',
    }

    def __init__(self):
        super(OfficeImporter, self).__init__()
        arguments = [
            Argument('extract_pages', 'boolean', default=True,
                     toolTip=self.tool_tips['extract_pages']),
            Argument('extract_content', 'boolean', default=True,
                     toolTip=self.tool_tips['extract_content']),
            Argument('proxy_dpi', 'int', default=75,
                     toolTip=self.tool_tips['proxy_dpi']),
        ]
        for arg in arguments:
            self.add_arg(arg)

    @property
    def service_url(self):
        url = os.environ.get('OFFICER_FQDN', 'http://officer')
        port = os.environ.get('OFFICER_PORT', '7081')
        return '{url}:{port}'.format(url=url, port=port)

    @property
    def extract_url(self):
        return '{service}/extract'.format(service=self.service_url)

    def _is_content_extractable(self, asset_path):
        """Filters filetypes that result in an unusable amount of content."""
        _, ext = os.path.splitext(asset_path)
        if ext.lstrip('.') in self.content_extractable_file_types:
            return True
        return False

    def _needs_rerender(self, asset):
        """Make sure the rendered proxy and metadata still exists."""
        output_dir = asset.get_attr(self.tmp_loc_attr)
        page = asset.get_attr('media.clip.start')
        proxy = os.path.join(output_dir, 'proxy.{}.jpg'.format(page))
        metadata = os.path.join(output_dir, 'metadata.{}.json'.format(page))

        if not os.path.exists(proxy):
            self.logger.warning("The proxy file '{}' does not exist, re-rendering".format(proxy))
            return True

        if not os.path.exists(metadata):
            self.logger.warning("The metadata file '{}' does not exist, re-rendering"
                                .format(metadata))
            return True

        return False

    def _get_request_body(self, asset):
        asset_path = asset.uri
        page = asset.get_attr("media.clip.start")
        # The output dir is always the parent directory.
        output_dir = asset.get_attr("media.clip.parent") or asset.id
        request_body = {'input_file': asset_path,
                        'dpi': self.arg_value('proxy_dpi'),
                        'output_dir': output_dir}
        if self.arg_value("extract_content") and self._is_content_extractable(asset_path):
            request_body["content"] = "true"

        if page:
            request_body["page"] = page

        return request_body

    def _load_metadata(self, metadata_path):
        """Loads the metadata file. Test seam."""
        return json.load(open(metadata_path), object_hook=_content_sanitizer)

    def _render_outputs(self, asset):
        request_body = self._get_request_body(asset)
        self.logger.info("Making post request: %s" % request_body)

        try:
            response = self._post_to_service(self.extract_url, request_body)
        except RequestException as e:
            self.logger.warning('RequestException: %s' % e)
            if e.request is not None:
                self.logger.warning('Request: %s' % e.request.body)
            if e.response is not None:
                self.logger.warning('Response: %s' % e.response.content)
            raise PixmlUnrecoverableProcessorException(
                'An exception was returned while communicating with the Officer service')

        return response.json()['output']

    @backoff.on_exception(backoff.expo, requests.exceptions.HTTPError, max_time=5*60)
    def _post_to_service(self, url, body):
        """Sends the asset to Officer for render. Retries for 5 minutes if necessary."""
        response = requests.post(url, json=body)
        response.raise_for_status()
        return response

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
        clip_start = asset.get_attr('media.clip.start')
        page = int(clip_start) if clip_start else None
        is_parent = True if not asset.get_attr('media.clip.parent') else False

        # If it's a parent or the previously rendered data is missing, rerender
        if is_parent or self._needs_rerender(asset):
            # Use the returned output directory
            output_dir = self._render_outputs(asset)
            self.logger.info("Rendered proxy and metadata outputs to: {}".format(output_dir))
        else:
            # Since it exists, Use the previously set output directory
            output_dir = asset.get_attr(self.tmp_loc_attr)
            self.logger.info("Utilizing proxy and metadata outputs: {}".format(output_dir))

        # Use the first page for the Parent img and metadata
        if is_parent and not page:
            page = 1

        proxy_path = os.path.join(output_dir, 'proxy.{}.jpg'.format(page))
        metadata_path = os.path.join(output_dir, 'metadata.{}.json'.format(page))

        # Set frame.image for ProxyIngestor to pick up
        asset.set_attr("tmp.proxy_source_image", proxy_path)

        # Load extracted metadata and restore media.clip
        saved_clip = asset.get_attr("media.clip")
        media = self._load_metadata(metadata_path)
        asset.set_attr("media", media)
        if media.get('width') and media.get('height'):
            asset.set_resolution(media.get('width'), media.get('height'))
        if saved_clip:
            asset.set_attr("media.clip", saved_clip)

        # Zero content out if extract_content arg is false
        if not self.arg_value("extract_content"):
            asset.set_attr("media.content", None)

        if self.arg_value("extract_pages"):
            num_pages = asset.get_attr("media.pages")
            if not asset.attr_exists("media.clip") and num_pages > 1:
                for page_num in range(1, num_pages + 1):
                    clip = Clip('page',page_num, page_num)
                    child_asset = AssetSpec(asset.get_attr('source.path'), clip)
                    child_asset.set_attr(self.tmp_loc_attr, output_dir)
                    expand = ExpandFrame(child_asset)
                    self.expand(frame, expand)


def _content_sanitizer(metadata):
    if "content" in metadata:
        metadata["content"] = metadata["content"].replace(u"\u0000", " ")
    return metadata
