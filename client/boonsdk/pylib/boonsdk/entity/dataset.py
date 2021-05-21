from enum import Enum

from .base import BaseEntity
from ..filters import TrainingSetFilter
from ..util import as_id

__all__ = [
    'LabelScope',
    'Label',
    'DataSet',
    'DataSetType'
]


class DataSetType(Enum):
    """
    Types of models that can be Trained.
    """

    Classification = 0
    """Classify the type of content of an image as a whole"""
    Detection = 1
    """Detect objects within an image."""
    FaceRecognition = 2
    """Recognize faces in an image."""


class DataSet(BaseEntity):

    def __init__(self, data):
        super(DataSet, self).__init__(data)

    @staticmethod
    def as_id(obj):
        """
        Extract a DataSet ID from the given object, if one exists.

        Args:
            obj (mixed): A DataSet, Model with assigned DataSet, or DataSet unique Id.

        Returns:
            str: The DataSet id.
        """
        return getattr(obj, 'dataset_id', as_id(obj))

    @property
    def name(self):
        """The name of the DataSet"""
        return self._data['name']

    @property
    def type(self):
        """The type of DataSet"""
        return DataSetType[self._data['type']]

    @property
    def description(self):
        """Description"""
        return self._data['description']

    def make_label(self, label, bbox=None, simhash=None, scope=None):
        """
        Make an instance of a Label which can be used to label assets.

        Args:
            label (str): The label name.
            bbox (list[float]): A open bounding box.
            simhash (str): An associated simhash, if any.
            scope (LabelScope): The scope of the image, can be TEST or TRAIN.
                Defaults to TRAIN.
        Returns:
            Label: The new label.
        """
        return Label(self, label, bbox=bbox, simhash=simhash, scope=scope)

    def asset_search_filter(self, scopes=None, labels=None):
        """
        Create and return a TrainingSetFilter for filtering Assets by this particular label.

        Args:
            scopes (list): A optional list of LabelScopes to filter by.
            labels (list): A optional list of labels to filter by.

        Returns:
            TrainingSetFilter: A preconfigured TrainingSetFilter
        """
        return TrainingSetFilter(self.id, scopes=scopes, labels=labels)

    def make_label_from_prediction(self, prediction, scope=None, label=None):
        """
        Make a label from a prediction.  This will copy the bbox
        and simhash from the prediction, if any.

        Args:
            prediction (dict): A prediction from an analysis namespace.s
            scope (LabelScope): The scope of the image, can be TEST or TRAIN.
                Defaults to TRAIN.
            label (str): Override the label on the prediction.
        Returns:
            Label: A new label
        """
        return Label(self, label or prediction.get('label'),
                     bbox=prediction.get('bbox'),
                     simhash=prediction.get('simhash'),
                     scope=scope)


class LabelScope(Enum):
    """
    Types of label scopes
    """
    TRAIN = 0
    """The label marks the Asset as part of the Training set."""

    TEST = 1
    """The label marks the Asset as part of the Test set."""


class Label:
    """
    A Label that can be added to an Asset either at import time
    or once the Asset has been imported.
    """

    def __init__(self, ds, label, bbox=None, simhash=None, scope=None):
        """
        Create a new label.

        Args:
            ds: (DataSet): The DataSet of the label.
            label (str): The label itself.
            bbox (list): A optional list of floats for a bounding box.
            simhash (str): An optional similatity hash.
            scope (LabelScope): The scope of the image, can be TEST or TRAIN.
                Defaults to TRAIN.
        """
        self.dataset_id = as_id(ds)
        self.label = label
        self.bbox = bbox
        self.simhash = simhash
        self.scope = scope or LabelScope.TRAIN

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
            'simhash': self.simhash,
            'scope': self.scope.name
        }

    def asset_search_filter(self):
        """
        Create and return a TrainingSetFilter for filtering Assets by this particular label.

        Returns:
            TrainingSetFilter: A preconfigured TrainingSetFilter
        """
        return TrainingSetFilter(self.dataset_id, scopes=[self.scope], labels=[self.label])
