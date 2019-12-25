import glob
import hashlib
import logging
import os
import shutil
import tempfile
import urllib
from urllib.parse import urlparse

from pathlib2 import Path

from zmlp import app_from_env, Asset
from zmlp.exception import ZmlpException
from .base import ZmlpEnv
from .cloud import get_cached_google_storage_client, get_zmlp_storage_client

__all__ = [
    "file_cache",
    "get_proxy_min_width",
    "get_proxy_level",
    "add_proxy_file",
    "add_zmlp_file",
    "ZmlpStorageException"
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
        "file",
        "zmlp"
    ]

    def __init__(self):
        """
        Create a new LocalFileCache instance.
        """
        self.root = None
        self.app = app_from_env()

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
                return self.localize_asset_file(rep, source_files[0])
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
        self.__init_root()
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
            data = get_zmlp_storage_client().get_object(parsed_uri.netloc, parsed_uri.path[1:])
            with open(path, 'wb') as fpw:
                for d in data.stream(32 * 1024):
                    fpw.write(d)

        # GCS buckets
        elif parsed_uri.scheme == 'gs':
            gcs_client = get_cached_google_storage_client()
            bucket = gcs_client.get_bucket(parsed_uri.netloc)
            blob = bucket.blob(parsed_uri.path[1:])
            blob.download_to_filename(path)
        else:
            raise ZmlpStorageException('Invalid URI, unsupported scheme: {}'.format(parsed_uri))
        return path

    def localize_asset_file(self, asset, fdict, copy_path=None):
        """
        Localize the file described by the ZMLP file storage dictionary.
        If a path argument is provided, overwrite the file cache
        location with that file.

        This storage is used for files you want to serve externally,
        like proxy images.

        Args:
            asset (Asset): The ID of the asset.
            fdict (dict): a ZMLP Project file storage dictionary.
            copy_path (str): an optional path to a file to copy into the cache location.

        Returns:
            str: a path to a location in the local file cache.

        """
        self.__init_root()
        _, suffix = os.path.splitext(copy_path or zfile['name'])

        # Obtain the necessary properties to formulate a cache key.
        name = fdict['name']
        category = fdict['category']
        # handle the pfile referencing another asset.
        asset_id = fdict.get("sourceAssetId", asset.id)
        key = ''.join((asset_id, name, category))

        cache_path = self.get_path(key, suffix)
        if copy_path:
            copy_path = urlparse(str(copy_path)).path
            logger.debug("Copying to cache {} to {}".format(copy_path, cache_path))
            shutil.copy(urlparse(copy_path).path, cache_path)
        elif not os.path.exists(cache_path):
            self.app.client.stream('/api/v3/assets/{}/files/{}/{}'
                                   .format(asset_id, category, name), cache_path)
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
        self.__init_root()
        sha = hashlib.sha1()
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


def get_proxy_level(asset, level, mimetype="image/"):
    """
    Localize and return the given proxy level.  The smallest proxy is
    level 0, the largest proxy is 0 or greater.  Out of bounds level
    values will be clamped to the correct range automatically.  For example
    if there are only 2 proxies and you pass level 3, then you will get the
    level 2 proxy.

    Args:
        asset (Asset): The Asset.
        level (int): The proxy level, the larger the number the bigger the file.
        mimetype (str): The proxy mimetype, defaults to image/

    Returns:
        str: A path to the localized proxy file or None on no match.

    """
    files = asset.get_files(mimetype=mimetype, category="proxy", attr_keys=["width"],
                            sort_func=lambda f: f['attrs']['width'])
    if level >= len(files):
        level = -1
    try:
        proxy = files[level]
        return file_cache.localize_asset_file(asset, proxy)
    except IndexError:
        return None


def get_proxy_min_width(asset, min_width, mimetype="image/", fallback=False):
    """
    Return a tuple containing a suitable proxy file or fallback to the source media.
    The first element of the tuple is the name of proxy file such as "proxy_200x200.jpg"
    or simply "source" if the source was selected.

    Args:
        asset (Asset): an Asset instance
        min_width (int): The minimum width to accept for the proxy.
        mimetype (str): A mimetype filter, returns only files that start with this filter.
        fallback (bool): Fallback to the source if the proxy is not available.

    Returns:
        str: A path to the localized proxy file or None on no match.

    """
    files = asset.get_files(mimetype=mimetype, category="proxy", attr_keys=["width"],
                            sort_func=lambda f: f['attrs']['width'])
    # Trim out smaller ones
    files = [file for file in files if file["attrs"]["width"] >= min_width]

    if files:
        return file_cache.localize_asset_file(asset, files[0])
    elif fallback:
        return file_cache.localize_remote_file(asset)
    else:
        raise ValueError("No suitable proxy file was found.")


def add_proxy_file(asset, path, size):
    """
    A convenience function that adds a proxy file to the Asset and
    uploads the file to ZMLP storage.

    Args:
        asset (Asset): The purpose of the file, ex proxy.
        path (str): The local path to the file.
        size (tuple of int): a tuple of width, height

    Returns:
        dict: a ZMLP file storage dict.
    """
    _, ext = os.path.splitext(path)
    if not ext:
        raise ValueError("The path to the proxy file has no extension, but one is required.")
    name = "proxy_{}x{}{}".format(size[0], size[1], ext)
    return add_zmlp_file(asset, path, "proxy", rename=name,
                          attrs={"width": size[0], "height": size[1]})


def add_zmlp_file(asset, path, category, rename=None, attrs=None):
    """
    Add a file to the asset and upload into ZMLP storage.
    Also stores a copy into the local file cache.

    Args:
        asset (Asset): The purpose of the file, ex proxy.
        category (str): The purpose of the file, ex proxy.
        path (str): The local path to the file.
        rename (str): Rename the file to something better.
        attrs (dict): Arbitrary attributes to attach to the file.

    Returns:
        dict: a ZMLP file storage dict.

    """
    app = app_from_env()
    spec = {
        "name": rename or Path(path).name,
        "attrs": {}
    }
    if attrs:
        spec["attrs"].update(attrs)

    # handle file:// urls
    path = urlparse(str(path)).path
    result = app.client.upload_file(
        "/api/v3/assets/{}/files/{}".format(asset.id, category), path, spec)

    # Store the path to the proxy in our local file storage
    # because a processor will need it down the line.
    file_cache.localize_asset_file(asset, result, path)

    # Ensure the file doesn't already exist in the metadata
    if not asset.get_files(name=spec["name"], category=category):
        files = asset.get_attr("files") or []
        files.append(result)
        asset.set_attr("files", files)

    return result


class ZmlpStorageException(ZmlpException):
    """
    This exception is thrown if there are problems with storing or retrieving a file.
    """
    pass


"""
A local file cache singleton.
"""
file_cache = LocalFileCache()
