from ..entity import DataSource, Job
from ..util import is_valid_uuid, as_collection


class DataSourceApp(object):

    def __init__(self, app):
        self.app = app

    def create_datasource(self, name, uri, modules=None, file_types=None, credentials=None):
        """
        Create a new DataSource.

        Args:
            name (str): The name of the data source.
            uri (str): The URI where the data can be found.
            modules (list): A list of AnalysisModules names to apply to the data.
            file_types (list of str): a list of file extensions or general types like
                'images', 'videos', 'documents'. Defaults to all file types.
            credentials (list of str): A list of pre-created credentials blob names.
        Returns:
            DataSource: The created DataSource

        """
        url = '/api/v1/data-sources'
        body = {
            'name': name,
            'uri': uri,
            'credentials': as_collection(credentials),
            'fileTypes': file_types,
            'modules': as_collection(modules)
        }
        return DataSource(self.app.client.post(url, body=body))

    def get_datasource(self, name):
        """
        Finds a DataSource by name or unique Id.

        Args:
            name (str): The unique name or unique ID.

        Returns:
            DataSource: The DataSource

        """
        url = '/api/v1/data-sources/_findOne'
        if is_valid_uuid(name):
            body = {"ids": [name]}
        else:
            body = {"names": [name]}

        return DataSource(self.app.client.post(url, body=body))

    def import_files(self, ds, batch_size=25):
        """
        Import all assets found at the given DataSource.  If the
        DataSource has already been imported then only new files will be
        imported. New modules assigned to the datasource will
        also be applied to existing assets as well as new assets.

        Args:
            ds (DataSource): A DataSource object or the name of a data source.
            batch_size (int): The number of Assets per batch.  Must be at least 20.
        Returns:
            Job: Return the Job responsible for processing the files.

        """
        body = {
            "batchSize": batch_size
        }
        url = '/api/v1/data-sources/{}/_import'.format(ds.id)
        return Job(self.app.client.post(url, body))

    def delete_datasource(self, ds, remove_assets=False):
        """
        Delete the given datasource.  If remove_assets is true, then all
        assets that were imported with a datasource are removed as well.  This
        cannot be undone.

        Args:
            ds (DataSource): A DataSource object or the name of a data source.
            remove_assets (bool): Set to true if Assets should be deleted as well.
        Returns:
            dict: Status object
        """
        body = {
            'deleteAssets': remove_assets
        }
        url = '/api/v1/data-sources/{}'.format(ds.id)
        return self.app.client.delete(url, body)
