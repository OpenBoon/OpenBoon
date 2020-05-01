from .base import BaseEntity

__all__ = [
    'Model',
    'ModelType'
]


class ModelType:
    """
    Types of models that can be Trained.
    """
    FAST_CLASSIFICATON = 'FAST_CLASSIFICATON'
    TF2_XFER_RESNET152 = 'TF2_XFER_RESNET152'
    TF2_XFER_VGG16 = 'TF2_XFER_VGG16'
    TF2_XFER_MOBILENET2 = 'TF2_XFER_MOBILENET2'


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
        return self._data['type']

    @property
    def file_id(self):
        """The file ID of the trained model"""
        return self._data['fileId']

    @property
    def training_job_name(self):
        """The name of the model training job, the job may or may not exist."""
        return self._data['trainingJobName']
