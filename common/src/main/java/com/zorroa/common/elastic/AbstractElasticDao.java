package com.zorroa.common.elastic;

import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractElasticDao {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractElasticDao.class);

    protected Client client;
    protected ElasticTemplate elastic;
    protected String alias = "archivist";

    @Autowired
    public void setClient(Client client) {
        this.client = client;
        this.elastic = new ElasticTemplate(client, alias, getType());
    }

    public abstract String getType();

    public void refreshIndex() {
        client.admin().indices().prepareRefresh(alias).get();
    }
}
