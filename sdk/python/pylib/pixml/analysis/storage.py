import glob
import hashlib
import logging
import os
import shutil
import tempfile
import urllib
from urllib.parse import urlparse

import google.cloud.storage as gcs

import pixml.app

__all__ = [
    "local_file_cache"
]

logger = logging.getLogger(__name__)


class LocalFileCache(object):
    """
    The LocalFileCache provides a temporary place for storing source and
    support files such as thumbnails for processing.

    """
    supported_schemes = [
        "gs",
        "http",
        "https"
    ]

    def __init__(self, app=None):
        """
        Create a new LocalFileCache instance.

        Args:
            app (PixmlApp): Optionally provide a PixmlApp instance,
                defaults to None.
        """
        self.root = tempfile.mkdtemp('pixml', 'lfc')
        self.app = app or pixml.app.from_env()

    def localize_uri(self, uri):
        """
        Download the given URI, store it in the cache, and return the local path.

        Args:
            uri (str): A supported remote data URI.

        Returns:
            str: The path within the local file cache.

        """
        logger.info('Localizing URI: {}'.format(uri))
        _, ext = os.path.splitext(uri)
        path = self.get_path(str(uri), ext)
        _uri = urlparse(uri)
        if _uri.scheme in ('http', 'https'):
            urllib.request.urlretrieve(uri, filename=str(path))
        elif _uri.scheme == 'gs':
            try:
                # Attempt to use credentials.
                gcs_client = gcs.Client()
            except OSError:
                gcs_client = gcs.Client.create_anonymous_client()
            bucket = gcs_client.get_bucket(_uri.netloc)
            blob = bucket.blob(_uri.path[1:])
            blob.download_to_filename(path)
        else:
            raise ValueError('Invalid URI, unsupported scheme: {}'.format(schema))
        return path

    def localize_file_storage(self, storage, copy_path=None):
        """
        Localize the file described by the storage dict.  If a
        path argument is provided, overwrite the file cache
        location with that file.

        Args:
            storage (dict): a file storage dict
            copy_path (str): an optional path to a file to copy into the cache location.

        Returns:
            str: a path to a location in the local file cache.

        """
        _, suffix = os.path.splitext(copy_path or storage['name'])
        key = ''.join((storage['assetId'], storage['name'], storage['category']))
        cache_path = self.get_path(key, suffix)
        if copy_path:
            shutil.copy(copy_path, cache_path)
        elif not os.path.exists(cache_path):
            self.app.client.stream('/api/v2/assets/{}/_files/{}/_stream'
                                   .format(storage['assetId'], storage['name']), cache_path)
        return cache_path

    def get_path(self, key, suffix=""):
        """
        Get the local path for the give cache key.

        Args:
            key (str): a cache key.
            suffix (str): a suffix to append to the result (like a file extension)

        Returns:
            str: The path
        """
        sha = hashlib.sha1()
        sha.update(key.encode('utf-8'))
        sha.update(suffix.encode('utf-8'))
        filename = sha.hexdigest()
        return os.path.join(self.root, filename + suffix)

    def clear(self):
        """
        Clear out the local storage directory.

        """
        logger.info('clearing out temp files: "{}"'.format(self.root))
        files = glob.glob('{}/*'.format(self.root))
        for f in files:
            os.remove(f)


__lfc = LocalFileCache()


def local_file_cache():
    return __lfc
