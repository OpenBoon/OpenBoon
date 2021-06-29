from boonsdk.util import as_id

from boonsdk.entity.boonlib import BoonLib, BaseEntity

__all__ = [
    'BoonLibApp'
]


class BoonLibApp:
    """
    The IndexApp handles managing per-project ES indexes.

    """
    def __init__(self, bz, app):
        self.bz = bz
        self.app = app

    def create_boonlib(self, entity, entity_id, name, description):
        """
        Create a BoonLib from a supported Entity.

        Args:
            entity (obj): A supported entity type.
            entity_id (str): The ID of the entity.
            name (str): The name of the BoonLib.
            description (str): A description for the BoonLob.

        Returns:
            BoonLib: The new Boonlib.
        """
        if isinstance(entity, BaseEntity):
            clz = entity.__class__.__name__
        else:
            clz = str(entity)
        req = {
            'entityId': as_id(entity_id),
            'entity': clz,
            'name': name,
            'description': description
        }

        return BoonLib(self.app.client.post('/api/v3/boonlibs', req))
