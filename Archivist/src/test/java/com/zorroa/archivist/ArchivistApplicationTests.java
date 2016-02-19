package com.zorroa.archivist;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zorroa.archivist.aggregators.Aggregator;
import com.zorroa.archivist.domain.MigrationType;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.schema.IngestSchema;
import com.zorroa.archivist.sdk.util.FileUtils;
import com.zorroa.archivist.security.BackgroundTaskAuthentication;
import com.zorroa.archivist.security.UnitTestAuthentication;
import com.zorroa.archivist.service.*;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.service.EventLogService;
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
import org.springframework.security.core.Authentication;
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
import java.util.List;
import java.util.Set;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
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
    protected MigrationService migrationService;

    @Autowired
    protected FolderService folderService;

    @Autowired
    protected IngestExecutorService ingestExecutorService;

    @Autowired
    protected IngestScheduleService ingestScheduleService;

    @Autowired
    protected ExportExecutorService exportExecutorService;

    @Autowired
    protected ExportService exportService;

    @Autowired
    protected IngestService ingestService;

    @Autowired
    protected SearchService searchService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected RoomService roomService;

    @Autowired
    protected MessagingService messagingService;

    @Autowired
    protected EventLogService eventLogSerivce;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Autowired
    DataSourceTransactionManager transactionManager;

    @Autowired
    ArchivistRepositorySetup archivistRepositorySetup;

    @Value("${zorroa.common.index.alias}")
    protected String alias;

    @Value("${archivist.snapshot.repoName}")
    private String snapshotRepoName;

    protected JdbcTemplate jdbc;

    protected Set<String> testImages;

    public static final String TEST_DATA_PATH = new File("src/test/resources/static").getAbsolutePath();

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
        /**
         * Before we can do anything reliably we need a logged in user.
         */
        authenticate();

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

        /*
         * The Elastic index(s) has been created, but we have to delete it and recreate it
         * so each test has a clean index.  Once this is done we can call setupDataSources()
         * which adds some standard data to both databases.
         */
        client.admin().indices().prepareDelete("_all").get();
        migrationService.processMigrations(migrationService.getAll(MigrationType.ElasticSearchIndex), true);
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

    private static final Set<String> SUPPORTED_FORMATS = ImmutableSet.of(
            "jpg", "pdf", "mov", "gif", "tif");

    public List<AssetBuilder> getTestAssets(String subdir) {
        List<AssetBuilder> result = Lists.newArrayList();
        FileSystemResource resource = new FileSystemResource(TEST_DATA_PATH + "/images/" + subdir);
        for (File f: resource.getFile().listFiles()) {

            if (f.isFile()) {
                if (SUPPORTED_FORMATS.contains(FileUtils.extension(f.getPath()).toLowerCase())) {
                    AssetBuilder b = new AssetBuilder(f);
                    b.setAttr("user", "rating", 4);
                    b.setAttr("test", "path", resource.getFile().getAbsolutePath());
                    result.add(b);
                }
            }
        }

        for (File f: resource.getFile().listFiles()) {
            if (f.isDirectory()) {
                result.addAll(getTestAssets(subdir + "/" + f.getName()));
            }
        }

        return result;
    }

    public Ingest addTestAssets(String subdir) {
        return addTestAssets(getTestAssets(subdir));
    }

    public Ingest addTestAssets(List<AssetBuilder> builders) {

        logger.info("testPath: {}", (String) builders.get(0).getAttr("test.path"));
        Ingest i = ingestService.createIngest(
                new IngestBuilder().setName("foo").addToPaths(builders.get(0).getAttr("test.path")));

        IngestSchema schema = new IngestSchema();
        schema.addIngest(i);
        for (AssetBuilder builder: builders) {
            logger.info("Adding test asset: {}", builder.getAbsolutePath());
            builder.addSchema(schema);
            assetService.upsert(builder);
        }
        refreshIndex();

        for (Aggregator a: ingestExecutorService.getAggregators(i)) {
            a.aggregate();
        }

        return i;
    }

    /**
     * Athenticates a user as admin but with all permissions, including internal ones.
     */
    public void authenticate() {
        Authentication auth = new BackgroundTaskAuthentication(userService.get("admin"));
        SecurityContextHolder.getContext().setAuthentication(
                authenticationManager.authenticate(auth));
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

    /*
     * TODO: We can refactor these to go away eventually.  See addTestAssets()
     */
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
