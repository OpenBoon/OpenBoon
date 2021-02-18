from ..entity.field import CustomField
from ..util import as_id


class CustomFieldApp:
    """
    An App instance for managing custom fields.
    """
    def __init__(self, app):
        self.app = app

    def create_custom_field(self, name, type):
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

    def get_custom_field(self, id):
        """
        Get the record for the custom field.

        Args:
            id (str): The id of the field.

        Returns:
             CustomField: The custom field.
        """
        id = as_id(id)
        return CustomField(self.app.client.get(f'/api/v3/custom-fields/{id}'))
