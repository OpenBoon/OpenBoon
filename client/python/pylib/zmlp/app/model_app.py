import logging

from ..entity import Model, Job, PipelineMod
from ..util import as_collection, as_id, as_id_collection

logger = logging.getLogger(__name__)

__all__ = [
    'ModelApp'
]


class ModelApp:
    """
    Methods for manipulating models.
    """

    def __init__(self, app):
        self.app = app

    def create_model(self, dataset, type):
        """
        Create a new model from the given DataSet and ModelType.
        Args:
            dataset (DataSet): The DataSet instance or it's unique Id/
            type (ModelType): The type of Model, see the ModelType class.

        Returns:
            Model: The new model.
        """
        body = {
            "dataSetId": as_id(dataset),
            "type": type.name
        }
        return Model(self.app.client.post("/api/v3/models", body))

    def get_model(self, id):
        """
        Get a Model by Id
        Args:
            id (str): The model id.

        Returns:
            Model: The model.
        """
        return Model(self.app.client.get("/api/v3/models/{}".format(as_id(id))))

    def find_one_model(self, id=None, name=None, type=None, dataset=None):
        """
        Find a single Model based on various properties.

        Args:
            id (str): The ID or list of Ids.
            name (str): The model name or list of names.
            type (str): The model type or list of types.
            dataset (DatSet): A DataSet, DataSet Id or list of either type.
        Returns:
            Model: the matching Model.
        """
        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'types': as_collection(type),
            'dataSetIds': as_id_collection(dataset)
        }
        return Model(self.app.client.post("/api/v3/models/_find_one", body))

    def find_models(self, id=None, name=None, type=None, dataset=None, limit=None, sort=None):
        """
        Find a single Model based on various properties.

        Args:
            id (str): The ID or list of Ids.
            name (str): The model name or list of names.
            type (str): The model type or list of types.
            dataset (DataSet): A DataSet, DataSet Id or list of either type.
            limit (int): Limit results to the given size.
            sort (list): An arary of properties to sort by. Example: ["name:asc"]

        Returns:
            generator: A generator which will return matching Models when iterated.

        """
        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'types': as_collection(type),
            'dataSetIds': as_id_collection(dataset),
            'sort': sort
        }
        return self.app.client.iter_paged_results('/api/v3/models/_search', body, limit, Model)

    def train_model(self, model, **kwargs):
        """
        Train the given Model by kicking off a model training job.

        Args:
            model (Model): The Model instance or a unique Model id.
            **kwargs (kwargs): Model training arguments which differ based on the model.

        Returns:
            Job: A model training job.
        """
        model_id = as_id(model)
        body = dict(kwargs)
        return Job(self.app.client.post('/api/v3/models/{}/_train'.format(model_id), body))

    def publish_model(self, model):
        """
        Publish the given model.  The model must have been trained before.  This
        will make the model available for execution using a PipelineModel

        Args:
            model (Model): The Model instance or a unique Model id.

        Returns:
            PipelineMod: A PipelineMod which can be used to execute the model on Data.
        """
        return PipelineMod(
            self.app.client.post('/api/v3/models/{}/_publish'.format(as_id(model))))
