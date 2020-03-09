package com.zorroa.zmlp.client.app;

import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.domain.datasource.DataSource;
import com.zorroa.zmlp.client.domain.datasource.DataSourceCredentials;
import com.zorroa.zmlp.client.domain.datasource.DataSourceSpec;

import java.util.*;

public class DataSourceApp {

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
        return client.post("/api/v1/data-sources", spec, DataSource.class);
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
        String url = String.format("/api/v1/data-sources/%s/_import", id);
        return client.post(url, new HashMap(), DataSource.class);
    }

    /**
     * Finds a DataSource by name or unique Id.
     *
     * @param name The unique name or unique ID.
     * @return The DataSource
     */
    public DataSource getDataSource(String name) {

        String url = String.format("/api/v1/jobs/_findOne");
        Map body = new HashMap();

        try {
            body.put("ids", Arrays.asList(UUID.fromString(name)));
        } catch (IllegalArgumentException ex) {
            body.put("names", Arrays.asList(name));
        }

        return client.post(url, body, DataSource.class);
    }
}
