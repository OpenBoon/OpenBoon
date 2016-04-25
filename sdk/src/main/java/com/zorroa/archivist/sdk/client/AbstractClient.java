package com.zorroa.archivist.sdk.client;

import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
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
        detectTrustStore();
        this.client = Http.initClient(trustStore);
    }

    public AbstractClient(KeyStore trustStore) {
        this.trustStore = trustStore;
        this.client = Http.initClient(trustStore);
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    private void detectTrustStore() {
        try {
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            InputStream trustStoreInput = getClass().getResourceAsStream(getClass().getSimpleName() + ".trust");
            trustStore.load(trustStoreInput, "zorroa" .toCharArray());
            this.trustStore = trustStore;
        } catch (Exception e) {
            logger.warn("Failed to detect trusted analyst store");
        }
    }
    public void setTrustStore(KeyStore trustStore) {
        this.trustStore = trustStore;
        if (this.client != null) {
            close();
            this.client = Http.initClient(trustStore);
        }
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (IOException e) {

        }
    }
}
