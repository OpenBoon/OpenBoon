import glob
import hashlib
import logging
import os
import shutil
import tempfile
import time
import urllib
import uuid
import zipfile
import subprocess
from pathlib import Path
from urllib.parse import urlparse

import backoff
import requests

from boonsdk import app_from_env, Asset, StoredFile, AnalysisModule, \
    BoonSdkException, util
from .base import BoonEnv
from .cloud import get_cached_google_storage_client, \
    get_cached_aws_client, get_cached_azure_storage_client

__all__ = [
    'file_storage',
    'StorageException'
]

logger = logging.getLogger(__name__)


class ModelStorage:
    """
    ModelStorage handles installing models into the local model cache.
    """

    model_ver_file = "/model-version.txt"

    def __init__(self, app, projects):
        self.app = app
        self.projects = projects

        # When running within the analyst, our project specific
        # model cache is mounted here.
        self.root = os.environ.get("BOONAI_MODEL_CACHE", "/tmp/boonai/model-cache")

    @staticmethod
    def get_model_file_id(model, tag):
        """
        Utility method which takes a model instance or model file id (str)
        and returns the model file id.

        Args:
            model (mixed): A Model instance or model file id.
            tag (str): The model version tag.
        Returns:
            str: The model file id.
        """
        file_id = getattr(model, 'file_id', None) or model
        return file_id.replace("__TAG__", tag)

    def get_model_install_path(self, model, tag):
        """
        Return the path to where the model should be installed.

        Args:
            model (mixed): A Model instance or model file id.
            tag (str): The Model version tag.
        Returns:
            str: The model install path.
        """
        model_file_id = self.get_model_file_id(model, tag)
        base, ext = os.path.splitext(model_file_id)
        base = base.replace('/', '_')
        return os.path.join(self.root, base)

    def install_model(self, model, tag):
        """
        Install the given model file.

        Args:
            model (mixed): The Model instance or the model file id.
            tag (str): The model version tag.

        Returns:
            str: The path to an unzipped model directory.

        """
        model_file_id = self.get_model_file_id(model, tag)
        install_path = self.get_model_install_path(model, tag)

        if not self.model_exists(model_file_id, tag):
            logger.info(f'Installing model ({tag} into {install_path}')
            model_zip = self.projects.localize_file(model_file_id)
            os.makedirs(install_path, exist_ok=True)

            # extract all files
            with zipfile.ZipFile(model_zip) as z:
                z.extractall(path=install_path)
        else:
            logger.info(f'Utilizing cached model {model_file_id}')

        return install_path

    def model_exists(self, model, tag):
        """
        Return true if the model we have exists and its the latest version.

        Args:
            model (mixed): The Model instance or model file id.
            tag: (str): The model tag.
        Returns:
            bool: True if we have the latest model.
        """
        model_file_id = self.get_model_file_id(model, tag)
        install_path = self.get_model_install_path(model, tag)

        if not os.path.exists(install_path):
            return False

        try:
            with(open(install_path + self.model_ver_file, "r")) as fp:
                this_version = fp.read().strip()

            latest_ver = os.path.dirname(model_file_id) + self.model_ver_file
            with open(self.projects.localize_file(latest_ver), "r") as fp:
                latest_version = fp.read().strip()

            if this_version != latest_version:
                logger.info(f'Found new version of model {model_file_id}, {latest_ver}')
                return False

        except FileNotFoundError:
            logger.warning(f"Model is unversioned {model_file_id}")

        return True

    def save_model(self, src_dir, model, tag, post_action):
        """
        Upload a directory containing model files to cloud storage.

        Args:
            src_dir (str): The source directory.
            model (Model): The Model instance.
            tag (str): The model version tag.
            post_action (str): handle the post action.
        Returns:
            AnalysisModule: A AnalysisModule for utilizing the model.
        """
        file_id = self.get_model_file_id(model, tag)
        version_file = src_dir + self.model_ver_file
        with open(version_file, 'w') as fp:
            fp.write("{}-{}\n".format(int(time.time()), str(uuid.uuid4())))

        zip_file_path = util.zip_directory(
            src_dir, tempfile.mkstemp(prefix="model_", suffix=".zip")[1])
        self.projects.store_file_by_id(version_file, os.path.dirname(file_id) + self.model_ver_file)
        self.projects.store_file_by_id(zip_file_path, file_id, precache=False)

        mod = self.publish_model(model)

        if not post_action:
            return mod
        elif post_action.lower() == "test":
            logger.info("Applying model to test Assets")
            self.app.models.test_model(model)
        elif post_action.lower() == "apply":
            logger.info("Applying model to Assets")
            self.app.models.apply_model(model)
        return mod

    def publish_model(self, model):
        """
        Publish the given model.  The model must have been trained before.  This
        will make the model available for execution using a AnalysisModule.

        Args:
            model (Model): The Model instance or a unique Model id.
        Returns:
            AnalysisModule: A AnalysisModule which can be used to execute the model on Data.
        """
        mid = util.as_id(model)
        return AnalysisModule(self.app.client.post(f'/api/v3/models/{mid}/_publish'))


