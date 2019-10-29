import shutil
import tempfile
import urllib
import os
from filecmp import cmp

import requests
import hashlib
import uritools

from pathlib2 import Path

from zorroa.zsdk.document.base import Document
from zorroa.zsdk.ofs.core import AbstractObjectFile, AbstractObjectFileSystem
from zorroa.client import get_client


def resolve_hashable_ofs_name(value):
    result = value
    if isinstance(value, Document):
        result = value.id
    if not value:
        raise ValueError('The file must have a name')
    return result


class ObjectFile(AbstractObjectFile):
    """
    An ObjectFile represents a single file with an ObjectFileSystem. This class is a base
    class that should be inherited from when creating any concrete ObjectFile classes.
    """
    def __init__(self, data, system):
        super(ObjectFile, self).__init__()
        self._data = data

        # Need a reference to the system to delete/refresh ourself.
        # in the future.
        self.system = system

    @property
    def path(self):
        if self.storage_type != "file":
            sha1 = hashlib.sha1()
            sha1.update(self._data["id"].encode('utf-8'))
            key = sha1.hexdigest()
            name = os.path.basename(self._data["uri"])
            return str(Path(tempfile.gettempdir()).joinpath(key).joinpath(name))
        else:
            return urllib.request.url2pathname(uritools.urisplit(self._data["uri"]).path)

    def store(self, rfp):
        if isinstance(rfp, str):
            rfp = open(rfp, 'rb')
        elif isinstance(rfp, Path):
            rfp = rfp.open('rb')

        self.sync_remote(rfp)
        return self

    def mkdirs(self):
        Path(self.path).parent.mkdir(parents=True, exist_ok=True, mode=0o755)

    def open(self, mode="r"):
        if self.storage_type != "file":
            self.sync_local()
        return open(self.path, mode)

    def exists(self):
        """
        Makes a stat call to the server to determine if the file exists.  Return
        True if it does, false if it doesn't.

        Returns: bool
        """
        return self.stat()["exists"]

    @property
    def size(self):
        return self._data["size"]

    @property
    def id(self):
        return self._data["id"]

    @property
    def mediatype(self):
        return self._data.get('mediaType')

    @property
    def storage_type(self):
        return self._data['scheme']

    def sync_local(self):
        if self.storage_type == "file":
            return self.path

        destination_path = Path(self.path)
        if not destination_path.exists():
            url = self.system.client.get(
                '/api/v1/file-storage/%s/_download-uri' % self.id)['uri']
            destination_path.parent.mkdir(exist_ok=True, parents=True)
            urllib.request.urlretrieve(url, filename=str(destination_path))
        return self.path

    def sync_remote(self, rfp):

        # Request a store URI, even for local files as this signals the
        # server to prepare for a file to be written.
        url = self.system.client.get(
            '/api/v1/file-storage/%s/_upload-uri' % self.id)['uri']

        if self.storage_type == 'file':
            self.mkdirs()
            with open(self.path, 'wb') as wfp:
                wfp.write(rfp.read())
        else:
            response = requests.put(url, headers={'Content-Type': self.mediatype}, data=rfp)
            response.raise_for_status()
            local_path = Path(self.path)
            try:
                local_path.parent.mkdir(parents=True, exist_ok=True)
            except OSError as e:
                # This handles a race case that is difficult to recreate with tests.
                if e.errno == os.errno.EEXIST:
                    pass
                else:
                    raise
            shutil.copyfile(rfp.name, str(local_path))

    def stat(self):
        """
        Stat the file on the backend. Returns size, mediaType, and if it exists.
        {
            "mediaType": "image/jpeg",
            "size": 53423
            "exists": true
        }

        Returns: dict
        """
        return self.system.client.get('/api/v1/file-storage/%s/_stat' % self.id)

    def __str__(self):
        return self.path

    def __repr__(self):
        return "<ObjectFile id=%s path=%s>" % (self.id, self.path)

    def __eq__(self, other):
        return self.id == other.id

    def __cmp__(self, other):
        return cmp(other.path, self.path)

    def __hash__(self):
        return hash(self.id)


class ObjectFileSystem(AbstractObjectFileSystem):

    # The base URI used for requesting file storage
    file_storage_uri = "api/v1/file-storage"

    def __init__(self, client=None):
        super(ObjectFileSystem, self).__init__()
        if client:
            self.client = client
        else:
            self.client = get_client()

    def init(self):
        """Does any initialization required by the file system."""
        pass

    def prepare(self, parent_type, parent_id, name):
        """
        Prepare storage for the given file spec.  The file spec consists of a parent_type
        (like asset or folder, singular form), the unique ID of the parent, and a file name.

        Args:
            parent_type (str): The parent type the file is associated with.
            parent_id (str): The unique Id of the parent type.
            name: (str): The name of the file.

        Returns: ObjectFile
        """
        body = self._get_request_body(parent_type, parent_id, name)
        return ObjectFile(self.client.post(self.file_storage_uri, body), self)

    def get(self, id):
        """
        Return an ObjectFile by its unique ID.

        Args:
            id: the ID of the OFS file

        Returns: ObjectFile

        """
        url = "/".join([self.file_storage_uri, id])
        return ObjectFile(self.client.get(url), self)

    def _get_request_body(self, parent_type, parent_id, name):
        """
        Return a file-storage request body.  Return a valid dictionary for making
        a storage request.

        Args:
            parent_type(str): The category for the file.
            parent_id(str): A unique Id for the category
            name(str): the name of the file
        Returns: dict

        """
        # If the parent object has an ID property, use that.
        try:
            parent_id = parent_id.id
        except AttributeError:
            pass

        body = {
            "parentType": parent_type,
            "parentId": parent_id,
            "name": name,
            "jobId": os.environ.get("ZORROA_JOB_ID"),
            "taskId": os.environ.get("ZORROA_TASK_ID")
        }
        return body
