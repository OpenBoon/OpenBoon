package com.zorroa.archivist;

import java.io.File;
import java.util.Set;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ArchivistApplication.class)
@WebAppConfiguration
public abstract class ArchivistApplicationTests {

    public static final Logger logger = LoggerFactory.getLogger(ArchivistConfiguration.class);

    @Autowired
    protected Client client;

    @Value("${archivist.index.alias}")
    protected String alias;

    protected Set<String> testImages;

    private static final String TEST_IMAGE_PATH = "src/test/resources/static/images";

    public ArchivistApplicationTests() {
        logger.info("Setting unit test");
        ArchivistConfiguration.unittest = true;
    }

    @Before
    public void setup() {
        // delete all the assets
        client.prepareDeleteByQuery(alias)
            .setTypes("asset")
            .setQuery(QueryBuilders.matchAllQuery())
            .get();
    }

    public String getStaticImagePath() {
        FileSystemResource resource = new FileSystemResource(TEST_IMAGE_PATH);
        String path = resource.getFile().getAbsolutePath();
        logger.info("test image path: {}", path);
        return path;
    }

    public File getTestImage(String name) {
        return new File(getStaticImagePath() + "/" + name);
    }

    public void refreshIndex() {
        client.admin().indices().prepareRefresh(alias).get();
    }

    public void refreshIndex(long sleep) {
        try {
            Thread.sleep(sleep/2);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        client.admin().indices().prepareRefresh(alias).get();
        try {
            Thread.sleep(sleep/2);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
