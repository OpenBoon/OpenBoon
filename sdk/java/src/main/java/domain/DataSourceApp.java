package domain;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataSourceApp {

    PixmlApp app;

    public DataSourceApp(PixmlApp app) {
        this.app = app;
    }

    public DataSource createDataSource(String name, String uri, String credentials, List<String> fileTypes, List analysis) throws IOException, InterruptedException {
        /*
        Create a new DataSource.

        Args:
            name (str): The name of the data source.
            uri (str): The URI where the data can be found.
            credentials (str): A file path to an associated credentials file.
            file_types (list of str): a list of file paths or mimetypes to match.
            analysis (list): A list of Analysis Modules to apply to the data.

        Returns:
            DataSource: The created DataSource

         */

        if (credentials != null) {
            credentials = Utils.readTextFromFile(credentials);
        }

        Map body = new HashMap();

        body.put("name", name);
        body.put("uri", uri);
        body.put("credentials", credentials);
        body.put("fileTypes", fileTypes);
        body.put("analysis", analysis);

        // TODO - Implement Analysis
        final String url = "/api/v1/data-sources";
        return new DataSource(app.pixmlClient.post(url, body));

    }

    public DataSource getDataSource(String name) throws IOException, InterruptedException {
        /*
        Finds a DataSource by name or unique Id.

        Args:
            name (str): The unique name or unique ID.

        Returns:
            DataSource: The DataSource

        */

        String url = "/api/v1/data-sources/_findOne";

        Map body = new HashMap();
        if (Utils.isValidUUI(name)) {
            body.put("ids", Arrays.asList(name));
        } else {
            body.put("names", Arrays.asList(name));
        }


        return new DataSource(this.app.pixmlClient.post(url, body));

    }

    public Map importDataSource(DataSource ds) throws IOException, InterruptedException {
        /*
        Import or re-import all assets found at the given DataSource.  If the
        DataSource has already been imported then calling this will
        completely overwrite the existing Assets with fresh copies.

        If the DataSource URI contains less Assets, no assets will be
        removed from PixelML.

        Args:
            ds (DataSource): A DataSource object or the name of a data source.

        Returns:
            dict: An import DataSource result dictionary.

        url = '/api/v1/data-sources/{}/_import'.format(ds.id)
        return self.app.client.post(url)
         */

        String url = String.format("/api/v1/data-sources/%s/_import", ds.getId());

        return this.app.pixmlClient.post(url, null);

    }

    public Map updateCredentials(DataSource ds, String blob) throws IOException, InterruptedException {

        /*
         """
        Update the DataSource credentials.  Set the blob to None
        to delete the credentials.

        Args:
            ds (DataSource):
            blob (str): A credentials blob.

        Returns:
            dict: A status dict.

        Raises:
            PixmlNotFoundException: If the DataSource does not exist.

        """
        url = '/api/v1/data-sources/{}/_credentials'.format(ds.id)
        body = {
            'blob': blob
        }
        return self.app.client.put(url, body=body)
         */

        String url = String.format("/api/v1/data-sources/{}/_credentials").format(ds.getId());

        Map body = new HashMap();
        body.put("blob", blob);

        return this.app.pixmlClient.put(url, body);

    }
}
