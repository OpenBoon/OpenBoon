from enum import Enum

from .base import BaseEntity
from ..util import as_id
from ..filters import TrainingSetFilter

__all__ = [
    'Model',
    'ModelType',
    'Label',
    'LabelScope',
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

    TF_UPLOADED_CLASSIFIER = 4
    """Provide your own custom Tensorflow2/Keras model"""

    PYTORCH_CLASSIFIER = 5
    """Retrain ResNet50 with your own labels, using Pytorch."""

    PYTORCH_UPLOADED_CLASSIFIER = 6
    """Provide your own custom Pytorch model"""


class LabelScope(Enum):
    """
    Types of label scopes
    """
    TRAIN = 1
    """The label marks the Asset as part of the Training set."""

    TEST = 2
    """The label marks the Asset as part of the Test set."""


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

    def get_label_search(self, scope=None):
        """
        Return a search that can be used to query all assets
        with labels.

        Args:
            scope (LabelScope): An optional label scope to filter by.

        Returns:
            dict: A search to pass to an asset search.
        """
        search = {
            'size': 64,
            'sort': [
                '_doc'
            ],
            '_source': ['labels', 'files'],
            'query': {
                'nested': {
                    'path': 'labels',
                    'query': {
                        'bool': {
                            'must': [
                                {'term': {'labels.modelId': self.id}}
                            ]
                        }
                    }
                }
            }
        }

        if scope:
            must = search['query']['nested']['query']['bool']['must']
            must.append({'term': {'labels.scope': scope.name}})

        return search

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
                                        {"term": {"labels.modelId": self.id}}
                                    ]
                                }
                            },
                            "aggs": {
                                "labels": {
                                    "terms": {"field": "labels.label"},
                                    "aggs": {
                                        "predictions_by_label": {
                                            "reverse_nested": {},
                                            "aggs": {
                                                "predictions": {
                                                    "terms": {
                                                        "field": f'{self.namespace}.predictions.label'  # noqa
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
        Create and return a TrainingSetFilter for filtering Assets by this particular label.

        Args:
            scopes (list): A optional list of LabelScopes to filter by.
            labels (list): A optional list of labels to filter by.

        Returns:
            TrainingSetFilter: A preconfigured TrainingSetFilter
        """
        return TrainingSetFilter(self.id, scopes=scopes, labels=labels)


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

    def asset_search_filter(self):
        """
        Create and return a TrainingSetFilter for filtering Assts by this particular label.

        Returns:
            TrainingSetFilter: A preconfigured TrainingSetFilter
        """
        return TrainingSetFilter(self.model_id, scopes=[self.scope], labels=[self.label])
