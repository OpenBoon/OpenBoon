import tempfile
import os

__all__ = [
    "CachedFile",
    "LocalFileStorageCache"
]


class CachedFile(object):
    def __init__(self):
        pass


class LocalFileStorageCache(object):

    supported = [
        "file:",
        "gs:",
        "http:",
        "https:"
    ]

    def __init__(self):
        self.root = tempfile.mkdtemp("pixml",
                                     os.environ.get("PIXML_TASK_ID", "local"))

    def localize_uri(self, uri):
        pass

    def localize_file_storage(self, storage):
        pass

_lfs = LocalFileStorageCache()


def get_lfs():
    return _lfs

