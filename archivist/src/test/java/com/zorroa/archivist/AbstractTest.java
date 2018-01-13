package com.zorroa.archivist;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zorroa.archivist.config.ArchivistConfiguration;
import com.zorroa.archivist.domain.MigrationType;
import com.zorroa.archivist.domain.Permission;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserSpec;
import com.zorroa.archivist.repository.AnalystDao;
import com.zorroa.archivist.security.UnitTestAuthentication;
import com.zorroa.archivist.service.*;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.domain.AnalystSpec;
import com.zorroa.common.domain.AnalystState;
import com.zorroa.common.elastic.ElasticClientUtils;
import com.zorroa.sdk.domain.Proxy;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.schema.ProxySchema;
import com.zorroa.sdk.util.AssetUtils;
import com.zorroa.sdk.util.FileUtils;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource("/test.properties")
@WebAppConfiguration
@Transactional
public abstract class AbstractTest {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected Client client;

    @Autowired
    protected UserService userService;

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    protected MigrationService migrationService;

    @Autowired
    protected FolderService folderService;

    @Autowired
    protected ExportService exportService;

    @Autowired
    protected PipelineService pipelineService;

    @Autowired
    protected SearchService searchService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected AnalystService analystService;

    @Autowired
    protected ApplicationProperties properties;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Autowired
    DataSourceTransactionManager transactionManager;

    @Autowired
    ArchivistRepositorySetup archivistRepositorySetup;

    @Autowired
    AnalystDao analystDao;

    @Value("${zorroa.cluster.index.alias}")
    protected String alias;

    protected JdbcTemplate jdbc;

    protected Path resources;

    public AbstractTest() {
        ArchivistConfiguration.unittest = true;
    }

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
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
         * Before we can do anything reliably we need a logged in user.
         */
        authenticate();

        /**
         * Adds in a test, non privileged user.
         */
        UserSpec userBuilder = new UserSpec();
        userBuilder.setEmail("user@zorroa.com");
        userBuilder.setFirstName("Bob");
        userBuilder.setLastName("User");
        userBuilder.setUsername("user");
        userBuilder.setPassword("user");
        userService.create(userBuilder);

        UserSpec managerBuilder = new UserSpec();
        managerBuilder.setEmail("manager@zorroa.com");
        managerBuilder.setFirstName("Bob");
        managerBuilder.setLastName("Manager");
        managerBuilder.setUsername("manager");
        managerBuilder.setPassword("manager");
        User manager = userService.create(managerBuilder);
        userService.addPermissions(manager, Lists.newArrayList(
                permissionService.getPermission("group::manager")));


        resources = FileUtils.normalize(Paths.get("../../zorroa-test-data"));
    }

    public void cleanElastic() {
        /*
         * The Elastic index(s) has been created, but we have to delete it and recreate it
         * so each test has a clean index.  Once this is done we can call setupDataSources()
         * which adds some standard data to both databases.
         */
        client.admin().indices().prepareDelete("_all").get();
        migrationService.processMigrations(migrationService.getAll(MigrationType.ElasticSearchIndex), true);
        try {
            archivistRepositorySetup.setupDataSources();
            refreshIndex();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Authenticates a user as admin but with all permissions, including internal ones.
     */
    public void authenticate() {
        Authentication auth = new UsernamePasswordAuthenticationToken("admin", "admin");
        SecurityContextHolder.getContext().setAuthentication(
                authenticationManager.authenticate(auth));
    }

    public void authenticate(String username) {
        authenticate(username, false);
    }

    public void authenticate(String username, boolean superUser) {
        User user = userService.get(username);
        List<Permission> perms = userService.getPermissions(user);
        if (superUser) {
            perms.add(permissionService.getPermission("group::administrator"));
        }

        SecurityContextHolder.getContext().setAuthentication(
                authenticationManager.authenticate(new UnitTestAuthentication(user,
                        perms)));
    }

    public void logout() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    public Path getTestPath(String subdir) {
        return resources.resolve(subdir);
    }

    public Path getTestImagePath(String subdir) {
        if (subdir.startsWith("/")) {
            return resources.resolve(subdir.substring(1));
        }
        else {
            return resources.resolve("images/" + subdir);
        }
    }

    public Path getTestImagePath() {
        return getTestImagePath("set04/standard");
    }

    private static final Set<String> SUPPORTED_FORMATS = ImmutableSet.of(
        "jpg", "pdf", "m4v", "gif", "tif");

    public List<Source> getTestAssets(String subdir) {
        List<Source> result = Lists.newArrayList();
        Path tip = getTestImagePath(subdir);
        logger.info("{}", tip);
        for (File f: tip.toFile().listFiles()) {

            if (f.isFile()) {
                if (SUPPORTED_FORMATS.contains(FileUtils.extension(f.getPath()).toLowerCase())) {
                    Source b = new Source(f);
                    b.setAttr("test.path", getTestImagePath(subdir).toAbsolutePath().toString());
                    AssetUtils.addKeywords(b, "source", b.getAttr("source.filename", String.class));

                    String id = UUID.randomUUID().toString();

                    List<Proxy> proxies = Lists.newArrayList();
                    proxies.add(new Proxy().setHeight(100).setWidth(100).setId("proxy/" + id + "_foo.jpg"));
                    proxies.add(new Proxy().setHeight(200).setWidth(200).setId("proxy/" + id + "_bar.jpg"));
                    proxies.add(new Proxy().setHeight(300).setWidth(300).setId("proxy/" + id + "_bing.jpg"));

                    ProxySchema p = new ProxySchema();
                    p.setProxies(proxies);
                    b.setAttr("proxies", p);
                    result.add(b);
                }
            }
        }

        for (File f: getTestImagePath(subdir).toFile().listFiles()) {
            if (f.isDirectory()) {
                result.addAll(getTestAssets(subdir + "/" + f.getName()));
            }
        }

        logger.info("TEST ASSET: {}", result);
        return result;
    }

    public void addTestAssets(String subdir) {
        addTestAssets(getTestAssets(subdir));
    }

    public void addTestAssets(List<Source> builders) {
        for (Source builder: builders) {
            logger.info("Adding test asset: {}", builder.getPath());
            AssetUtils.addKeywords(builder, "source", builder.getAttr("source.filename", String.class));
            assetService.index(builder);
        }
        refreshIndex();
    }

    public void refreshIndex() {
        ElasticClientUtils.refreshIndex(client, 10);
    }

    public void refreshIndex(long sleep) { ElasticClientUtils.refreshIndex(client, sleep); }

    public AnalystSpec sendAnalystPing() {
        AnalystSpec ab = getAnalystBuilder();
        ab.setId("ab");
        analystDao.register(ab);
        refreshIndex();
        return ab;
    }

    public AnalystSpec getAnalystBuilder() {
        AnalystSpec ping = new AnalystSpec();
        ping.setUrl("https://192.168.100.100:8080");
        ping.setData(false);
        ping.setState(AnalystState.UP);
        ping.setOs("test");
        ping.setArch("test_x86-64");
        ping.setThreadCount(2);
        return ping;
    }
}
