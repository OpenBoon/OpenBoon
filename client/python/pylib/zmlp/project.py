from datetime import datetime


class Project(object):
    """
    Represents a ZMLP Project.

    """
    def __init__(self, data):
        self._data = data

    @property
    def name(self):
        """The project's unique name."""
        return self._data['name']

    @property
    def id(self):
        """The project's unique id."""
        return self._data['id']

    @property
    def time_created(self):
        """The date/time the project was created."""
        return datetime.fromtimestamp(self._data['timeCreated'] / 1000.0)

    @property
    def time_modified(self):
        """The date/time the project was modified."""
        return datetime.fromtimestamp(self._data['timeModified'] / 1000.0)

    @property
    def actor_created(self):
        """The UUID of the actor that created the project."""
        return self._data['actorCreated']

    @property
    def actor_modified(self):
        """The UUID of the actor that modified the project."""
        return self._data['actorModified']
