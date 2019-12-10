package domain;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataSourceApp {


    final String url = "/api/v1/data-sources";
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

        /*
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
            'analysis': analysis
        }
        return DataSource(self.app.client.post(url, body=body))
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

        //Implement Rest Request PixmlClient
        //Implement Analysis

        return new DataSource(app.pixmlClient.post(this.url, body));


    }

}
