import os


class AbstractObjectFile(object):
    """
    The AbstractObjectFile defines the interface for all concrete implementations of object files.

    Object files represent a single file within an ObjectFileSystem.
    """
    def __init__(self):
        pass

    @property
    def path(self):
        """
        Get the object file's absolute path in local storage.

        This property must be overridden in concrete classes.

        Returns:
            The absolute path to the where this file can be accessed on local
            storage.
        """
        raise NotImplementedError

    def store(self, rfp):
        """
        Store the contents of the given file object to this object file.

        Any and all parent directories necessary for storing the file
        will be created automatically.

        This method must be overridden in concrete classes.

        Args:
            rfp(file): The read file handle.
        """
        raise NotImplementedError

    def open(self, mode="r"):
        """
        Open the underlying file and return a File object of the given mode.

        Args:
            mode(str): the mode to open the file with.

        Returns:
            file: The File object
        """
        raise NotImplementedError

    def mkdirs(self):
        """
        Make all parent directories if the storage_type is 'file'.  If the
        directory already exists or is not necessary, return quietly
        with no error.

        Returns:
            None
        """
        raise NotImplementedError

    def stat(self):
        """
        Stat the file on the backend.

        .. :code: json

            {
                "mediaType": "image/jpeg",
                "size": 53423
                "exists": true
            }

        Returns:
            dict: Size, mediaType, and whether the file exists.
        """
        raise NotImplementedError

    def exists(self):
        """
        Does this object file exist?

        Returns:
            boolean: True if this object file exists on disk; False otherwise.
        """
        raise NotImplementedError

    def sync_local(self):
        """If the file is remote, download the object file to the local cache
        location and return the path. If the ObjectFile is not downloadable
        then None is returned.

        Returns:
            str or None: Path to the tmp location the file was downloaded to or None if
             the file is not downloadable.
        """
        raise NotImplementedError

    def sync_remote(self, rfp):
        """Upload the given file to the remote storage location.

        Args:
            rfp (File): File that should be uploaded.
        """
        raise NotImplementedError

    @property
    def size(self):
        """
        The size of this object file.

        Returns:
            int: The size of the file.
        """
        raise NotImplementedError

    @property
    def id(self):
        """
        The unique ID of this file.
        """
        raise NotImplementedError

    @property
    def name(self):
        """
        The base name of the file.
        """
        return os.path.basename(self.path)

    @property
    def parent(self):
        """
        The parent directory of the file.
        """
        return os.path.dirname(self.path)

    @property
    def type(self):
        """
        The file extension.
        """
        return os.path.basename(self.path).split(".")[-1]

    @property
    def storage_type(self):
        """
        The storage type, ex: 'file' or 'gcs'.
        """
        raise NotImplementedError


class AbstractObjectFileSystem(object):
    """
    The AbstractObjectFileSystem defines the interface for all concrete object file systems.

    An object file system (OFS) is an abstraction for storing and retrieving files where obtaining
    any given file can be achieved by its unique ID only.  The ID is a string comprised of
    a category, a unique string of some kind, a variable list of variant qualifiers, and
    a file extension.

    For example:
        :code:`proxy/1C0D5BBE-7D60-4A38-ABF1-D87C8B5C3472_512sq.png`

    An OFS instance will be set on each Processor while its executing.
    """
    def __init__(self):
        pass

    def init(self):
        """
        Does any initialization required by the file system.
        """
        pass

    def prepare(self, parent_type, parent_id, name):
        """
        Prepare storage for the given file spec.  The file spec consists of a parent_type (like
        asset or folder, singular form), the unique ID of the parent, and a file name.

        Args:
            parent_type (str): The parent type the file is associated with.
            parent_id (str): The unique Id of the parent type.
            name (str): The name of the file.

        Returns:
            AbstractObjectFile: The freshly prepared object file.
        """
        raise NotImplementedError

    def get(self, id):
        """
        Return the ObjectFile the ID points to.

        Args:
            id (str): The unique ID of the file.

        Returns:
            AbstractObjectFile: The object file.
        """
        NotImplementedError
