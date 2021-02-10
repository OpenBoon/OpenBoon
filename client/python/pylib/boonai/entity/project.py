from enum import Enum

from .base import BaseEntity

__all__ = [
    'Project',
    'ProjectTier'
]


class ProjectTier(Enum):
    """
    ProjectTiers determine which features are available to a project.s
    """

    ESSENTIALS = 0
    """Allows the use of essentials features."""

    PREMIER = 1
    """Allows the use of premier features."""


class Project(BaseEntity):
    """
    Represents a ZMLP Project.

    """
    def __init__(self, data):
        super(Project, self).__init__(data)

    @property
    def name(self):
        """The project's unique name."""
        return self._data['name']

    @property
    def id(self):
        """The project's unique id."""
        return self._data['id']

    @property
    def tier(self):
        """The project billing tier"""
        return ProjectTier[self._data['tier']]
