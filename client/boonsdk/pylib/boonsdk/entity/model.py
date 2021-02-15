from enum import Enum

from .base import BaseEntity
from ..util import as_id

__all__ = [
    'Model',
    'ModelType',
    'Label',
    'LabelScope',
    'ModelTypeInfo'
]


class ModelType(Enum):
    """
    Types of models that can be Trained.
    """

    ZVI_KNN_CLASSIFIER = 0
    """A KMeans clustering model for quickly clustering assets into general groups."""

    ZVI_LABEL_DETECTION = 1
    """Retrain the ResNet50 with your own labels, Using TensorFlow"""

    ZVI_FACE_RECOGNITION = 2
    """Face Recognition model using a KNN classifier."""

    GCP_LABEL_DETECTION = 3
    """Train a Google AutoML vision model."""

    TF2_IMAGE_CLASSIFIER = 4
    """Provide your own custom Tensorflow2/Keras model"""

    ZVI_PYTORCH_LABEL_DETECTION = 5
    """Retrain ResNet50 with your own labels, using Pytorch."""

    PYTORCH_IMAGE_CLASSIFIER = 6
    """Provide your own custom Pytorch model"""


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
        prediction_term_map = {
            ModelType.ZVI_KNN_CLASSIFIER: f'{self.namespace}.label',
            ModelType.ZVI_FACE_RECOGNITION: f'{self.namespace}.predictions.label'
        }
        score_map = {ModelType.ZVI_KNN_CLASSIFIER: f'{self.namespace}.score',
                     ModelType.ZVI_LABEL_DETECTION: f'{self.namespace}.score',
                     ModelType.ZVI_FACE_RECOGNITION: f'{self.namespace}.predictions.score'}
        if self.type not in prediction_term_map:
            raise TypeError(f'Cannot create a confusion matrix search for {self.type} models.')
        search_query = {
            "size": 0,
            "query": {
                "bool": {
                    "filter": [
                        {"range": {score_map[self.type]: {"gte": min_score, "lte": max_score}}}
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
                                                        "field": prediction_term_map[self.type]
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
