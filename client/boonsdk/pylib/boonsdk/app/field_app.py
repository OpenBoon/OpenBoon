from ..entity.field import CustomField
from ..util import as_id, as_collection, as_id_collection


class CustomFieldApp:
    """
    An App instance for managing custom fields.
    """
    def __init__(self, app):
        self.app = app

    def create_field(self, name, type):
        """
        Create a new Field.  The name of the field will be used as the actual
        ElasticSearch field name.  The name must be alpha-numeric, underscores/dashes
        are allowed, periods are not.

        To reference your custom field in an ES query you must prepend the field name
        with 'custom.'. For example if your field name is 'city', then you must use the
        fully qualified name 'custom.city' in your query.

        Args:
            name (str): The name of the field.
            type (str): The ES field type.

        Returns:
            CustomField: The new custom field.

        """
        body = {
            'name': name,
            'type': type
        }
        return CustomField(self.app.client.post('/api/v3/custom-fields', body))

    def get_field(self, id):
        """
        Get the record for the custom field.

        Args:
            id (str): The id of the field.

        Returns:
             CustomField: The custom field.
        """
        id = as_id(id)
        return CustomField(self.app.client.get(f'/api/v3/custom-fields/{id}'))

    def find_one_field(self, id=None, name=None, type=None):
        """
        Find a single custom field.

        Args:
            id (str): One or more custom field ids.
            name (str): One or more custom field names.
            type (str): One or more custom field types.

        Returns:
            CustomField
        """
        body = {
            'names': as_collection(name),
            'ids': as_id_collection(id),
            'types': as_collection(type)
        }
        return CustomField(self.app.client.post("/api/v3/custom-fields/_find_one", body))

    def find_fields(self, id=None, name=None, type=None, limit=None, sort=None):
        """
        Find matching fields.

        Args:
            id (str): One or more custom field ids.
            name (str): One or more custom field names.
            type (str): One or more custom field types.
            limit (int): Limit the number of results.
            sort (list):  An array of sort columns, for example ["name:asc", "type:desc"]

        Returns:
             generator: A generator which will return matching Fields when iterated.
        """
        body = {
            "ids": as_id_collection(id),
            "names": as_collection(name),
            "type": as_collection(type),
            'sort': sort
        }
        return self.app.client.iter_paged_results(
            '/api/v3/custom-fields/_search', body, limit, CustomField)
