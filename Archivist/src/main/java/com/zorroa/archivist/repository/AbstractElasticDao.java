package com.zorroa.archivist.repository;

import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class AbstractElasticDao {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractElasticDao.class);

    protected Client client;

    protected ElasticTemplate elastic;

    @Value("${archivist.index.alias}")
    protected String alias;

    @Autowired
    public void setClient(Client client) {
        this.client = client;
        this.elastic = new ElasticTemplate(client, alias, getType());
    }

    public abstract String getType();
}
