import json
import logging
import os
import time

import backoff
import requests

from zmlpsdk import ZmlpFatalProcessorException
from zmlpsdk.storage import file_storage, ZmlpStorageException
from zmlp.client import ZmlpJsonEncoder
from zmlp.util import as_id

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
            json_response = rsp.json()
            logger.info('IRON DEBUG -1')
            logger.info('IRON DEBUG Media.LENGth = {} '.format(asset.get_attr('media.length')))
            logger.info('IRON DEBUG PageCount = {} '.format(json_response['page-count']))
            logger.info('IRON DEBUG One Or Another = {} '.format(asset.get_attr('media.length')
                                                                 or json_response['page-count']))

            asset.set_attr('media.length', asset.get_attr('media.length') or json_response['page-count'])
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
        body = {
            'outputPath': self.get_full_storage_prefix(asset),
            'page': page
        }
        rsp = requests.post(self.exists_url, json=body,
                            headers={'Content-Type': 'application/json'})
        if rsp.status_code == 200:
            return rsp.json().get('location')
        elif rsp.status_code == 404 or rsp.status_code == 410:
            return None
        else:
            rsp.raise_for_status()

    def get_render_status(self, asset, page):
        """
        Return the render status of the cached page or None if one is not cached.

        Args:
            asset (Asset): The asset to check
            page (int): The page number.

        Returns:
            bool: True if the both the metadata and proxy file exist for the page.
        """

        logger.info("IRON DEBUG RENDER STATUS {}".format(asset.get_attr('tmp.request.id')))
        body = {
            'outputPath': self.get_full_storage_prefix(asset),
            'page': page,
            'requestId': asset.get_attr('tmp.request.id')
        }
        rsp = requests.post(self.exists_url, json=body,
                            headers={'Content-Type': 'application/json'})
        return rsp

    @backoff.on_exception(backoff.expo, requests.exceptions.HTTPError, max_time=5 * 60)
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
        while True:
            logger.info("IRON DEBUG WAITING FOR RENDER")
            rsp = self.get_render_status(asset, page)
            if rsp.status_code == 200:
                logger.info("Page: {} is ready".format(page))
                break
            if rsp.status_code == 410:
                logger.error("An error occurred. Page {} will not be rendered by officer".format(page))
                raise ZmlpFatalProcessorException(
                    'Rendering failure, Asset: {} Page: {} will not render'.format(asset.id, page))

            logger.info("IRON DEBUG WAIT FOR RENDERING")
            rsp.raise_for_status()
            logger.info('Waiting page : {} of asset: {} try number: {}'.format(page, asset.id, retry_number))
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
            list : An array which can be used for a multi-part upload.

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

        # combine the file and json body into a multi-part request
        post_files = [
            ('file', (body['fileName'], open(file_path, 'rb'))),
            ('body', (None, json.dumps(body, cls=ZmlpJsonEncoder), 'application/json'))
        ]
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
