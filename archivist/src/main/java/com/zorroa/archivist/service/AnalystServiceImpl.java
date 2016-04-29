package com.zorroa.archivist.service;

import com.zorroa.archivist.repository.AnalystDao;
import com.zorroa.sdk.client.analyst.AnalystClient;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.AnalystPing;
import com.zorroa.sdk.domain.AnalystState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.List;

/**
 * Created by chambers on 2/9/16.
 */
@Service
@Transactional
public class AnalystServiceImpl implements AnalystService {

    @Autowired
    AnalystDao analystDao;

    @Override
    public void register(AnalystPing ping) {
        if (!analystDao.update(ping)) {
            analystDao.create(ping);
        }
        else {
            /*
             * Reset the host state to up.  Eventually, we'll only want to move the state to up
             * if the node is in a certain state, which is why this isn't part of the
             * standard update.
             */
            analystDao.setState(ping.getUrl(), AnalystState.UP);
        }
    }

    @Override
    public void shutdown(AnalystPing ping) {
        /*
         * Do a final update then then set the state to shutdown.
         */
        if (analystDao.update(ping)) {
            analystDao.setState(ping.getUrl(), AnalystState.SHUTDOWN, AnalystState.UP);
        }
    }

    @Override
    public List<Analyst> getActive() {
        return analystDao.getActive();
    }

    @Override
    public List<Analyst> getAll() {
        return analystDao.getAll();
    }

    @Override
    public AnalystClient getAnalystClient() throws Exception {

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        InputStream trustStoreInput = new ClassPathResource("truststore.p12").getInputStream();
        trustStore.load(trustStoreInput, "zorroa" .toCharArray());

        AnalystClient client = new AnalystClient(trustStore);
        for (Analyst a:  analystDao.getAll(AnalystState.UP)) {
            client.getLoadBalancer().addHost(a.getUrl());
        }
        return client;
    }
}
