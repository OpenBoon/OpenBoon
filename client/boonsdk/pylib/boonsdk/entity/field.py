
from .base import BaseEntity


class CustomField(BaseEntity):
    """
    Fields are used to store your own metadata on an asset.
    """

    def __init__(self, data):
        super(CustomField, self).__init__(data)

    @property
    def type(self):
        """The ES field data type."""
        return self._data['type']

    @property
    def name(self):
        """The base field name."""
        return self._data['name']

    @property
    def path(self):
        """The full path to the ES field."""
        return self._data['path']
