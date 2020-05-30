from enum import Enum

from .base import BaseEntity
from ..util import as_id

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
        return DataSetType[self._data['type']]

    def make_label(self, label, bbox=None, simhash=None):
        """
        Make an instance of a DataSetLabel which can be used
        to label assets.

        Args:
            label (str): The label name.
            bbox (list[float]): A open bounding box.
            simhash (str): An associated simhash, if any.
        Returns:
            DataSetLabel: The new label.
        """
        return DataSetLabel(self, label, bbox, simhash)

    def make_label_from_prediction(self, label, prediction):
        """
        Make a label from a prediction.  This will copy the bbox
        and simhash from the prediction, if any.

        Args:
            label (str): A name for the prediction.
            prediction (dict): A prediction from an analysis namespace.s

        Returns:
            DataSetLabel: A new label
        """
        return DataSetLabel(self, label, prediction.get('bbox'), prediction.get('simhash'))


class DataSetLabel:
    """
    A Label that can be added to an Asset either at import time
    or once the Asset has been imported.
    """

    def __init__(self, dataset, label, bbox=None, simhash=None):
        self.dataset_id = as_id(dataset)
        self.label = label
        self.bbox = bbox
        self.simhash = simhash

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        return {
            'dataSetId': self.dataset_id,
            'label': self.label,
            'bbox': self.bbox,
            'simhash': self.simhash
        }


class DataSetType(Enum):
    """
    The various DataSet types.
    """

    LABEL_DETECTION = 0
    """The DataSet contains labels useful for Label Detection"""

    OBJECT_DETECTION = 1
    """The DataSet labels are useful for Object Detection"""

    FACE_RECOGNITION = 2
    """The DataSet labels useful for Face Recognition"""
