from enum import Enum

from .base import BaseEntity


class BoonLibEntity(Enum):

    Dataset = 0
    """A Dataset BoonLib"""


class BoonLib(BaseEntity):
    """Represents an entity that can be imported into a project."""

    def __init__(self, data):
        super(BoonLib, self).__init__(data)

    @property
    def name(self):
        """The name of the Boonlib"""
        return self._data['name']

    @property
    def entity(self):
        """The Entity the BoonLib will create if imported."""
        return BoonLibEntity[self._data['entity']]

    @property
    def entity_type(self):
        """The type of Entity """
        return self._data['entityType']

    @property
    def description(self):
        return self._data['description']
