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

    KMEANS_LABEL_DETECTION = 0
    """A kmeans fast classification model that works with just a single example."""

    RESNET152_LABEL_DETECTION = 1
    """Tensorflow2 and Resnet152 to transfer learning model."""

    VGG16_LABEL_DETECTION = 2
    """Tensorflow2 and VGG16 to transfer learning model."""

    MOBILENET2_LABEL_DETECTION = 3
    """Tensorflow2 and Mobilenet2 to transfer learning model."""


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
