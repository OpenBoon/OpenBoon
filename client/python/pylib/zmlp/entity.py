from datetime import datetime


class BaseEntity:

    def __init__(self, data):
        self._data = data

    @property
    def time_created(self):
        """The date/time the entity was created."""
        return datetime.fromtimestamp(self._data['timeCreated'] / 1000.0)

    @property
    def time_modified(self):
        """The date/time the entity was modified."""
        return datetime.fromtimestamp(self._data['timeModified'] / 1000.0)

    @property
    def actor_created(self):
        """The UUID of the actor that created the entity."""
        return self._data['actorCreated']

    @property
    def actor_modified(self):
        """The UUID of the actor that modified the entity."""
        return self._data['actorModified']
