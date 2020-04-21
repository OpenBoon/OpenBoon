from .base import BaseEntity

__all__ = [
    'DataSource'
]


class DataSource(BaseEntity):
    """
    A DataSource is a remote source for Assets that can be
    iterated by the Analysis framework and imported
    in a single import Job.
    """

    def __init__(self, data):
        super(DataSource, self).__init__(data)

    @property
    def id(self):
        """The id of the DataSource"""
        return self._data['id']

    @property
    def name(self):
        """The name of the DataSource"""
        return self._data['name']

    @property
    def uri(self):
        """The URI of the DataSource"""
        return self._data['uri']

    @property
    def file_types(self):
        """The file type filter for the DataSource"""
        return self._data.get('file_types', [])

    @property
    def modules(self):
        """The type of modules done to the DataSource"""
        return self._data.get('modules', [])

    @property
    def credentials(self):
        """The type of credentials attached to DataSource"""
        return self._data.get('credentials', [])
