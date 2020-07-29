from enum import Enum
from zmlp.entity.base import BaseEntity


__all__ = [
    'Index',
    'IndexTask',
    'IndexState',
    'IndexTaskState',
    'IndexTaskType'
]


class IndexState(Enum):
    """
    Determines the state of the the index.
    """

    READY = 0
    """The index is ready for use."""
    BUILDING = 1
    """The index is building"""
    PENDING_DELETE = 2
    """The index is closed and will eventually be deleted."""


class IndexTaskType(Enum):
    """
    Determines the type of IndexTask.
    """

    REINDEX = 0
    """The task is reindexing into a new index."""
    UPDATE = 1
    """The task is performing a mass update"""


class IndexTaskState(Enum):
    """
    Determines the state of the the IndexTask.
    """

    RUNNING = 0
    """The IndexTask is running."""
    FINISHED = 1
    """The IndexTask is finished."""


class Index(BaseEntity):
    """
    The Index class represents and ElasticSearch index.

    """
    def __init__(self, data):
        super(Index, self).__init__(data)

    @property
    def project_id(self):
        return self._data['projectId']

    @property
    def cluster_id(self):
        return self._data['clusterId']

    @property
    def cluster_url(self):
        return self._data['clusterUrl']

    @property
    def state(self):
        return IndexState[self._data['state']]

    @property
    def index_name(self):
        return self._data['indexName']

    @property
    def mapping_version(self):
        return "{}-{}.{}".format(self._data['mapping'],
                                 self._data['majorVer'],
                                 self._data['minorVer'])

    @property
    def shards(self):
        return self._data['shards']

    @property
    def replicas(self):
        return self._data['replicas']

    @property
    def index_url(self):
        return self._data['indexUrl']


class IndexTask(BaseEntity):
    """
    An IndexTask represents a long running index modification task.
    """
    def __init__(self, data):
        super(IndexTask, self).__init__(data)

    @property
    def project_id(self):
        return self._data['projectId']

    @property
    def src_index_id(self):
        return self._data['srcIndexRouteId']

    @property
    def dst_index_id(self):
        return self._data['dstIndexRouteId']

    @property
    def name(self):
        return self._data['name']

    @property
    def state(self):
        return IndexTaskState[self._data['state']]

    @property
    def type(self):
        return IndexTaskType[self._data['type']]

    @property
    def es_task_id(self):
        return self._data['esTaskId']