class AssetStorage(object):
    """
    AssetStorage provides ability to store and retrieve files related to the asset.  The
    files are stored in cloud storage.  This is mostly a convenience class around
    the ProjectStorage class, however it performs the extra job or appending the
    StoredFile to the files namespace.
    """

    def __init__(self, app, proj_store):
        self.app = app
        self.proj_store = proj_store

    def get_native_uri(self, stored_file):
        """
        Return the file's native url (like gs://).

        Args:
            stored_file (StoredFile): A filed stored in the Zorroa backend.
        Returns:
            str: The native uri.

        """
        return self.proj_store.get_native_uri(stored_file)

    def store_file(self, src_path, asset, category, rename=None, attrs=None):
        """
        Add a file to the asset's file list and store into externally
        available cloud storage. Also stores a copy into the
        local file cache for use by other processors.

        To obtain the local cache path for the file, call 'localize_asset_file'
        with the result of this method.

        Args:
            src_path (str): The local path to the file.
            asset (Asset):  A Boon AI Asset instance.
            category (str): The purpose of the file, ex proxy.
            rename (str): Rename the file to something better.
            attrs (dict): Arbitrary attributes to attach to the file.

        Returns:
            StoredFile: A stored file record.

        """
        result = self.proj_store.store_file(src_path, asset, category, rename, attrs)
        asset.add_file(result)
        return result

    def store_blob(self, blob, asset, category, name, attrs=None):
        """
        Add a blob of text to the asset's file list and store into externally
        available cloud storage.  This is mainly a convenience function that
        eliminates the need for the user to write a file to disk.

        To obtain the local cache path for the file, call 'localize_asset_file'
        with the result of this method.

        Args:
            blob (bytes): The string blob of data to write.
            asset (Asset): A Boon AI Asset instance.
            category (str): The purpose of the file, ex proxy.
            name (str): The name of th efile.
            attrs (dict): Arbitrary attributes to attach to the file.
        Returns:
            StoredFile: The stored file record.

        """
        result = self.proj_store.store_blob(blob, asset, category, name, attrs)
        asset.add_file(result)
        return result


