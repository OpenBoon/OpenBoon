import os

from ..datasource import DataSource
from ..util import is_valid_uuid, as_collection


class DataSourceApp(object):

    def __init__(self, app):
        self.app = app

    def create_datasource(self, name, uri, modules=None, credentials=None, file_types=None):
        """
        Create a new DataSource.

        Args:
            name (str): The name of the data source.
            uri (str): The URI where the data can be found.
            modules (list): A list of PipelineMods names to apply to the data
            file_types (list of str): a list of file paths or mimetypes to match.
            credentials (str): A file path to an associated credentials file.
        Returns:
            DataSource: The created DataSource

        """
        if credentials:
            if not os.path.exists(credentials):
                raise ValueError('The credentials path {} does not exist')
            else:
                with open(credentials, 'r') as fp:
                    credentials = fp.read()
        url = '/api/v1/data-sources'
        body = {
            'name': name,
            'uri': uri,
            'credentials': credentials,
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

    def import_files(self, ds):
        """
        Import or re-import all assets found at the given DataSource.  If the
        DataSource has already been imported then calling this will
        completely overwrite the existing Assets with fresh copies.

        If the DataSource URI contains less Assets, no assets will be
        removed from ZMLP.

        Args:
            ds (DataSource): A DataSource object or the name of a data source.

        Returns:
            dict: An import DataSource result dictionary.

        """
        url = '/api/v1/data-sources/{}/_import'.format(ds.id)
        return self.app.client.post(url)

    def update_credentials(self, ds, blob):
        """
        Update the DataSource credentials.  Set the blob to None
        to delete the credentials.

        Args:
            ds (DataSource):
            blob (str): A credentials blob.

        Returns:
            dict: A status dict.

        Raises:
            ZmlpNotFoundException: If the DataSource does not exist.

        """
        url = '/api/v1/data-sources/{}/_credentials'.format(ds.id)
        body = {
            'blob': blob
        }
        return self.app.client.put(url, body=body)
