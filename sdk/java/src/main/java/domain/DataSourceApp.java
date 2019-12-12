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

    /**
     * Create a new DataSource.
     *
     * @param name        The name of the data source.
     * @param uri         The URI where the data can be found.
     * @param credentials A file path to an associated credentials file.
     * @param fileTypes   A list of file paths or mimetypes to match.
     * @param analysis    A list of Analysis Modules to apply to the data.
     * @return The created DataSource
     * @throws IOException
     * @throws InterruptedException
     */

    public DataSource createDataSource(String name, String uri, String credentials, List<String> fileTypes, List analysis) throws IOException, InterruptedException {

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

    /**
     * Finds a DataSource by name or unique Id.
     *
     * @param name The unique name or unique ID.
     * @return The DataSource
     * @throws IOException
     * @throws InterruptedException
     */

    public DataSource getDataSource(String name) throws IOException, InterruptedException {

        String url = "/api/v1/data-sources/_findOne";

        Map body = new HashMap();
        if (Utils.isValidUUI(name)) {
            body.put("ids", Arrays.asList(name));
        } else {
            body.put("names", Arrays.asList(name));
        }


        return new DataSource(this.app.pixmlClient.post(url, body));

    }

    /**
     * Import or re-import all assets found at the given DataSource.  If the
     * DataSource has already been imported then calling this will
     * completely overwrite the existing Assets with fresh copies.
     * If the DataSource URI contains less Assets, no assets will be
     * removed from PixelML.
     *
     * @param ds A DataSource object or the name of a data source.
     * @return An import DataSource result dictionary.
     * @throws IOException          If the DataSource does not exist.
     * @throws InterruptedException
     */
    public Map importDataSource(DataSource ds) throws IOException, InterruptedException {

        String url = String.format("/api/v1/data-sources/%s/_import", ds.getId());

        return this.app.pixmlClient.post(url, null);

    }

    /**
     * Update the DataSource credentials.  Set the blob to None to delete the credentials.
     *
     * @param ds   A DataSource object or the name of a data source.
     * @param blob A credentials blob.
     * @return A status dict.
     * @throws IOException          If the DataSource does not exist.
     * @throws InterruptedException
     */

    public Map updateCredentials(DataSource ds, String blob) throws IOException, InterruptedException {

        String url = String.format("/api/v1/data-sources/%s/_credentials", ds.getId());

        Map body = new HashMap();
        body.put("blob", blob);

        return this.app.pixmlClient.put(url, body);

    }
}
