import json
import logging
import os

import backoff
import requests

from zmlpsdk import ZmlpFatalProcessorException
from zmlpsdk.storage import file_storage, ZmlpStorageException
from zmlp.client import ZmlpJsonEncoder

logger = logging.getLogger(__name__)


class OfficerClient(object):
    """
    A Python client for the Officer service.
    """

    # An attribute which stores an officer page cache location.  This is needed
    # because the page cache isn't always under the current asset's id.
    tmp_loc_attr = 'tmp.officer_page_cache_location'

    def __init__(self, url=None, dpi=150):
        """
        Create a new OfficerClient instance.

        Args:
            url (str): An optional URL. Will look for the OFFICER_URL environment
            variable and finally default to 'http://officer:7078'
        """
        self.url = url or os.environ.get('OFFICER_URL', 'http://officer:7078')
        self.dpi = dpi

    @property
    def render_url(self):
        """The full render URL"""
        return '{}/render'.format(self.url)

    @property
    def exists_url(self):
        """The full exists URL"""
        return '{}/exists'.format(self.url)

    @backoff.on_exception(backoff.expo, requests.exceptions.HTTPError, max_time=5 * 60)
    def render(self, asset, page, disable_images=False):
        """
        Render thumbnails and metadata for the given Asset.

        Args:
            asset (Asset): The asset we're going to render
            page (int): The page number, None for all pages.
            disable_images (bool): Disable the image render.
        Returns:
            (str): An internal ZMLP URL where the thumbnails and metadata are located.

        """
        try:
            post_files = self._get_render_request_body(asset, page, disable_images)
            rsp = requests.post(self.render_url,
                                files=post_files)
            rsp.raise_for_status()
            return rsp.json()['location']

        except requests.RequestException as e:
            logger.warning('RequestException: %s' % e)
            if e.response is not None:
                logger.warning('Response: %s' % e.response.content)
            raise ZmlpFatalProcessorException(
                'An exception was returned while communicating with the Officer service')
        except ZmlpStorageException as ex:
            raise ZmlpFatalProcessorException(
                'Storage failure {}, unable to localize asset id={} uri={}'.format(
                    ex, asset.id, asset.uri))

    @backoff.on_exception(backoff.expo, requests.exceptions.HTTPError, max_time=5 * 60)
    def get_cache_location(self, asset, page):
        """
        Return the location of the cached page or None if one is not cached.

        Args:
            asset (Asset): The asset to check
            page (int): The page number.

        Returns:
            bool: True if the both the metadata and proxy file exist for the page.
        """
        # Look at the tmp_loc_attr first because it won't
        # always have the current asset Id.
        tmp_loc = asset.get_attr(self.tmp_loc_attr)
        asset_id = asset.id
        if tmp_loc:
            asset_id = os.path.basename(tmp_loc)

        body = {
            'outputUri': asset_id,
            'page': page
        }
        rsp = requests.post(self.exists_url, json=body,
                            headers={'Content-Type': 'application/json'})
        if rsp.status_code == 200:
            return rsp.json().get('location')
        elif rsp.status_code == 404:
            return None
        else:
            rsp.raise_for_status()

    def _get_render_request_body(self, asset, page, disable_images):
        """
        Formulates a multi-part request body in order to upload a file
        to be rendered to officer.

        Args:
            asset (Asset): The asset we're going to upload.
            page (int): The page number or None to render all pages.
            disable_images (bool): Disable image rendering.
        Returns:
            list : An array which can be used for a multi-part upload.

        """
        # localizes the asset's source file if it's not
        # already localized.
        file_path = file_storage.localize_file(asset)

        if not page:
            # -1 means render everything.
            page = -1

        # Setup the json body
        job_storage_uri = os.environ.get('ZORROA_JOB_STORAGE_PATH')
        output_uri = '{}/officer/{}'.format(job_storage_uri, asset.id) \
            if job_storage_uri else asset.id
        body = {
            'fileName': asset.uri,
            'outputUri': output_uri,
            'page': page,
            'disableImageRender': disable_images,
            'dpi': self.dpi
        }

        # combine the file and json body into a multi-part request
        post_files = [
            ('file', (body['fileName'], open(file_path, 'rb'))),
            ('body', (None, json.dumps(body, cls=ZmlpJsonEncoder), 'application/json'))
        ]
        return post_files
