import json
import os

import backoff
import requests

from pixml.analysis import PixmlUnrecoverableProcessorException
from pixml.analysis.storage import file_cache, PixmlStorageException
from pixml.rest import PixmlJsonEncoder


class OfficerClient(object):
    """
    A Python client for the Officer service.
    """

    def __init__(self, url=None):
        """
        Create a new OfficerClient instance.

        Args:
            url (str): An optional URL. Will look for the PIXML_OFFICER_URL envionment
            variable and finally default to 'http://officer:7081'
        """
        self.url = url or os.environ.get('PIXML_OFFICER_URL', 'http://officer:7081')

    @property
    def render_url(self):
        """The full render URL"""
        return "{}/render".format(self.url)

    @property
    def exists_url(self):
        """The full exists URL"""
        return "{}/exists".format(self.url)

    @backoff.on_exception(backoff.expo, requests.exceptions.HTTPError, max_time=5 * 60)
    def render(self, asset, page):
        """
        Render thumbnails and metadata for the given aset.

        Args:
            asset (Asset): The asset we're going to render
            page (int): The page number, None for all pages.

        Returns:
            (str): An pixml URL where the thumbnails and metadata are located.

        """
        try:
            post_files = self._get_render_request_body(asset, page)
            rsp = requests.post(self.render_url,
                                headers={'Content-Type': ''},
                                files=post_files)
            rsp.raise_for_status()
            return rsp.json()['output']

        except requests.RequestException as e:
            self.logger.warning('RequestException: %s' % e)
            if e.request is not None:
                self.logger.warning('Request: %s' % e.request.body)
            if e.response is not None:
                self.logger.warning('Response: %s' % e.response.content)
            raise PixmlUnrecoverableProcessorException(
                'An exception was returned while communicating with the Officer service')
        except PixmlStorageException as ex:
            raise PixmlUnrecoverableProcessorException(
                'Storage failure {}, unable to localize asset id={} uri={}'.format(
                    ex, asset.id, asset.uri))

    def exists(self, asset, page):
        """

        Args:
            asset (Asset): The asset to check
            page (int): The page number.

        Returns:
            bool: True if the both the metadata and proxy file exist for the page.
        """
        body = {
            "output_dir": asset.id,
            "page": page
        }
        rsp = requests.post(self.exists_url, body=body)
        if rsp.status_code == 200:
            return True
        else:
            return False

    def _get_render_request_body(self, asset, page):
        """
        Formulates a multi-part request body in order to upload a file
        to be rendered to officer.

        Args:
            asset (Asset): The asset we're going to upload.
            page (int): The page number or None to render all pages.

        Returns:
            list : An array which can be used for a multi-part upload.

        """
        # localizes the asset's source file if it's not
        # already localized.
        file_path = file_cache.localize_remote_file(asset)

        # Setup the json body
        body = {
            "file_name": asset.uri,
            "output_dir": asset.id,
            "page": page or -1
        }

        # combine the file and json body into a multi-part request
        post_files = [
            ("file", (body["file_name"], open(file_path, 'rb'))),
            ("body", (None, json.dumps(body, cls=PixmlJsonEncoder), 'application/json'))
        ]
        return post_files
