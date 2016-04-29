package com.zorroa.analyst.service;

import com.zorroa.sdk.filesystem.ObjectFile;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Utilized for downloading remote assets or models.
 */
@Component
public class TransferServiceImpl implements TransferService {
    private static final Logger logger = LoggerFactory.getLogger(TransferServiceImpl.class);

    @Autowired
    ObjectFileSystem objectFileSystem;

    private final CloseableHttpClient httpClient;

    public TransferServiceImpl() {
        httpClient = HttpClients.createDefault();
    }

    @Override
    public void transfer(URI uri, ObjectFile dst) throws IOException {
        logger.debug("transfering {} to {}", uri, dst);
        HttpGet httpget = new HttpGet(uri);
        HttpResponse response = httpClient.execute(httpget);

        HttpEntity entity = response.getEntity();
        try (InputStream is = entity.getContent()) {
            dst.store(is);
        }
    }

    @Override
    public void transfer(String src, ObjectFile dst) throws IOException {
        transfer(URI.create(src), dst);
    }
}
