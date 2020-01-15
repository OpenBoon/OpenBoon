package com.zorroa.zmlp.client.domain.datasource;

import java.util.UUID;

public class DataSourceCredentials {
    /**
     * A credentials blob of some kind. See docs for more details.
     */
    private String blob;

    /**
     * The SALT used to encrypt the credentials.
     */
    private String salt;

    private UUID dataSourceId;

    public DataSourceCredentials(UUID dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public String getBlob() {
        return blob;
    }

    public DataSourceCredentials setBlob(String blob) {
        this.blob = blob;
        return this;
    }

    public String getSalt() {
        return salt;
    }

    public DataSourceCredentials setSalt(String salt) {
        this.salt = salt;
        return this;
    }

    public UUID getDataSourceId() {
        return dataSourceId;
    }

    public DataSourceCredentials setDataSourceId(UUID dataSourceId) {
        this.dataSourceId = dataSourceId;
        return this;
    }
}


