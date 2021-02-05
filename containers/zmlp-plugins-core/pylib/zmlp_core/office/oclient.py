import json
import logging
import os
import time

import requests
import websocket
import base64

from zmlpsdk import ZmlpFatalProcessorException
from zmlpsdk.storage import file_storage, ZmlpStorageException
from zmlp.client import ZmlpJsonEncoder
from zmlp.util import as_id
from enum import Enum

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
            variable and finally default to 'ws://officer:7078'
        """
        self.url = url or os.environ.get('OFFICER_URL', 'ws://officer:7078')
        self.dpi = dpi

    @property
    def render_url(self):
        """The full render URL"""
        return '{}/render'.format(self.url)

    @property
    def exists_url(self):
        """The full exists URL"""
        return '{}/exists'.format(self.url)

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
            conn = self._prepare_connection(self.exists_url)
            post_files = self._get_render_request_body(asset, page, disable_images)

            conn.send(json.dumps(post_files))
            json_response = json.loads(conn.recv())
            self._raise_conn_error(json_response)

            asset.set_attr('media.length', asset.get_attr('media.length')
                           or json_response['page-count'])
            asset.set_attr('tmp.request.id', json_response['request-id'])
            return json_response['location']

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

    def get_cache_location(self, asset, page):
        """
        Return the location of the cached page or None if one is not cached.

        Args:
            asset (Asset): The asset to check
            page (int): The page number.

        Returns:
            bool: Return cache location or None if it doesn't exist.
        """

        conn = self._prepare_connection(self.exists_url)
        body = {
            'outputPath': self.get_full_storage_prefix(asset),
            'page': page
        }

        conn.send(json.dumps(body))
        rsp = json.loads(conn.recv())
        self._raise_conn_error(rsp)

        if rsp["status"] == "EXISTS":
            return rsp['location']
        return None

    def get_render_status(self, asset, page, conn):
        """
        Return the render status of the cached page or None if one is not cached.

        Args:
            asset (Asset): The asset to check
            page (int): The page number.
            conn (WebSocket): Connection already established to officer

        Returns:
            bool: True if the both the metadata and proxy file exist for the page.
        """

        body = {
            'outputPath': self.get_full_storage_prefix(asset),
            'page': page,
            'requestId': asset.get_attr('tmp.request.id')
        }

        conn.send(json.dumps(body))
        rsp = json.loads(conn.recv())
        self._raise_conn_error(rsp)

        return rsp

    def _prepare_connection(self, url):
        """
        Prepare websocket connection
        :param url:
        :return:
        """
        return websocket.create_connection(url)

    def _raise_conn_error(self, rsp):
        """
        Verify if websocket response contains an error
        :param rsp:
        :return:
        """
        if isinstance(rsp, str):
            rsp = json.loads(rsp)
        if rsp["status"] == ResponseStatus.FAIL:
            raise ZmlpFatalProcessorException(rsp["message"])
        if rsp["status"] == ResponseStatus.TOO_MANY_REQUESTS:
            raise ZmlpFatalProcessorException(
                "Officer server is overloaded: Load: {} - Max {}".format(rsp["load"], rsp["max"])
            )

    def wait_for_rendering(self, asset, page):
        """
        Loop and wait until the asset page rendering accomplish
        or raise an exception if an error occurred in officer server
        :param asset: The asset
        :param page: Asset page
        :return: Waits until assets rendering finish or raise an exception if exceed accepted time
        """
        number_of_retries = 20
        retry_number = 0
        conn = self._prepare_connection(self.exists_url)
        while True:
            rsp = self.get_render_status(asset, page, conn)
            if rsp["status"] == ResponseStatus.EXISTS:
                logger.info("Page: {} is ready".format(page))
                break
            if rsp["status"] == ResponseStatus.NOT_EXISTS:
                logger.error("An error occurred. Page {} will not be rendered by officer"
                             .format(page))
                raise ZmlpFatalProcessorException(
                    'Rendering failure, Asset: {} Page: {} will not render'.format(asset.id, page))

            logger.info('Waiting page : {} of asset: {} try number: {}'
                        .format(page, asset.id, retry_number))
            time.sleep(1)
            retry_number += 1
            if retry_number > number_of_retries:
                raise ZmlpFatalProcessorException(
                    'Unable to obtain officer metadata, {} {}'.format(asset, page))

    def _get_render_request_body(self, asset, page, disable_images):
        """
        Formulates a multi-part request body in order to upload a file
        to be rendered to officer.

        Args:
            asset (Asset): The asset we're going to upload.
            page (int): The page number or None to render all pages.
            disable_images (bool): Disable image rendering.
        Returns:
            dict : An dictionary containing a Base64 encoded file and a body
            containing a rendering request

        """
        # localizes the asset's source file if it's not
        # already localized.
        file_path = file_storage.localize_file(asset)

        if not page:
            # -1 means render everything.
            page = -1

        body = {
            'fileName': asset.uri,
            'outputPath': self.get_full_storage_prefix(asset),
            'page': page,
            'disableImageRender': disable_images,
            'dpi': self.dpi
        }

        # combine the file and json body into a websocket request
        post_files = {
            'file': base64.b64encode(open(file_path, 'rb').read()).decode("utf-8"),
            'body': json.dumps(body, cls=ZmlpJsonEncoder)
        }

        return post_files

    @staticmethod
    def get_full_storage_prefix(asset):
        """
        Return the full native file storage path prefix for the given asset.

        Args:
            asset (Asset): The asset or its unique Id.

        Returns:

        """
        job_storage_path = os.environ.get('ZORROA_JOB_STORAGE_PATH')
        return '{}/officer/{}'.format(job_storage_path, as_id(asset))

    @staticmethod
    def get_file_storage_uri(asset, fname):
        """
        Return a ZMLP file storage URI for the given asset and file name.  The
        ZMLP URI is always stored under the current job. and looks like:

            zmlp://job/<jobid>/officer/<asset_id>_<file name>

        This form of URI can be obtained using file_storage.

        Args:
            asset (Asset): The Asset or unique id.
            fname (str): The file name.

        Returns:
            str: The URI.
        """
        jid = os.environ.get('ZMLP_JOB_ID')
        return 'zmlp://job/{}/officer/{}_{}'.format(jid, as_id(asset), fname)

    @staticmethod
    def get_metadata_file_id(asset, page):
        """
        Return the ZMLP storage URL for the given page.

        Args:
            asset (Asset): The asset the metadata file is associated wiht.
            page (int): The page number, 0 for parent page.

        Returns:
            str: the ZMLP URL to the image.
        """
        return OfficerClient.get_file_storage_uri(
            as_id(asset), "metadata.{}.json".format(max(page, 0)))

    @staticmethod
    def get_image_file_id(asset, page):
        """
        Return the ZMLP storage URL for the given page.

        Args:
            asset (Asset): The asset the image file is associated with.
            page (int): The page number, 0 for parent page.

        Returns:
            str: the ZMLP URL to the image.
        """
        return OfficerClient.get_file_storage_uri(
            as_id(asset), "proxy.{}.jpg".format(max(page, 0)))


class ResponseStatus(Enum):
    EXISTS = 'EXISTS'
    RENDERING = 'RENDERING'
    NOT_EXISTS = 'NOT_EXISTS'
    TOO_MANY_REQUESTS = 'TOO_MANY_REQUESTS'
    FAIL = 'FAIL'
    RENDER_QUEUE = 'RENDER_QUEUE'
    BAD_REQUEST = 'BAD_REQUEST'
