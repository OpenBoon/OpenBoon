package com.zorroa.zmlp.sdk.app;

import com.zorroa.zmlp.sdk.ZmlpClient;
import com.zorroa.zmlp.sdk.domain.DataSource;
import com.zorroa.zmlp.sdk.Utils;
import com.zorroa.zmlp.sdk.ZmlpApp;
import com.zorroa.zmlp.sdk.domain.DataSourceSpec;

import java.io.IOException;
import java.util.*;

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
     * @param ds A DataSource object or the name of a data source.
     * @return An import DataSource result dictionary.
     */
    public Map importDataSource(DataSource ds) {
        String url = String.format("%s/%s/_import", BASE_URI, ds.getId());
        return client.post(url, null, Map.class);
    }

    /**
     * Update the DataSource credentials.  Set the blob to None to delete the credentials.
     *
     * @param ds   A DataSource object or the name of a data source.
     * @param blob A credentials blob.
     * @return A status dict.
     */

    public Map updateCredentials(DataSource ds, String blob)  {
        String url = String.format("%s/%s/_credentials", BASE_URI, ds.getId());
        Map body = new HashMap();
        body.put("blob", blob);
        return client.put(url, body, Map.class);
    }
}
