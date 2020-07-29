from enum import Enum

__all__ = [
    'ProjectSize'
]


class ProjectSize(Enum):
    """
    Used to determine the shard and replica values for a new project.
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
