import logging
import os
import tempfile

from ..entity import Model, Job, ModelTypeInfo, StoredFile
from ..training import TrainingSetDownloader
from ..util import as_collection, as_id, zip_directory

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
        Create and retrn a new model .

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
        return Model(self.app.client.get("/api/v3/models/{}".format(as_id(id))))

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

    def train_model(self, model, deploy=False, **kwargs):
        """
        Train the given Model by kicking off a model training job.

        Args:
            model (Model): The Model instance or a unique Model id.
            deploy (bool): Deploy the model on your production data immediately after training.
            **kwargs (kwargs): Model training arguments which differ based on the model..

        Returns:
            Job: A model training job.
        """
        model_id = as_id(model)
        body = {
            'deploy': deploy,
            'args': dict(kwargs)
        }
        return Job(self.app.client.post('/api/v3/models/{}/_train'.format(model_id), body))

    def deploy_model(self, model, search=None, file_types=None):
        """
        Apply the model to the given search.

        Args:
            model (Model): A Model instance or a model unique Id.
            search (dict): An arbitrary asset search, defaults to using the
                deployment search associated with the model
            file_types (list): An optional file type filer, can be combination of
                "images", "documents", and "videos"

        Returns:
            Job: The Job that is hosting the reprocess task.
        """
        mid = as_id(model)
        body = {
            "search": search,
            "fileTypes": file_types,
            "jobId": os.environ.get("ZMLP_JOB_ID")
        }
        return Job(self.app.client.post(f'/api/v3/models/{mid}/_deploy', body))

    def upload_custom_model(self, model, dir_path, labels=None):
        """
        Uploads a directory containing a Pytorch or Tensorflow model.   The zip file
        must contain the model binary files and a labels.txt file.  See the docs
        on prepping the model zip file for upload.

        Args:
            model (Model): The Model or te unique Model ID.
            dir_path (str): The path to the zip file.
            labels (list): The list of labels.  You can also pre-copy the labels in manually.

        Returns:
            StoredFile: The StoredFile record of the model.
        """
        if labels:
            with open(dir_path + '/labels.txt', 'w') as fp:
                for label in labels:
                    fp.write(f'{label}\n')

        model_file = tempfile.mkstemp(prefix="model_", suffix=".zip")[1]
        zip_file_path = zip_directory(dir_path, model_file)
        mid = as_id(model)
        return StoredFile(self.app.client.send_file(
            f'/api/v3/models/{mid}/_upload', zip_file_path))

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

    def get_all_model_type_info(self):
        """
        Get all available ModelTypeInfo options.

        Returns:
            list: A list of ModelTypeInfo
        """
        return [ModelTypeInfo(info) for info in self.app.client.get('/api/v3/models/_types')]
