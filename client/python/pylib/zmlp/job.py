
class Job(object):
    """
    A Job represents a background file import process.
    """

    def __init__(self, data):
        self._data = data

    @property
    def id(self):
        """The id of the DataSource"""
        return self._data['id']

    @property
    def name(self):
        """The name of the DataSource"""
        return self._data['name']
