from enum import Enum

from .base import BaseEntity

__all__ = [
    'Model',
    'ModelType'
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


class Model(BaseEntity):

    def __init__(self, data):
        super(Model, self).__init__(data)

    @property
    def dataset_id(self):
        """The ID of the DataSet the model was trained from."""
        return self._data['dataSetId']

    @property
    def name(self):
        """The name of the Model"""
        return self._data['name']

    @property
    def type(self):
        """The type of model"""
        return ModelType[self._data['type']]

    @property
    def file_id(self):
        """The file ID of the trained model"""
        return self._data['fileId']
