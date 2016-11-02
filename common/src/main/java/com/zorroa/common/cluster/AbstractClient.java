package com.zorroa.common.cluster;

import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.security.KeyStore;

/**
 * Created by chambers on 2/18/16.
 */
public class AbstractClient implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClient.class);

    protected final LoadBalancer loadBalancer = new LoadBalancer();
    protected CloseableHttpClient client;
    protected KeyStore trustStore = null;

    public AbstractClient() {
        this.client = Http.initClient();
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (IOException e) {

        }
    }
}
