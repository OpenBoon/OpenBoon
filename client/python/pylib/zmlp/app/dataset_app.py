import logging
import os

from zmlp.client import ZmlpRequestException
from ..entity import DataSet
from ..util import as_collection, as_id

logger = logging.getLogger(__name__)

__all__ = [
    'DataSetApp',
    'DataSetDownloader'
]


class DataSetApp:

    def __init__(self, app):
        self.app = app

    def create_dataset(self, name, type):
        """
        Create a new DataSet of the given type.  DataSets must have
        unique names. Once the type is set, it cannot be changed.

        Args:
            name (str): The name of the DataSet.
            type (str):The type of DataSet.

        Returns:
            DataSet: The new DataSet
        """

        dataset = self.find_one_dataset(name=name)
        if dataset is None:
            req = {
                'name': name,
                'type': type
            }

            dataset = DataSet(self.app.client.post('/api/v1/data-sets', req))

        return dataset

    def get_dataset(self, id):
        """
        Get a DataSet by its unique Id.

        Args:
            id (str): The dataset id.

        Returns:
            DataSet: The DataSet
        """
        return DataSet(self.app.client.get('/api/v1/data-sets/{}'.format(id)))

    def find_one_dataset(self, id=None, name=None, type=None):
        """
        Find a single DataSet.

        Args:
            id (mixed): An ID or collection of IDs to filter on.
            name (mixed): A name or collection of names to filter on.
            type: (mixed): A DataSet type or collection of types to filter on.

        Returns:
            DataSet: The matching DataSet

        """
        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'types': as_collection(type),
        }

        try:
            dataset = self.app.client.post('/api/v3/data-sets/_find_one', body)
        except ZmlpRequestException:
            dataset = None

        return dataset

    def find_datasets(self, id=None, name=None, type=None, limit=None, sort=None):
        """
        Search for datasets.

        Args:
            id (mixed): An ID or collection of IDs to filter on.
            name (mixed): A name or collection of names to filter on.
            type: (mixed): A DataSet type or collection of types to filter on.
            limit: (int) Limit the number of results.
            sort: (list): A sort array, example: ["time_created:desc"]

        Returns:
            generator: A generator which will return matching DataSets when iterated.

        """
        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'types': as_collection(type),
            'sort': sort
        }
        return self.app.client.iter_paged_results('/api/v1/data-sets/_search', body, limit, DataSet)

    def get_label_counts(self, dataset):
        """
        Get a dictionary of the labels and how many times they occur in the dataset.

        Args:
            dataset (dataset): The dataset or its unique Id.

        Returns:
            dict: a dictionary of label name to occurrence count.

        """
        return self.app.client.get('/api/v3/data-sets/{}/_label_counts'.format(as_id(dataset)))

    def train_model(self, dataset, model_type):
        body = {
            'modelType': model_type
        }
        return self.app.client.post(
            '/api/v3/data-sets/{}/_train_model'.format(as_id(dataset)), body)


class DataSetDownloader:
    """
    The DataSetDownloader class handles writing out the images in a
    DataSet to local disk for model training purposes.

    Multiple directory layouts are supported.

    Layout #1 is used for simple label classification with Tensorflow
    Layout #2 is used writing images and objects into darknet format.

    Examples:

        # Single Label
        base_dir/flowers/set_train/daisy
        base_dir/flowers/set_train/rose
        base_dir/flowers/set_test/daisy
        base_dir/flowers/set_test/rose


    """

    def __init__(self, app, dataset, dst_dir, train_test_ratio=4):
        """
        Create a new DataSetDownloader.

        Args:
            app: (ZmlpApp): A ZmlpApp instance.
            dataset: (DataSet): A DataSet or unique DataSet ID.
            dst_dir (str): A destination directory to write the files into.
            train_test_ratio (int): The number of images in the training
                set for every image in the test set.
        """
        self.app = app
        self.dataset_id = as_id(dataset)
        self.dst_dir = dst_dir
        self.train_test_ratio = train_test_ratio

        self.labels = {}
        self.label_distrib = {}

    def download(self, pool=None):
        """
        Downloads the files in the DataSet to local disk.

        Args:
            pool (multiprocessing.Pool): An optional Pool instance which can be used
                to download files in parallel.

        """
        self._setup()

        query = {
            'size': 32,
            '_source': ['labels', 'files'],
            'query': {
                'nested': {
                    'path': 'labels',
                    'query': {
                        'term': {'labels.dataSetId': self.dataset_id}
                    }
                }
            }
        }

        for num, asset in enumerate(self.app.assets.scroll_search(query, timeout='5m')):
            prx = asset.get_thumbnail(0)
            if not prx:
                logger.warning('{} did not have a suitable thumbnail'.format(asset))
                continue

            ds_label = self._get_dataset_label(asset)
            label = ds_label.get('label')
            if not label:
                continue

            dir_name = self._get_image_set_type(label)
            dst_path = os.path.join(self.dst_dir, dir_name, label, prx.cache_id)
            os.makedirs(os.path.dirname(dst_path), exist_ok=True)

            logger.info('Downloading to {}'.format(dst_path))
            if pool:
                pool.apply_async(self.app.assets.download_file, args=(prx, dst_path))
            else:
                self.app.assets.download_file(prx, dst_path)

    def _setup(self):
        """
        Sets up a directory structure for storing the files in the DataSet.

        The structure is basically:
            set_train/<label>/<img file>
            set_test/<label>/<img file>
        """
        self.labels = self.app.datasets.get_label_counts(self.dataset_id)

        # Prebuild entire directory structure
        os.makedirs(self.dst_dir, exist_ok=True)

        # This is layout #1, we need to add darknet layout for object detection.
        dirs = ('set_train', 'set_test')
        for set_name in dirs:
            os.makedirs('{}/{}'.format(self.dst_dir, set_name), exist_ok=True)
            for label in self.labels.keys():
                os.makedirs(os.path.join(self.dst_dir, set_name, label), exist_ok=True)

        logger.info('DataSetDownloader setup, using {} labels'.format(len(self.labels)))

    def _get_image_set_type(self, label):
        """
        Using the train_ratio property, determine if the current label
        would be in the training set or test set.

        Args:
            label (str): The label name.

        Returns:
            str: Either set_test or set_test, depending on the train_test_ratio property.

        """
        value = self.label_distrib.get(label, -1) + 1
        self.label_distrib[label] = value
        if value % self.train_test_ratio == 0:
            return 'set_test'
        else:
            return 'set_train'

    def _get_dataset_label(self, asset):
        """
        Get the current dataset label for the given asset.

        Args:
            asset (Asset): The asset to check.

        Returns:
            dict: The label dict

        """
        ds_labels = asset.get_attr('labels')
        if not ds_labels:
            return None
        for ds_label in ds_labels:
            if ds_label.get('dataSetId') == self.dataset_id:
                return ds_label
        return None
