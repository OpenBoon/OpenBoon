from datetime import datetime


class BaseEntity:

    def __init__(self, data):
        self._data = data

    @property
    def id(self):
        """The id of the Entity"""
        return self._data['id']

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

    def __hash__(self):
        return hash(self._data['id'])

    def __eq__(self, other):
        return self._data['id'] == getattr(other, 'id', None)

    def __str__(self):
        vals = [self.__class__.__name__, self.id]
        name = self._data.get('name')
        if name:
            vals.append(name)
            return "<{} id={} name={}>".format(*vals)
        else:
            return "<{} id={}>".format(*vals)
