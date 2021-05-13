import logging
import os
import tempfile
import zipfile

from ..entity import Model, Job, ModelType, ModelTypeInfo, AnalysisModule, PostTrainAction
from ..training import TrainingSetDownloader
from ..util import as_collection, as_id, zip_directory, is_valid_uuid
from shutil import copyfile
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

    def create_model(self, name, type):
        """
        Create and return a new model .

        Args:
            name (str): The name of the model.
            type (ModelType): The type of Model, see the ModelType class.

        Returns:
            Model: The new model.
        """
        body = {
            "name": name,
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
        if is_valid_uuid(as_id(id)):
            return Model(self.app.client.get("/api/v3/models/{}".format(as_id(id))))
        else:
            return self.find_one_model(name=id)

    def find_one_model(self, id=None, name=None, type=None):
        """
        Find a single Model based on various properties.

        Args:
            id (str): The ID or list of Ids.
            name (str): The model name or list of names.
            type (str): The model type or list of types.
        Returns:
            Model: the matching Model.
        """
        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'types': as_collection(type)
        }
        return Model(self.app.client.post("/api/v3/models/_find_one", body))

    def find_models(self, id=None, name=None, type=None, limit=None, sort=None):
        """
        Find a single Model based on various properties.

        Args:
            id (str): The ID or list of Ids.
            name (str): The model name or list of names.
            type (str): The model type or list of types.
            limit (int): Limit results to the given size.
            sort (list): An arary of properties to sort by. Example: ["name:asc"]

        Returns:
            generator: A generator which will return matching Models when iterated.

        """
        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'types': as_collection(type),
            'sort': sort
        }
        return self.app.client.iter_paged_results('/api/v3/models/_search', body, limit, Model)

    def train_model(self, model, post_action=PostTrainAction.NONE, **kwargs):
        """
        Train the given Model by kicking off a model training job.  If a post action is
        specified the training job will expand once training is complete.

        Args:
            model (Model): The Model instance or a unique Model id
            post_action (PostTrainAction): An action to take once the model is trained.
        Returns:
            Job: A model training job.
        """
        model_id = as_id(model)
        body = {}

        if kwargs.get('deploy'):
            body['postAction'] = PostTrainAction.APPLY.name
        else:
            body['postAction'] = str(post_action)

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

    def upload_trained_model(self, model, model_path, labels):
        """
        Upload a trained model directory to Boon AI.

        Args:
            model (Model): The model object or it's unique ID.
            model_path (str): The path to a directory containing the proper files.
            labels (list): A list of labels, optional if you have a labels.txt file.

        Returns:
            dict: a dict describing the newly published Analysis Module.
        """
        # Make sure we have the model object so we can check its type
        mid = as_id(model)
        model = self.find_one_model(id=mid)
        label_path = f'{model_path}/labels.txt'
        labels_exist = os.path.exists(label_path)

        if not labels and not labels_exist:
            raise ValueError("You must provide an list of labels or a labels.txt file.")

        if labels:
            if labels_exist:
                # delete label file if it exists.
                # handles exported tf ,odel
                os.unlink(label_path)

            with open(label_path, 'w') as fp:
                for label in labels:
                    fp.write(f'{label}\n')

        # check the model types.
        if model.type not in (ModelType.TF_UPLOADED_CLASSIFIER,
                              ModelType.PYTORCH_UPLOADED_CLASSIFIER):
            raise ValueError(f'Invalid model type for upload: {model.type}')

        model_file = tempfile.mkstemp(prefix="model_", suffix=".zip")[1]
        zip_file_path = zip_directory(model_path, model_file)
        mid = as_id(model)

        rsp = AnalysisModule(self.app.client.send_file(
            f'/api/v3/models/{mid}/_upload', zip_file_path))

        os.unlink(zip_file_path)
        return rsp

    def download_and_unzip_model(self, model, model_path):
        """
        Download a trained model from BoonAI storage
        :param model: The model object or it's unique ID.
        :param model_path: The path to a directory that will be downloaded the files
        :return: a dict containing server download response
        """

        mid = as_id(model)
        model = self.find_one_model(id=mid)
        # check the model types.
        if model.type not in (ModelType.TF_UPLOADED_CLASSIFIER,
                              ModelType.PYTORCH_UPLOADED_CLASSIFIER):
            raise ValueError(f'Invalid model type for upload: {model.type}')

        mid = as_id(model)

        file_name = 'model.zip'
        self.app.client.stream("/api/v3/models/{}/_download".format(mid), "{}/{}".format(model_path, file_name))

        def unzip_model_files(model_path, file_name):
            os.chdir(model_path)
            zip_ref = zipfile.ZipFile(file_name)

            # extract to the model path
            tmp_dir = tempfile.mkdtemp()
            zip_ref.extractall(tmp_dir)

            # copying only files
            for root, dirs, files in os.walk(tmp_dir):
                for file in files:
                    path_file = os.path.join(root, file)
                    copyfile(path_file, "{}/{}".format(model_path, file))

            zip_ref.close()
            os.remove(file_name)

        unzip_model_files(model_path, file_name)

        return model_path

    def get_label_counts(self, model):
        """
        Get a dictionary of the labels and how many times they occur.

        Args:
            model (Model): The Model or its unique Id.

        Returns:
            dict: a dictionary of label name to occurrence count.

        """
        return self.app.client.get('/api/v3/models/{}/_label_counts'.format(as_id(model)))

    def rename_label(self, model, old_label, new_label):
        """
        Rename a the given label to a new label name.  The new label can already exist.

        Args:
            model (Model): The Model or its unique Id.
            old_label (str): The old label name.
            new_label (str): The new label name.

        Returns:
            dict: a dictionary containing the number of assets updated.

        """
        body = {
            "label": old_label,
            "newLabel": new_label
        }
        return self.app.client.put('/api/v3/models/{}/labels'.format(as_id(model)), body)

    def delete_label(self, model, label):
        """
        Removes the label from all Assets.

        Args:
            model (Model): The Model or its unique Id.
            label (str): The label name to remove.

        Returns:
            dict: a dictionary containing the number of assets updated.

        """
        body = {
            "label": label
        }
        return self.app.client.delete('/api/v3/models/{}/labels'.format(as_id(model)), body)

    def download_labeled_images(self, model, style, dst_dir, validation_split=0.2):
        """
        Get a TrainingSetDownloader instance which can be used to download all the
        labeled images for a Model to local disk.

        Args:
            model (Model): The Model or its unique ID.
            style (str): The structure style to build: labels_std, objects_keras, objects_coco
            dst_dir (str): The destination dir to write the Assets into.
            validation_split (float): The ratio of training images to validation images.
                Defaults to 0.2.
        """
        return TrainingSetDownloader(self.app, model, style, dst_dir, validation_split)

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
        mtype = getattr(model_type, 'type', model_type).name
        return self.app.client.get(f'/api/v3/models/_types/{mtype}/_training_args')

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
