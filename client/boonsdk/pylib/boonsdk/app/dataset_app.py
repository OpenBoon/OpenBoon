import logging

from ..entity import DataSet
from ..util import is_valid_uuid, as_collection, as_name_collection

logger = logging.getLogger(__name__)

__all__ = [
    'DataSetApp'
]


class DataSetApp:
    """
    Methods for manipulating DataSets.
    """

    def __init__(self, app):
        self.app = app

    def create_dataset(self, name, type):
        """
        Create a new DataSet.

        Args:
            name (str): A unique name.
            type (DataSetType): The type of dataset.

        Returns:
            DataSet: The newly created DataSet.
        """
        body = {
            'name': name,
            'type': getattr(type, 'name', str(type))
        }
        return DataSet(self.app.client.post('/api/v3/datasets', body))

    def get_dataset(self, id):
        """
        Get a DataSet by it's unique ID or name.

        Args:
            id (str): The unique ID or unique name.

        Returns:
            DataSet: The DataSet
        """
        ds = DataSet.as_id(id)
        if is_valid_uuid(ds):
            return DataSet(self.app.client.get(f'/api/v3/datasets/{ds}'))
        else:
            return self.find_one_dataset(name=ds)

    def find_one_dataset(self, id=None, name=None, type=None):
        """
        Find a single DataSet based on various properties.

        Args:
            id:
            name:
            type (DataSetType): The type of dataset.

        Returns:
            DataSet: A single DataSet
        """
        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'types': as_name_collection(type)
        }
        return DataSet(self.app.client.post("/api/v3/datasets/_find_one", body))

    def find_datasets(self, id=None, name=None, type=None, limit=None, sort=None):
        """
        Find a single DataSet based on various properties.

        Args:
            id (str): The ID or list of Ids.
            name (str): The DataSet name or list of names.
            type (str): The DataSet type or list of types.
            limit (int): Limit results to the given size.
            sort (list): An array of properties to sort by. Example: ["name:asc"]

        Returns:
            generator: A generator which will return matching DataSets when iterated.

        """
        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'types': as_name_collection(type),
            'sort': sort
        }
        return self.app.client.iter_paged_results('/api/v3/datasets/_search', body, limit, DataSet)

    def delete_dataset(self, dataset):
        """
        Delete a DataSet.  Once a DataSet is deleted all Assets for the DataSet
        are unlabeled.

        Args:
            dataset (DataSet): A DataSet or it's unique ID.

        Returns:
            dict: A status dict
        """
        ds = DataSet.as_id(dataset)
        return self.app.client.delete(f'/api/v3/datasets/{ds}')

    def get_label_counts(self, dataset):
        """
        Get a dictionary of the labels and how many times they occur.

        Args:
            dataset (DataSet): The DataSet, it's unique Id, or a model with a DataSet.

        Returns:
            dict: a dictionary of label name to occurrence count.
        """
        ds = DataSet.as_id(dataset)
        return self.app.client.get(f'/api/v3/datasets/{ds}/_label_counts')

    def rename_label(self, dataset, old_label, new_label):
        """
        Rename a the given label to a new label name.  The new label can already exist.

        Args:
            dataset (DataSet): The DataSet or its unique Id.
            old_label (str): The old label name.
            new_label (str): The new label name.

        Returns:
            dict: a dictionary containing the number of assets updated.

        """
        ds = DataSet.as_id(dataset)
        body = {
            "label": old_label,
            "newLabel": new_label
        }
        return self.app.client.put(f'/api/v3/datasets/{ds}/labels', body)

    def delete_label(self, dataset, label):
        """
        Removes the label from all Assets.

        Args:
            dataset (DataSet): The DataSet or its unique Id.
            label (str): The label name to remove.

        Returns:
            dict: a dictionary containing the number of assets updated.

        """
        ds = DataSet.as_id(dataset)
        body = {
            "label": label
        }
        return self.app.client.delete(f'/api/v3/datasets/{ds}/labels', body)
