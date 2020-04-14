from ..dataset import DataSet


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
