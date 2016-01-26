package com.zorroa.archivist;

import com.zorroa.archivist.sdk.domain.User;
import com.zorroa.archivist.sdk.domain.UserBuilder;
import com.zorroa.archivist.sdk.service.UserService;
import com.zorroa.archivist.security.UnitTestAuthentication;
import com.zorroa.archivist.tx.TransactionEventManager;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.util.Set;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ArchivistApplication.class)
@TestPropertySource("/test.properties")
@WebAppConfiguration
@Transactional
public abstract class ArchivistApplicationTests {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected Client client;

    @Autowired
    protected UserService userService;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Autowired
    DataSourceTransactionManager transactionManager;

    @Autowired
    ArchivistRepositorySetup archivistRepositorySetup;

    @Value("${archivist.index.alias}")
    protected String alias;

    @Value("${archivist.snapshot.repoName}")
    private String snapshotRepoName;

    protected JdbcTemplate jdbc;

    protected Set<String> testImages;

    public static final String TEST_DATA_PATH = "src/test/resources/static";

    public ArchivistApplicationTests() {
        ArchivistConfiguration.unittest = true;
    }

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    private ImmutableList<SnapshotInfo> getSnapshotInfos() {
        GetSnapshotsRequestBuilder builder =
                new GetSnapshotsRequestBuilder(client.admin().cluster());
        builder.setRepository(snapshotRepoName);
        GetSnapshotsResponse getSnapshotsResponse = builder.execute().actionGet();
        return getSnapshotsResponse.getSnapshots();
    }

    @Before
    public void setup() throws IOException {

        /*
          * Now that folders are created using what is essentially a nested transaction,
          * we can't rely on the unittests to roll them back.  For this, we manually delete
          * every folder not created by the SQL schema migration.
          *
          * Eventually we need a different way to do this because it relies on the created
          * time, which just happens to be the same for all schema created folders.
          *
          * //TODO: find a more robust way to handle deleting folders created by a test.
          * Maybe use naming conventions (test_) or utilize a new field on the table.
          *
         */
        TransactionTemplate tmpl = new TransactionTemplate(transactionManager);
        tmpl.setPropagationBehavior(Propagation.NOT_SUPPORTED.ordinal());
                tmpl.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                jdbc.update("DELETE FROM folder WHERE time_created !=1450709321000");
            }
        });

        /*
         * Ensures that all transaction events run within the unit test transaction.
         * If this was not set then  transaction events like AfterCommit would never execute
         * because unit test transactions are never committed.
         */
        transactionEventManager.setImmediateMode(true);

        /**
         * Delete and recreate all indexes
         */
        client.admin().indices().prepareDelete("_all").get();
        archivistRepositorySetup.setupDataSources();
        refreshIndex(100);

        /**
         * TODO: fix this for elastic 1.7
         */
        /*
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

        */

        /**
         * Before we can do anything reliably we need a logged in user.
         */
        authenticate();

        /**
         * Adds in a test, non privileged user.
         */
        UserBuilder userBuilder = new UserBuilder();
        userBuilder.setEmail("user@zorroa.com");
        userBuilder.setFirstName("Bob");
        userBuilder.setLastName("User");
        userBuilder.setUsername("user");
        userBuilder.setPassword("user");
        userService.create(userBuilder);
    }

    /**
     * Athenticates a user as admin but with all permissions, including internal ones.
     */
    public void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                authenticationManager.authenticate(new UnitTestAuthentication(userService.get("admin"),
                        userService.getPermissions())));
    }

    public void authenticate(String username) {
        User user = userService.get(username);
        SecurityContextHolder.getContext().setAuthentication(
                authenticationManager.authenticate(new UnitTestAuthentication(user,
                        userService.getPermissions(user))));
    }

    public void logout() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    public String getStaticImagePath(String subdir) {
        FileSystemResource resource = new FileSystemResource(TEST_DATA_PATH + "/images");
        String path = resource.getFile().getAbsolutePath() + "/" + subdir;
        return path;
    }

    public String getStaticImagePath() {
        return getStaticImagePath("standard");
    }

    public File getTestImage(String name) {
        return new File(getStaticImagePath() + "/" + name);
    }

    public void refreshIndex() {
        refreshIndex(10);
    }

    public void refreshIndex(long sleep) {
        refreshIndex(alias, sleep);
    }

    public void refreshIndex(String alias, long sleep) {
        try {
            Thread.sleep(sleep/2);
        } catch (InterruptedException e) {
        }
        client.admin().indices().prepareRefresh(alias).get();
        try {
            Thread.sleep(sleep/2);
        } catch (InterruptedException e) {
        }
    }

}