class ProjectStorage(object):
    """
    Provides access to Project cloud storage.
    """

    def __init__(self, app, cache):
        self.app = app
        self.cache = cache

    def store_file_by_id(self, src_path, file_id, attrs=None, precache=True):
        """
        Store a file using its unique file id.

        Args:
            src_path (str): The path to the source file.
            file_id (str): The ID of the file.
            attrs (dict): Any additional attrs to be attached to the file.

        Returns:
            StoredFile: A record for the stored file.

        """
        entity, entity_id, category, name = file_id.split("/", 3)

        spec = {
            "entity": entity,
            "entityId": entity_id,
            "category": category,
            "name": name,
            "attrs": attrs or {},
            "size": os.path.getsize(src_path)
        }

        # To upload a file into project storage, first we get a signed upload URI.
        # Doing it this way offloads upload IO from the Archivist to cloud storage.
        # Additionally there is no size restriction like there would be with
        # a multi-part upload.
        signed = self.app.client.post("/api/v3/files/_signed_upload_uri", spec)
        # Once we have that, we upload the file directly to the URI.
        try:
            self.__upload_to_signed_url(src_path, signed)
        except Exception as e:
            logger.debug("Failed to upload to {}".format(signed), e)
            logger.warning("Failed to upload {}, was rejected by cloud storage".format(file_id))

        # Now that the file is in place, we add our attrs onto the file
        # This returns a StoredFile record which we can embed into the asset.
        result = StoredFile(self.app.client.put("/api/v3/files/_attrs", spec))

        # Once we have the stored file its precached into the proper cache location
        path = urlparse(str(src_path)).path

        if precache:
            self.cache.precache_file(result, path)
        return result

    @backoff.on_exception(backoff.expo, requests.exceptions.HTTPError, max_time=300)
    def __upload_to_signed_url(self, src_path, signed):
        size = os.path.getsize(src_path)
        with open(src_path, 'rb') as fp:
            response = requests.put(signed["uri"],
                                    headers={
                                        "Content-Type": signed["mediaType"],
                                        "Content-Length": str(size)
                                    },
                                    data=fp)
            response.raise_for_status()

    def store_file(self, src_path, entity, category, rename=None, attrs=None, precache=True):
        """
        Store an arbitrary file against the project.

        Args:
            src_path (str): The src path to the file.
            entity (mixed): The instance of the entity to store a file against.
            category (str): The general category for the file. (proxy, model, etc)
            rename (str): An optional file name if it should not be based on the src_path name.
            attrs (dict): A dict of arbitrary attrs.
            precache (bool): pre-cache the file into the storage cache.
        Returns:
            StoredFile: A record for the stored file.

        """
        fid = "/".join((
            entity.__class__.__name__.upper() + "S",
            entity.id,
            category,
            rename or Path(src_path).name
        ))
        return self.store_file_by_id(src_path, fid, attrs=attrs, precache=precache)

    def store_blob(self, src_blob, entity, category, name, attrs=None):
        """
        Store an arbitrary blob against the given entity.

        Args:
            src_blob (bytes): A byte string or array.
            entity (mixed): The instance of the entity to store a file against.
            category (str): A general category for the blob.
            name (str): A name for the blob
            attrs (dict): Arbitrary attrs for the blob.

        Returns:
            StoredFile: A stored file record.

        """
        if isinstance(src_blob, str):
            src_blob = src_blob.encode('utf-8')
        elif not isinstance(src_blob, (bytes, bytearray)):
            raise ValueError("The blob must be a bytes like object or bytearray")

        base, ext = os.path.splitext(name)
        if not ext:
            raise ValueError("The blob name requires a file extension")

        fd, tmp_path = tempfile.mkstemp(suffix=ext, prefix='zblob')
        with open(tmp_path, 'wb') as fp:
            fp.write(src_blob)

        spec = {
            "entity": entity.__class__.__name__.upper() + "S",
            "entityId": entity.id,
            "category": category,
            "name": name,
            "attrs": attrs or {},
            "size": os.path.getsize(tmp_path)
        }

        result = StoredFile(self.app.client.upload_file(
            "/api/v3/files/_upload", tmp_path, spec))
        self.cache.precache_file(result, tmp_path)
        return result

    def localize_file(self, sfile):
        """
        Localize the file described by the StoredFile instance.
        If a path argument is provided, overwrite the file cache
        location with that file.

        This storage is used for files you want to serve externally,
        like proxy images.

        Args:
            sfile (StoredFile): a Boon AI StoredFile object or a StoredFile unique Id.

        Returns:
            str: a path to a location in the local file cache.

        """
        if isinstance(sfile, str):
            file_id = sfile
        else:
            file_id = sfile.id

        _, suffix = os.path.splitext(file_id)
        cache_path = self.cache.get_path(file_id, suffix)

        if not os.path.exists(cache_path):
            try:
                os.makedirs(os.path.dirname(cache_path), exist_ok=True)
            except Exception as e:
                logger.warning(f"Failed to create cache path dir: {cache_path}", e)
            logger.info("localizing file: {}".format(file_id))
            self.app.client.stream('/api/v3/files/_stream/{}'.format(file_id), cache_path)
        return cache_path

    def get_native_uri(self, stored_file):
        """
        Return the file's native url (like gs://).

        Args:
            stored_file (StoredFile): A filed stored in the Zorroa backend.
        Returns:
            str: The native uri.

        """
        return self.app.client.get('/api/v3/files/_locate/{}'
                                   .format(stored_file.id))['uri']


