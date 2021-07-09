import logging

from ..entity import Model, Job, ModelType, ModelTypeInfo, PostTrainAction
from ..util import as_collection, as_id, \
    is_valid_uuid, as_name_collection, as_id_collection, enum_name

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

    def create_model(self, name, type, dataset=None):
        """
        Create and return a new model .

        Args:
            name (str): The name of the model.
            type (ModelType): The type of Model, see the ModelType class.
            dataset (DataSet): An optional DataSet for training or testing the model.

        Returns:
            Model: The new model.
        """
        body = {
            "name": name,
            "type": getattr(type, 'name', str(type)),
            "datasetId": as_id(dataset)
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
        if is_valid_uuid(as_id(id)):
            return Model(self.app.client.get("/api/v3/models/{}".format(as_id(id))))
        else:
            return self.find_one_model(name=id)

    def find_one_model(self, id=None, name=None, type=None, dataset=None):
        """
        Find a single Model based on various properties.

        Args:
            id (str): The ID or list of Ids.
            name (str): The Model name or list of names.
            type (str): The Model type or list of types.
            dataset(str): Datasets or unique Dataset Ids.
        Returns:
            Model: the matching Model.
        """
        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'datasetIds': as_id_collection(dataset),
            'types': as_name_collection(type)
        }
        return Model(self.app.client.post("/api/v3/models/_find_one", body))

    def find_models(self, id=None, name=None, type=None, dataset=None, limit=None, sort=None):
        """
        Find a single Model based on various properties.

        Args:
            id (str): The ID or list of Ids.
            name (str): The model name or list of names.
            type (str): The model type or list of types.
            dataset(str): Datasets or unique Dataset Ids.
            limit (int): Limit results to the given size.
            sort (list): An array of properties to sort by. Example: ["name:asc"]

        Returns:
            generator: A generator which will return matching Models when iterated.

        """
        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'datasetIds': as_id_collection(dataset),
            'types': as_name_collection(type),
            'sort': sort
        }
        return self.app.client.iter_paged_results('/api/v3/models/_search', body, limit, Model)

    def train_model(self, model, post_action=PostTrainAction.NONE, train_args=None):
        """
        Train the given Model by kicking off a model training job.  If a post action is
        specified the training job will expand once training is complete.

        Args:
            model (Model): The Model instance or a unique Model id
            post_action (PostTrainAction): An action to take once the model is trained.
            train_args (dict): A dictionary of training arg values.
        Returns:
            Job: A model training job.
        """
        model_id = as_id(model)
        body = {}
        body['postAction'] = enum_name(PostTrainAction, post_action, True)
        if train_args:
            body['trainArgs'] = train_args
        return Job(self.app.client.post('/api/v4/models/{}/_train'.format(model_id), body))

    def apply_model(self, model, search=None):
        """
        Apply the latest model.

        Args:
            model (Model): A Model instance or a model unique Id.
            search (dict): An arbitrary asset search, defaults to using the
                apply search associated with the model.
        Returns:
            Job: The Job that is hosting the reprocess task.
        """
        mid = as_id(model)
        body = {
            "search": search
        }
        return Job(self.app.client.post(f'/api/v3/models/{mid}/_apply', body))

    def test_model(self, model):
        """
        Apply the latest model to any asset with test labels.

        Args:
            model (Model): A Model instance or a model unique Id.

        Returns:
            Job: The Job that is hosting the reprocess task.
        """
        mid = as_id(model)
        return Job(self.app.client.post(f'/api/v3/models/{mid}/_test', {}))

    def upload_trained_model(self, model, model_path):
        """
        Upload a trained model directory to Boon AI.

        Args:
            model (Model): The model object or it's unique ID.
            model_path (str): The path to a directory containing the proper files.

        Returns:
            dict: a dict describing the newly published Analysis Module.
        """
        # Make sure we have the model object so we can check its type
        mid = as_id(model)
        model = self.find_one_model(id=mid)

        # check the model types.
        if model.type not in (ModelType.TORCH_MAR_CLASSIFIER, ModelType.TORCH_MAR_DETECTOR):
            raise ValueError(f'Invalid model type for upload: {model.type}')

        self.app.client.send_file(f'/api/v3/models/{mid}/_upload', model_path)

    def export_trained_model(self, model, dst_file, tag='latest'):
        """
        Download a zip file containing the model.

        Args:
            model (Model): The Model instance.
            dst_file (str): path to store the model file.
            tag (str): The model version tag.

        Returns:
            (int) The size of the downloaded file.
        """
        file_id = model.file_id.replace('__TAG__', tag)
        return self.app.client.download_file(file_id, dst_file)

    def get_model_type_info(self, model_type):
        """
        Get additional properties concerning a specific model type.

        Args:
            model_type (ModelType): The model type Enum or name.

        Returns:
            ModelTypeInfo: Additional properties related to a model type.
        """
        type_name = getattr(model_type, 'name', str(model_type))
        return ModelTypeInfo(self.app.client.get(f'/api/v3/models/_types/{type_name}'))

    def get_model_type_training_args(self, model_type):
        """
        Return a dictionary describing the available training args for a given Model.

        Args:
            model_type: (ModelType): A Model or ModelType object.
        Returns:
            dict: A dict describing the argument structure.
        """
        if isinstance(model_type, ModelType):
            name = model_type.name
        elif isinstance(model_type, Model):
            name = model_type.type.name
        else:
            name = model_type.upper()
        return self.app.client.get(f'/api/v3/models/_types/{name}/_training_args')

    def get_all_model_type_info(self):
        """
        Get all available ModelTypeInfo options.

        Returns:
            list: A list of ModelTypeInfo
        """
        return [ModelTypeInfo(info) for info in self.app.client.get('/api/v3/models/_types')]

    def get_model_version_tags(self, model):
        """
        Return a list of model version tags.

        Args:
            model (Model): The model or unique model id.

        Returns:
            list: A list of model version tags.
        """
        return self.app.client.get('/api/v3/models/{}/_tags'.format(as_id(model)))

    def approve_model(self, model):
        """
        Copies your latest model to the approved model version tag, which
        allows you to train and test your model with no interruption to
        the Analysis Module being used by file ingestion services.

        Args:
            model (Model): The model or unique model id.

        Returns:
            dict: A status dict.
        """
        return self.app.client.post('/api/v3/models/{}/_approve'.format(as_id(model)))

    def set_training_args(self, model, args):
        """
        Replaces the training args for a a given model. Training args allow you to override
        certain training options. You can get the full list of args by calling
        the get_training_arg_schema() method.

        Args:
            model (Model): The model or unique model id.
            args: (dict): A dictionary of arguments.

        Returns:
            dict: The new args
        """
        return self.app.client.put(f'/api/v3/models/{as_id(model)}/_training_args', args)

    def set_training_arg(self, model, key, value):
        """
        Set a single training arg for a given mode. Training args allow you to override.
        Certain training options. You can get the full list of args by calling
        the get_training_arg_schema() method.

        Args:
            model: (Model): The model or unique model id.
            key: (str): The field name.
            value (mixed): A valid valu for the given arg.

        Returns:
           dict: The new args
        """
        body = {
            key: value
        }
        return self.app.client.patch(f'/api/v3/models/{as_id(model)}/_training_args', body)

    def get_training_args(self, model):
        """
        Get the resolved model training args.  This is a dictionary of values
        which include both manually set and overridden values.

        Args:
            model (Model): The model or unique model id.

        Returns:
            dict: The resolved args

        """
        return self.app.client.get(f'/api/v3/models/{as_id(model)}/_training_args')
