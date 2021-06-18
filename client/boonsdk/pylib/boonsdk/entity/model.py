from enum import Enum

from .base import BaseEntity
from ..filters import TrainingSetFilter

__all__ = [
    'Model',
    'ModelType',
    'ModelTypeInfo',
    'PostTrainAction'
]


class ModelType(Enum):
    """
    Types of models that can be Trained.
    """

    KNN_CLASSIFIER = 0
    """A KMeans clustering model for quickly clustering assets into general groups."""

    TF_CLASSIFIER = 1
    """Retrain the ResNet50 with your own labels, Using TensorFlow"""

    FACE_RECOGNITION = 2
    """Face Recognition model using a KNN classifier."""

    GCP_AUTOML_CLASSIFIER = 3
    """Train a Google AutoML vision model."""

    TF_SAVED_MODEL = 4
    """Provide your own custom Tensorflow2/Keras model"""

    PYTORCH_CLASSIFIER = 5
    """Retrain ResNet50 with your own labels, using Pytorch."""

    PYTORCH_MODEL_ARCHIVE = 6
    """Provide your own custom Pytorch model"""


class PostTrainAction(Enum):
    """
    Actions to take after the model training process is complete.
    """

    NONE = 0
    """No action is taken."""

    APPLY = 1
    """The model is applied to either a custom search or the default apply search."""

    TEST = 2
    """The model is applied to any asset with test labels."""


class Model(BaseEntity):

    def __init__(self, data):
        super(Model, self).__init__(data)

    @property
    def name(self):
        """The name of the Model"""
        return self._data['name']

    @property
    def dataset_id(self):
        """The Dataset unique ID"""
        return self._data['datasetId']

    @property
    def module_name(self):
        """The name of the Pipeline Module"""
        return self._data['moduleName']

    @property
    def namespace(self):
        """The name of the Pipeline Module"""
        return 'analysis.{}'.format(self._data['moduleName'])

    @property
    def type(self):
        """The type of model"""
        return ModelType[self._data['type']]

    @property
    def file_id(self):
        """The file ID of the trained model"""
        return self._data['fileId']

    @property
    def ready(self):
        """
        True if the model is fully trained and ready to use.
        Adding new labels will set ready to false.
        """
        return self._data['ready']

    @property
    def training_args(self):
        """
        A dictionary of manually set training arguments.
        """
        return self._data['trainingArgs']

    def get_confusion_matrix_search(self, min_score=0.0, max_score=1.0, test_set_only=True):
        """
        Returns a search query with aggregations that can be used to create a confusion
        matrix.

        Args:
            min_score (float): Minimum confidence score to return results for.
            max_score (float): Maximum confidence score to return results for.
            test_set_only (bool): If True only assets with TEST labels will be evaluated.

        Returns:
            dict: A search to pass to an asset search.

        """
        search_query = {
            "size": 0,
            "query": {
                "bool": {
                    "filter": [
                        {"range": {f'{self.namespace}.predictions.score': {"gte": min_score, "lte": max_score}}}  # noqa
                    ]
                }
            },
            "aggs": {
                "nested_labels": {
                    "nested": {
                        "path": "labels"
                    },
                    "aggs": {
                        "model_train_labels": {
                            "filter": {
                                "bool": {
                                    "must": [
                                        {
                                            "term": {
                                                "labels.modelId": self.id
                                            }
                                        }
                                    ]
                                }
                            },
                            "aggs": {
                                "labels": {
                                    "terms": {
                                        "field": "labels.label",
                                        "size": 1000
                                    },
                                    "aggs": {
                                        "predictions_by_label": {
                                            "reverse_nested": {},
                                            "aggs": {
                                                "predictions": {
                                                    "terms": {
                                                        "field": f'{self.namespace}.predictions.label',  # noqa
                                                        "size": 1000
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if test_set_only:
            (search_query
             ['aggs']
             ['nested_labels']
             ['aggs']
             ['model_train_labels']
             ['filter']
             ['bool']
             ['must'].append({"term": {"labels.scope": "TEST"}}))
        return search_query

    def asset_search_filter(self, scopes=None, labels=None):
        """
        Create and return a TrainingSetFilter for filtering Assets by the Dataset assigned
        to this model.

        Args:
            scopes (list): A optional list of LabelScopes to filter by.
            labels (list): A optional list of labels to filter by.

        Returns:
            TrainingSetFilter: A preconfigured TrainingSetFilter
        """
        if not self.dataset_id:
            raise ValueError('This model does not have an attached Dataset')
        return TrainingSetFilter(self.dataset_id, scopes=scopes, labels=labels)


class ModelTypeInfo:
    """
    Additional properties related to each ModelType.
    """
    def __init__(self, data):
        self._data = data

    @property
    def name(self):
        """The name of the model type."""
        return self._data['name']

    @property
    def description(self):
        """The description of the model type."""
        return self._data['description']

    @property
    def objective(self):
        """The objective of the model, LABEL_DETECTION, FACE_RECOGNITION, etc"""
        return self._data['objective']

    @property
    def provider(self):
        """The company that maintains the structure and algorithm for the model."""
        return self._data['provider']

    @property
    def min_concepts(self):
        """The minimum number of unique concepts a model must have before it can be trained."""
        return self._data['minConcepts']

    @property
    def min_examples(self):
        """
        The minimum number of examples per concept a
        model must have before it can be trained.
        """
        return self._data['minExamples']
