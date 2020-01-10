import glob
import hashlib
import logging
import os
import pickle
import shutil
import tempfile
import urllib
from urllib.parse import urlparse

from pathlib2 import Path

from zmlp import app_from_env, Asset
from zmlp.exception import ZmlpException
from .base import ZmlpEnv
from .cloud import get_cached_google_storage_client, get_pipeline_storage_client

__all__ = [
    "file_storage",
    "ZmlpStorageException"
]

logger = logging.getLogger(__name__)


class AssetStorage(object):
    """
    AssetStorage provides ability to store and retrieve files related to the asset.  The
    files are stored in cloud storage.
    """

    def __init__(self,lfc):
        self.lfc = lfc
        self.app = lfc.app

    def store_file(self, asset, src_path, category, rename=None, attrs=None):
        """
        Add a file to the asset's file list and store into externally
        available cloud storage. Also stores a copy into the
        local file cache for use by other processors.

        To obtain the local cache path for the file, call 'localize_asset_file'
        with the result of this method.

        Args:
            asset (Asset): The purpose of the file, ex proxy.
            src_path (str): The local path to the file.
            category (str): The purpose of the file, ex proxy.
            rename (str): Rename the file to something better.
            attrs (dict): Arbitrary attributes to attach to the file.

        Returns:
            dict: an Asset file storage dict.

        """
        spec = {
            "name": rename or Path(src_path).name,
            "category": category,
            "attrs": {}
        }
        if attrs:
            spec["attrs"].update(attrs)

        # handle file:// urls
        path = urlparse(str(src_path)).path
        result = self.app.client.upload_file(
            "/api/v3/assets/{}/files".format(asset.id), path, spec)

        # Store the path to the proxy in our local file storage
        # because a processor will need it down the line.
        self.localize_file(asset, result, path)

        # Ensure the file doesn't already exist in the metadata
        if not asset.get_files(name=spec["name"], category=category):
            files = asset.get_attr("files") or []
            files.append(result)
            asset.set_attr("files", files)

        return result

    def localize_file(self, asset, fdict, precache_file=None):
        """
        Localize the file described by the Asset file storage dictionary.
        If a path argument is provided, overwrite the file cache
        location with that file.

        This storage is used for files you want to serve externally,
        like proxy images.

        Args:
            asset (Asset): The ID of the asset.
            fdict (dict): a ZMLP Project file storage dictionary.
            precache_file (str): an optional path to a file to copy into the cache location.

        Returns:
            str: a path to a location in the local file cache.

        """
        _, suffix = os.path.splitext(precache_file or fdict['name'])
        # Obtain the necessary properties to formulate a cache key.
        name = fdict['name']
        category = fdict['category']
        # handle the pfile referencing another asset.
        asset_id = fdict.get("sourceAssetId", asset.id)
        key = ''.join((asset_id, name, category))

        cache_path = self.lfc.get_path(key, suffix)
        if precache_file:
            precache_path = urlparse(str(precache_file)).path
            logger.debug("Pre-caching {} to {}".format(precache_path, cache_path))
            shutil.copy(urlparse(precache_path).path, cache_path)
        elif not os.path.exists(cache_path):
            self.app.client.stream('/api/v3/assets/{}/files/{}/{}'
                                   .format(asset_id, category, name), cache_path)
        return cache_path


class ProjectStorage(object):
    """
    Provides access to Project cloud storage.
    """
    def __init__(self, lfc):
        self.lfc = lfc
        self.app = lfc.app

    def store_file(self, src_path, entity, category, rename=None):
        spec = {
            "entity": entity,
            "category": category,
            "name": rename or Path(src_path).name,
            "attrs": {}
        }
        path = urlparse(str(src_path)).path
        return self.app.client.upload_file("/api/v3/project/files".format, path, spec)

    def store_blob(self, src_blob, entity, category, name, attrs=None):
        spec = {
            "entity": entity,
            "category": category,
            "name": name,
            "attrs": attrs
        }
        with tempfile.NamedTemporaryFile(suffix=".dat") as tf:
            pickle.dump(src_blob, tf)
            result = self.app.client.upload_file("/api/v3/project/files".format, tf.name, spec)

        return result

    def localize_file(self, entity, category, name):
        _, suffix = os.path.splitext(name)
        key = "".join((entity, category, name))
        cache_path = self.lfc.get_path(key, suffix)
        self.app.client.stream('/api/v3/project/files/{}/{}/{}'.format(
            entity, category, name), cache_path)
        return cache_path


