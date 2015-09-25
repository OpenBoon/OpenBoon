package com.zorroa.archivist;

import com.google.common.collect.Sets;
import com.zorroa.archivist.domain.StandardRoles;
import com.zorroa.archivist.domain.UserBuilder;
import com.zorroa.archivist.service.UserService;
import org.elasticsearch.action.admin.cluster.snapshots.delete.DeleteSnapshotRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Set;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ArchivistApplication.class)
@WebAppConfiguration
@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=true)
@Transactional
public abstract class ArchivistApplicationTests {

    public static final Logger logger = LoggerFactory.getLogger(ArchivistConfiguration.class);

    @Autowired
    protected Client client;

    @Autowired
    protected UserService userService;

    @Value("${archivist.index.alias}")
    protected String alias;

    @Value("${archivist.snapshot.repoName}")
    private String snapshotRepoName;

    protected Set<String> testImages;

    public static final String TEST_IMAGE_PATH = "src/test/resources/static/images";

    public ArchivistApplicationTests() {
        logger.info("Setting unit test");
        ArchivistConfiguration.unittest = true;
    }

    private ImmutableList<SnapshotInfo> getSnapshotInfos() {
        GetSnapshotsRequestBuilder builder =
                new GetSnapshotsRequestBuilder(client.admin().cluster());
        builder.setRepository(snapshotRepoName);
        GetSnapshotsResponse getSnapshotsResponse = builder.execute().actionGet();
        return getSnapshotsResponse.getSnapshots();
    }

    @Before
    public void setup() {
        // delete all the assets
        client.prepareDeleteByQuery(alias)
            .setTypes("asset")
            .setQuery(QueryBuilders.matchAllQuery())
            .get();

        // Delete all snapshots
        for (SnapshotInfo info : getSnapshotInfos()) {
            DeleteSnapshotRequestBuilder builder = new DeleteSnapshotRequestBuilder(client.admin().cluster());
            builder.setRepository(snapshotRepoName).setSnapshot(info.name());
            builder.execute().actionGet();
        }

        // Delete any previously restored index
        try {
            DeleteIndexRequestBuilder deleteBuilder = new DeleteIndexRequestBuilder(client.admin().indices(), "restored_archivist_01");
            deleteBuilder.execute().actionGet();
        } catch (IndexMissingException e) {
            logger.info("No existing snapshot to delete");
        }


        /**
         * Adds in a test, non privileged user.
         */
        UserBuilder userBuilder = new UserBuilder();
        userBuilder.setEmail("user@zorroa.com");
        userBuilder.setFirstName("Bob");
        userBuilder.setLastName("User");
        userBuilder.setUsername("user");
        userBuilder.setPassword("user");
        userBuilder.setRoles(Sets.newHashSet(StandardRoles.USER));
        userService.create(userBuilder);
    }

    public String getStaticImagePath(String subdir) {
        FileSystemResource resource = new FileSystemResource(TEST_IMAGE_PATH);
        String path = resource.getFile().getAbsolutePath() + "/" + subdir;
        logger.info("test image path: {}", path);
        return path;
    }

    public String getStaticImagePath() {
        return getStaticImagePath("standard");
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
