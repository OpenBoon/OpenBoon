import json

from zorroa.client import ZorroaJsonEncoder


class AbstractDocumentSerializer(object):
    """Abstract Document serializer object that all concrete Document serializers
    should inherit.

    The object is initialized with a Document and the serialize
    method will return a valid json string. Concrete classes must override the
    _to_dict method.

    Args:
        document(:obj:`Document`): Document to serialize.

    """
    def __init__(self, document):
        self.document = document

    def get_dict(self):
        """Returns a dictionary representation of the document.

        Returns:
            :obj:`dict`: The result
        """
        raise NotImplementedError('You must override the get_dict method to '
                                  'return a dictionary.')

    def get_json(self):
        """Returns a json string representing the document.

        Returns:
            str: The JSON string.
        """
        return json.dumps(self.get_dict(), cls=ZorroaJsonEncoder)


class DefaultDocumentSerializer(AbstractDocumentSerializer):
    """Serializes a document to a basic structure.

    Returns:
        :obj:`dict`: The base struct for serializing an Asset to JSON.
    """
    def get_dict(self):
        _dict = {'id': self.document.id,
                 'type': self.document.type,
                 'document': self.document.dict(),
                 'replace': self.document.replace,
                 'links': self.document.links}
        return _dict
