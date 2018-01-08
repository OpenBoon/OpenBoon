package com.zorroa.common.elastic;

import com.zorroa.common.config.ApplicationProperties;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractElasticDao {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Client client;
    protected ElasticTemplate elastic;

    @Autowired
    public void setApplicationProperties(ApplicationProperties props) {

    }

    @Autowired
    public void setClient(Client client) {
        this.client = client;
        this.elastic = new ElasticTemplate(client, getIndex(), getType());
    }

    public abstract String getType();
    public abstract String getIndex();

    public void refreshIndex() {
        client.admin().indices().prepareRefresh(getIndex()).get();
    }
}
