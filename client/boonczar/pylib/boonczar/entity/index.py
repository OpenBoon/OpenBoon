from enum import Enum
from boonsdk.entity.base import BaseEntity


__all__ = [
    'Index',
    'IndexTask',
    'IndexState',
    'IndexTaskState',
    'IndexTaskType',
    'IndexSize'
]


class IndexState(Enum):
    """
    Determines the state of the the index.
    """

    READY = 0
    """The index is ready for use."""
    CLOSED = 1
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


class IndexSize(Enum):
    """
    Used to determine the shard and replica values for a new index.
    """

    XSMALL = 0
    """This is for very small test projects, 1 shard, 0 replicas"""
    SMALL = 1
    """This is the default for new projects, 2 shards, 1 replica"""
    MEDIUM = 2
    """Projects with 100,000 to 200,000 assets, 3 shards, 1 replica"""
    LARGE = 3
    """Projects with 200,000 to 2M assets, 5 shards, 1 replica"""
    XLARGE = 4
    """Projects with 2M+ assets, 7 shards, 1 replicas"""


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

    @property
    def project_name(self):
        return self._data['projectName']

    @property
    def major_version(self):
        return self._data['majorVer']

    @property
    def minor_version(self):
        return self._data['minorVer']

    @property
    def version(self):
        return "{}.{}".format(self._data['majorVer'], self._data['minorVer'])

    def __str__(self):
        return "<Index id={} url={} mapping={}>".format(
            self.id, self.index_url, self.mapping_version)

    def __repr__(self):
        return str(self)


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
