package com.zorroa.zmlp.sdk.domain.datasource;

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

    public DataSourceCredentials withBlob(String blob){
        this.blob = blob;
        return this;
    }

    public DataSourceCredentials withSalt(String salt){
        this.salt = salt;
        return this;
    }

    public DataSourceCredentials withDataSourceId(UUID id){
        this.dataSourceId = id;
        return this;
    }

    public String getBlob() {
        return blob;
    }

    public void setBlob(String blob) {
        this.blob = blob;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public UUID getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(UUID dataSourceId) {
        this.dataSourceId = dataSourceId;
    }
}


