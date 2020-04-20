from .base import BaseEntity

__all__ = [
    'DataSet',
    'DataSetLabel',
    'DataSetType'
]


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

    def make_label(self, label, bbox=None):
        """
        Make an instance of a DataSetLabel which can be used
        to label assets.

        Args:
            label (str): The label name.
            bbox (list[float]): A open bounding box.

        Returns:
            DataSetLabel: The new label.
        """
        return DataSetLabel(self,label, bbox)


class DataSetLabel:
    """
    A Label that can be added to an Asset either at import time
    or once the Asset has been imported.
    """
    def __init__(self, dataset, label, bbox=None):
        self.dataset_id = getattr(dataset, 'id', dataset)
        self.label = label
        self.bbox = bbox

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        return {
            "dataSetId": self.dataset_id,
            "label": self.label,
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