class LocalFileCache(object):
    """
    The LocalFileCache provides a temporary place for storing source and
    support files such as thumbnails for processing.

    """
    supported_schemes = [
        "gs",
        "http",
        "https"
        "file",
        "zmlp"
    ]

    def __init__(self):
        """
        Create a new LocalFileCache instance.
        """
        self.root = None
        self.app = app_from_env()
        self.assets = AssetStorage(self)
        self.projects = ProjectStorage(self)

    def __init_root(self):
        """
        This method builds the root cache directory when the cache is
        used, otherwise it may leave lots of empty cache temp dirs
        in containers or other places.
        """
        if not self.root:
            task = ZmlpEnv.get_task_id()
            if not task:
                self.root = tempfile.mkdtemp('zmlp', 'lfc')
            else:
                self.root = os.path.join(tempfile.gettempdir(), task)
                os.makedirs(self.root, exist_ok=True)

    def localize_remote_file(self, rep):
        """
        Localize a remote file representation.

        The 'rep' value can be:
            - A supported URI
            - Asset instance

        To localize an Asset the file must be in ZMLP storage or a remoote
        file available with the current DataSource credentials (if any).

        Args:
            rep(mixed): The uri or Asset to localize.

        Returns:
            str: a local file path to a remote file

        """
        if isinstance(rep, str):
            return self.localize_uri(rep)
        elif isinstance(rep, Asset):
            source_files = rep.get_files(category="source")
            if source_files:
                return self.assets.localize_file(rep, source_files[0])
            else:
                return self.localize_uri(rep.uri)
        else:
            raise ValueError("cannot localize file, unable to determine the remote file source")

    def localize_uri(self, uri):
        """
        Download the given URI, store it in the cache, and return the local path.

        Args:
            uri (str): A supported remote data URI.

        Returns:
            str: The path within the local file cache.

        """
        logger.debug('Localizing URI: {}'.format(uri))
        _, ext = os.path.splitext(uri)
        path = self.get_path(str(uri), ext)
        parsed_uri = urlparse(uri)

        # Remote HTTP/HTTPS Files
        if parsed_uri.scheme in ('http', 'https'):
            urllib.request.urlretrieve(uri, filename=str(path))

        # File URIs
        elif parsed_uri.scheme == 'file':
            return parsed_uri.path

        # ZMLP ML storage
        elif parsed_uri.scheme == 'zmlp':
            data = get_pipeline_storage_client().get_object(parsed_uri.netloc, parsed_uri.path[1:])
            with open(path, 'wb') as fpw:
                for d in data.stream(32 * 1024):
                    fpw.write(d)

        # GCS buckets
        elif parsed_uri.scheme == 'gs':
            gcs_client = get_cached_google_storage_client()
            bucket = gcs_client.get_bucket(parsed_uri.netloc)
            blob = bucket.blob(parsed_uri.path[1:])
            blob.download_to_filename(path)
        elif parsed_uri.scheme == '' and parsed_uri.path.startswith("/"):
            path = parsed_uri.path
        else:
            raise ZmlpStorageException('Invalid URI, unsupported scheme: {}'.format(parsed_uri))
        return path

    def get_path(self, key, suffix=""):
        """
        Get the local path for the give cache key.

        Args:
            key (str): a cache key.
            suffix (str): a suffix to append to the result (like a file extension)

        Returns:
            str: The path
        """
        self.__init_root()
        sha = hashlib.sha1()
        # Set during processing.
        project_id = ZmlpEnv.get_project_id()
        if project_id:
            sha.update(project_id.encode('utf-8'))
        sha.update(key.encode('utf-8'))
        sha.update(suffix.encode('utf-8'))
        filename = sha.hexdigest()
        return os.path.join(self.root, filename + suffix)

    def clear(self):
        """
        Clear out the local storage directory.

        """
        if not self.root:
            return
        logger.debug('clearing out local file cache: "{}"'.format(self.root))
        files = glob.glob('{}/*'.format(self.root))
        for f in files:
            os.remove(f)

    def close(self):
        """
        Close the local file cache and remove all files. The cache will
        not be usable after this is called.

        """
        if not self.root:
            return
        logger.info('closing local file cache : "{}"'.format(self.root))
        shutil.rmtree(self.root)


class ZmlpStorageException(ZmlpException):
    """
    This exception is thrown if there are problems with storing or retrieving a file.
    """
    pass


"""
A local file cache singleton.
"""
file_storage = LocalFileCache()
