from .entity import BaseEntity


class DataSet(BaseEntity):
    """
    A DataSet describes a set of hand tagged Assets with known good labels.
    """
    def __init__(self, data):
        super(DataSet, self).__init__(data)

    @property
    def id(self):
        """The id of the DataSet"""
        return self._data['id']

    @property
    def name(self):
        """The name of the DataSet"""
        return self._data['name']

    @property
    def type(self):
        """The type of DataSet"""
        return self._data['type']


class DataSetLabel:
    """
    A Label that can be added to an Asset either at import time
    or once the Asset has been imported.
    """
    def __init__(self, id, label, bbox=None):
        self.id = getattr(id, 'id', id)
        self.label = label
        self.bbox = bbox

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        return {
            "id": self.id,
            "label": self.get_attr("source.path"),
            "bbox": self.bbox
        }


class DataSetType:
    """
    The various DataSet types.
    """

    LabelDetection = "LabelDetection"
    """The DataSet contains labels useful for LabelDetection"""

    ObjectDetection = "ObjectDetection"
    """The DataSet labels are useful for ObjectDetection"""

    FaceRecognition = "FaceRecognition"
    """The DataSet labels useful for FaceRecognition"""
