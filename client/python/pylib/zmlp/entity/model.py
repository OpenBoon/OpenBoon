from enum import Enum

from .base import BaseEntity
from ..util import as_id


__all__ = [
    'Model',
    'ModelType',
    'Label',
    'LabelScope'
]


class ModelType(Enum):
    """
    Types of models that can be Trained.
    """

    ZVI_CLUSTERING = 0
    """A KMeans clustering model for quickly clustering assets into general groups."""

    ZVI_LABEL_DETECTION = 1
    """Retrain the ResNet50 convolutional neural network with your own labels."""

    ZVI_FACE_RECOGNITION = 2
    """Face Recognition model using a KNN classifier."""


class LabelScope(Enum):
    """
    Types of label scopes
    """
    TRAIN = 1
    """The label marks the Asset as part of the Training set."""

    TEST = 2
    """The label marks the Asset as part of the Test set."""


class Model(BaseEntity):

    def __init__(self, data):
        super(Model, self).__init__(data)

    @property
    def name(self):
        """The name of the Model"""
        return self._data['name']

    @property
    def module_name(self):
        """The name of the Pipeline Module"""
        return self._data['moduleName']

    @property
    def type(self):
        """The type of model"""
        return ModelType[self._data['type']]

    @property
    def file_id(self):
        """The file ID of the trained model"""
        return self._data['fileId']

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

    def make_label_from_prediction(self, label, prediction, scope=None):
        """
        Make a label from a prediction.  This will copy the bbox
        and simhash from the prediction, if any.

        Args:
            label (str): A name for the prediction.
            prediction (dict): A prediction from an analysis namespace.s
            scope (LabelScope): The scope of the image, can be TEST or TRAIN.
                Defaults to TRAIN.
        Returns:
            Label: A new label
        """
        return Label(self, label,
                     bbox=prediction.get('bbox'),
                     simhash=prediction.get('simhash'),
                     scope=scope)


class Label:
    """
    A Label that can be added to an Asset either at import time
    or once the Asset has been imported.
    """

    def __init__(self, model, label, bbox=None, simhash=None, scope=None):
        """
        Create a new label.

        Args:
            model: (Model): The model the label is for.
            label (str): The label itself.
            bbox (list): A optional list of floats for a bounding box.
            simhash (str): An optional similatity hash.
            scope (LabelScope): The scope of the image, can be TEST or TRAIN.
                Defaults to TRAIN.
        """
        self.model_id = as_id(model)
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
            'modelId': self.model_id,
            'label': self.label,
            'bbox': self.bbox,
            'simhash': self.simhash,
            'scope': self.scope.name
        }
