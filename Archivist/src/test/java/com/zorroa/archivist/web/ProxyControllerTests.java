package com.zorroa.archivist.web;

import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.Ingest;
import com.zorroa.archivist.sdk.domain.IngestBuilder;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.service.IngestExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 1/15/16.
 */
public class ProxyControllerTests  extends MockMvcTest {

    @Autowired
    AssetController assetController;

    @Autowired
    IngestService ingestService;

    @Autowired
    AssetDao assetDao;

    @Autowired
    IngestExecutorService ingestExecutorService;

    Ingest ingest;

    @Before
    public void init() {
        ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
    }

    @Test
    public void getProxy() throws Exception {
        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();

        Asset asset = assetDao.getAll().get(0);
        logger.info("{}", asset.getDocument().get("proxies"));

        mvc.perform(get("/api/v1/proxy/image/" + asset.getSchema("proxies", ProxySchema.class).get(0).getName())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }


}
