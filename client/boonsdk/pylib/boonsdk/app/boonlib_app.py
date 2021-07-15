
from ..util import is_valid_uuid, as_collection, as_name_collection, as_id
from ..entity.boonlib import BoonLib


class BoonLibApp:
    """
    The Boon Library is a collection of entities that can be imported into your project.
    """

    def __init__(self, app):
        self.app = app

    def import_boonlib(self, id):
        """
        Import the given BoonLib into your project.

        Args:
            id (str): A unique BoonLib ID, name or instance.

        Returns:
            dict: A status dict containing details about the import.
        """
        blib = self.get_boonlib(id)
        return self.app.client.post(f'/api/v3/boonlibs/{blib.id}/_import')

    def get_boonlib(self, id):
        """
        Get a BoonLib record.

        Args:
            id (str): A unique BoonLib ID, name or instance.

        Returns:
            BoonLib: A BoonLib instance.
        """
        if is_valid_uuid(as_id(id)):
            return BoonLib(self.app.client.get(f'api/v3/boonlibs/{id}'))
        else:
            return self.find_one_boonlib(name=id)

    def find_one_boonlib(self, id=None, name=None, type=None):
        """
        Find a single BoonLib instance using given properties.

        Args:
            id (str): A unique BoonLib ID.
            name (str): The unique nane of a BoonLib
            type (BoonLib

        Returns:

        """
        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'types': as_name_collection(type)
        }
        return BoonLib(self.app.client.post("/api/v3/boonlibs/_find_one", body))

    def find_boonlibs(self, id=None, name=None, entity=None, limit=None, sort=None):

        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'entityTypes': as_name_collection(entity),
            'sort': sort
        }
        return self.app.client.iter_paged_results('/api/v3/boonlibs/_search', body, limit, BoonLib)
