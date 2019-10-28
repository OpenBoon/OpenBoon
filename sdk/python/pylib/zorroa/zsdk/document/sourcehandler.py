import mimetypes
import os
import tempfile
import uuid

import magic
from google.auth.exceptions import RefreshError
from google.cloud import storage
from google.cloud.exceptions import NotFound
from pathlib2 import Path

from zorroa.zsdk.util import std
from zorroa.zsdk.document.base import logger


class AbstractSourceHandler(object):
    def __init__(self, path):
        self.path = path

    def _create_local_cache_path(self):
        """Creates a local cache location to store the source file. Returns the
         path to store the local file and creates the parent directory.

        Args:
            path (str): Path or URI to the source that will cached locally.

        """
        file_name = '%s%s' % (uuid.uuid4(), Path(self.path).suffix)
        cache_path = Path(tempfile.mkdtemp()).joinpath(file_name)
        cache_path.parent.mkdir(parents=True, exist_ok=True)
        return str(cache_path)

    def get_source_metadata(self):
        """Returns the metadata that should be stored in the "source" namespace
        of a Document as a dict.
        """
        source = {
            'path': self.path,
            'directory': os.path.dirname(self.path),
            'filename': Path(self.path).name,
            'timeCreated': std.str_time_now(),
        }

        # Attempt to set the basename if any.
        try:
            source["basename"] = os.path.join(
                os.path.basename(self.path).rsplit(".", 1)[0])
        except Exception as e:
            logger.warn("Failed to determine basename: %s" % e)

        # Attempt to set an extension if any.
        try:
            source["extension"] = os.path.basename(self.path).split(".")[
                -1].lower()
        except Exception as e:
            logger.warn("Failed to determine file extension: %s" % e)

        # Attempt to determine media type.
        try:
            source["mediaType"] = self._get_mime_type()
            if not source["mediaType"]:
                source["mediaType"] = mimetypes.guess_type(self.path)[0]
            source["type"], source["subType"] = source["mediaType"].split("/",
                                                                          1)
        except Exception as e:
            logger.warn("Failed to determine media type: %s" % e)

        # Determine file size.
        try:
            source["fileSize"] = self._get_file_size() or 0
        except Exception as e:
            logger.warn("Failed to determine file size: %s" % e)

        # Determine if the file exists.
        try:
            source["exists"] = self._get_exists()
        except Exception as e:
            logger.warn("Failed to determine if the file exists: %s" % e)

        return source

    def _get_mime_type(self):
        """Returns a string representing the mimetype of the source."""
        return None

    def _get_file_size(self):
        """Returns the size of the source in bytes as an int."""
        raise NotImplementedError()

    def _get_exists(self):
        """Returns True if the source exists."""
        raise NotImplementedError

    def store_local_cache(self):
        """Stores source file in a local cache location and returns the path as
         str.
         """
        raise NotImplementedError()


class FileSystemSourceHandler(AbstractSourceHandler):
    """Source handler for local file systems."""
    def store_local_cache(self):
        return self.path

    def _get_mime_type(self):
        if Path(self.path).exists():
            return magic.detect_from_filename(self.path).mime_type
        return None

    def _get_file_size(self):
        return os.path.getsize(self.path)

    def _get_exists(self):
        return Path(self.path).exists()


class GcsSourceHandler(AbstractSourceHandler):
    """Source handler for Google Cloud Storage paths expressed as a
    gs:// uri.
    """
    def __init__(self, path):
        super(GcsSourceHandler, self).__init__(path)
        client = storage.Client()
        path_parts = Path(self.path).parts
        try:
            bucket = client.get_bucket(path_parts[1])
        except RefreshError:
            client = storage.Client.create_anonymous_client()
            bucket = client.get_bucket(path_parts[1])
        self.blob = bucket.blob('/'.join(path_parts[2:]))
        try:
            self.blob.reload()
        except NotFound:
            pass

    def store_local_cache(self):
        local_path = self._create_local_cache_path()
        self.blob.download_to_filename(local_path)
        return local_path

    def _get_mime_type(self):
        return self.blob.content_type

    def _get_file_size(self):
        return self.blob.size

    def _get_exists(self):
        return self.blob.exists()


# TODO: Remove this duplicate code once the exports do not rely on it.
def get_source_schema(path):
    """Build and return a source schema for the given path."""
    path = str(path)
    abs_path = os.path.abspath(path)
    source = {
        "path": abs_path,
        "directory": os.path.dirname(abs_path),
        "filename": os.path.basename(abs_path),
        "timeCreated": std.str_time_now()
    }

    # Attempt to set the basename if any.
    try:
        source["basename"] = os.path.join(
            os.path.basename(path).rsplit(".", 1)[0])
    except Exception as e:
        logger.warn("Failed to determine basename: %s" % e)

    # Attempt to set an extension if any.
    try:
        source["extension"] = os.path.basename(path).split(".")[-1]
    except Exception as e:
        logger.warn("Failed to determine file extension: %s" % e)

    # Attempt to determine media type.
    try:
        if Path(path).exists():
            source["mediaType"] = magic.detect_from_filename(path).mime_type
        else:
            source["mediaType"] = mimetypes.guess_type(path)[0]
        source["type"], source["subType"] = source["mediaType"].split("/", 1)
    except Exception as e:
        logger.warn("Failed to determine media type: %s" % e)

    # Determine file size, note that the file might not exist yet.
    try:
        source["fileSize"] = os.path.getsize(path)
        source["exists"] = True
    except Exception as e:
        source["exists"] = False
        logger.warn("Failed to determine file size: %s" % e)

    return source