class FileCache(object):
    """
    The LocalFileCache provides a temporary place for storing source and
    support files such as thumbnails for processing.

    """
    supported_schemes = [
        "gs",
        "http",
        "https"
        "file",
        "boonsdk",
        "s3"
    ]
    """
    List of supported URI schemas.
    """

    def __init__(self, app):
        """
        Create a new LocalFileCache instance.
        """
        self.root = None
        self.app = app

    def __init_root(self):
        """
        This method builds the root cache directory when the cache is
        used, otherwise it may leave lots of empty cache temp dirs
        in containers or other places.
        """
        if not self.root:
            task = BoonEnv.get_task_id()
            if not task:
                self.root = tempfile.mkdtemp('boonsdk', 'lfc')
            else:
                self.root = os.path.join(tempfile.gettempdir(), task)
                os.makedirs(self.root, exist_ok=True)

    def precache_file(self, sfile, src_path):
        """
        Precache the src_file to the cache location for the provided StoredFile.

        Args:
            sfile (StoredFile): A StoredFile instance.
            src_path (str): A path to the file to precache.

        Returns:
            str: The precache location.

        """
        if sfile.size == 0:
            raise StorageException(f'Cannot precache {src_path}, file size is 0 bytes.')

        _, suffix = os.path.splitext(sfile.name)
        cache_path = self.get_path(sfile.id, suffix)
        precache_path = urlparse(str(src_path)).path

        # If the tmp file is in the task cache, just symlink it into file storage cache.
        if src_path.startswith(os.environ.get("TMPDIR", "/tmp")):
            symlinked = True
            os.symlink(src_path, cache_path)
        else:
            symlinked = False
            shutil.copy(urlparse(precache_path).path, cache_path)

        logger.debug(f'Pre-caching {src_path}, linked: {symlinked}')
        return cache_path

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
            if uri.startswith('https://www.youtube.com/watch'):
                fname = os.path.basename(uri).replace(".mp4", "")
                subprocess.check_call(
                    ['youtube-dl',
                     '-f', 'mp4',
                     '--output', str(path),
                     f'https://www.youtube.com/watch?v={fname}'], shell=False)
            else:
                urllib.request.urlretrieve(uri, filename=str(path))

        # File URIs
        elif parsed_uri.scheme == 'file':
            return parsed_uri.path

        # Boon AI ML storage
        elif parsed_uri.scheme == 'boonai':
            file_id = parsed_uri.netloc + parsed_uri.path
            self.app.assets.download_file(file_id, path)

        # GCS buckets
        elif parsed_uri.scheme == 'gs':
            # This client uses customer creds.
            gcs_client = get_cached_google_storage_client()
            bucket = gcs_client.get_bucket(parsed_uri.netloc)
            blob = bucket.blob(parsed_uri.path[1:])
            blob.download_to_filename(path)

        # S3 buckets
        elif parsed_uri.scheme == 's3':
            # Using cache, client is slow to connect
            s3_client = get_cached_aws_client('s3')
            s3_client.download_file(parsed_uri.netloc, parsed_uri.path[1:], path)

        # Azure buckets
        elif parsed_uri.scheme == 'azure':
            # Using cache, client is slow to connect
            azure_client = get_cached_azure_storage_client()
            container = azure_client.get_container_client(parsed_uri.netloc)
            blob = container.get_blob_client(parsed_uri.path[1:])
            with open(path, "wb") as fp:
                fp.write(blob.download_blob().readall())

        elif parsed_uri.scheme == '' and parsed_uri.path.startswith("/"):
            path = parsed_uri.path
        else:
            raise StorageException('Invalid URI, unsupported scheme: {}'.format(parsed_uri))
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
        project_id = BoonEnv.get_project_id()
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


class FileStorage(object):
    """
    The FileStorage class handles storing, retrieving and caching files
    from various sources.

    """

    def __init__(self):
        self.app = app_from_env()
        self.cache = FileCache(self.app)
        self.projects = ProjectStorage(self.app, self.cache)
        self.assets = AssetStorage(self.app, self.projects)
        self.models = ModelStorage(self.app, self.projects)

    def localize_file(self, rep):
        """
        Download and and cache the file the given file rep points to
        and return the local file path location.

        Args:
            rep (mixed): Supported types are an Asset, StoredFile, or URI (str).
        Returns:
            str: The local path to the file.

        """
        if isinstance(rep, str):
            return self.cache.localize_uri(rep)
        elif isinstance(rep, Asset):
            # To localize the asset source, we need to check for
            # a "source" entry in the file array, then fall back
            # on the uri.
            source_files = rep.get_files(category="source")
            if source_files:
                return self.projects.localize_file(source_files[0])
            else:
                return self.cache.localize_uri(rep.uri)
        elif isinstance(rep, StoredFile):
            return self.projects.localize_file(rep)
        else:
            raise StorageException(
                f'cannot localize file {rep} unable to determine the remote file source')


class StorageException(BoonSdkException):
    """
    This exception is thrown if there are problems with storing or retrieving a file.
    """
    pass


"""
A local file cache singleton.
"""
file_storage = FileStorage()
