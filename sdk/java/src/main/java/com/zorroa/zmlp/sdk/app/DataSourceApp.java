package com.zorroa.zmlp.sdk.app;

import com.zorroa.zmlp.sdk.ZmlpClient;
import com.zorroa.zmlp.sdk.domain.datasource.DataSource;
import com.zorroa.zmlp.sdk.domain.datasource.DataSourceCredentials;
import com.zorroa.zmlp.sdk.domain.datasource.DataSourceSpec;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataSourceApp {

    private static final String BASE_URI = "/api/v1/data-sources";

    public final ZmlpClient client;

    public DataSourceApp(ZmlpClient client) {
        this.client = client;
    }

    /**
     * Create a new DataSource.
     *
     * @param spec
     * @return
     */
    public DataSource createDataSource(DataSourceSpec spec) {
        return client.post(BASE_URI, spec, DataSource.class);
    }

    /**
     * Import or re-import all assets found at the given DataSource.  If the
     * DataSource has already been imported then calling this will
     * completely overwrite the existing Assets with fresh copies.
     * If the DataSource URI contains less Assets, no assets will be
     * removed from PixelML.
     *
     * @param id A DataSource id or the name of a data source.
     * @return An import DataSource result dictionary.
     */
    public DataSource importDataSource(String id) {
        String url = String.format("%s/%s/_import", BASE_URI, id);
        return client.post(url, null, DataSource.class);
    }

    /**
     * Update the DataSource credentials.  Set the blob to None to delete the credentials.
     *
     * @param dataSourceCredentials   A DataSourceCredentials contains information about Datasource and its credentials.
     * @return A status dict.
     */

    public Map updateCredentials(DataSourceCredentials dataSourceCredentials) {
        String url = String.format("%s/%s/_credentials", BASE_URI, dataSourceCredentials.getDataSourceId());
        Map body = new HashMap();
        body.put("blob", dataSourceCredentials.getBlob());
        return client.put(url, body, Map.class);
    }

    /**
     * Finds a DataSource by name or unique Id.
     *
     * @param name The unique name or unique ID.
     * @return The DataSource
     */
    public DataSource getDataSource(String name) {

        String url = String.format("%s/_findOne", BASE_URI);
        Map body = new HashMap();

        try {
            body.put("ids", Arrays.asList(UUID.fromString(name)));
        } catch (IllegalArgumentException ex) {
            body.put("names", Arrays.asList(name));
        }

        return client.post(url, body, DataSource.class);
    }
}
