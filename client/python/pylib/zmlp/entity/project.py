from .base import BaseEntity

__all__ = [
    'Project'
]


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
