from ..entity import DataSet
from ..util import as_collection, as_id


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
        req = {
            "name": name,
            "type": type
        }
        return DataSet(self.app.client.post("/api/v1/data-sets", req))

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
            "names": as_collection(name),
            "ids": as_collection(id),
            "types": as_collection(type),
        }
        return DataSet(self.app.client.post('/api/v3/data-sets/_find_one', body))

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
            "names": as_collection(name),
            "ids": as_collection(id),
            "types": as_collection(type),
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
