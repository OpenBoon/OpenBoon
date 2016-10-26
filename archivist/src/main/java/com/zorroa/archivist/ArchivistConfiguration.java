package com.zorroa.archivist;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.security.JdbcSessionRegistry;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import com.zorroa.sdk.filesystem.UUIDFileSystem;
import com.zorroa.sdk.processor.SharedData;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import javax.sql.DataSource;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass=true)
public class ArchivistConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistConfiguration.class);

    public static boolean unittest = false;

    @Autowired
    ApplicationProperties properties;

    @Bean
    @ConfigurationProperties(prefix="archivist.datasource.primary")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name="flyway", initMethod="migrate")
    @Autowired
    public Flyway flyway(DataSource datasource) {
        Flyway flyway =  new Flyway();
        flyway.setDataSource(datasource);
        return flyway;
    }

    @Bean
    @Autowired
    public DataSourceTransactionManager transactionManager(DataSource datasource) {
        return new DataSourceTransactionManager(datasource);
    }

    @Bean
    public TransactionEventManager transactionEventManager() {
        return new TransactionEventManager();
    }

    @Bean
    public InfoEndpoint infoEndpoint() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("description", "Zorroa Archivist Server");
        map.put("project", "zorroa-archivist");
        Properties props = new Properties();
        try {
            props.load(new ClassPathResource("META-INF/maven/com.zorroa/archivist/pom.properties").getInputStream());
            map.put("version", props.getProperty("version"));
        } catch (Exception e) {
            if (!ArchivistConfiguration.unittest) {
                logger.warn("Failed to load version info,", e);
            }
        }
        return new InfoEndpoint(ImmutableMap.of("build", map));
    }

    @Bean
    public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
        RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
        adapter.setMessageConverters(Lists.newArrayList(
                new MappingJackson2HttpMessageConverter()
        ));
        return adapter;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new JdbcSessionRegistry();
    }

    @Bean
    public ObjectFileSystem ofs() {
        UUIDFileSystem ufs = new UUIDFileSystem(new File(properties.getString("archivist.path.ofs")));
        ufs.init();
        return ufs;
    }

    @Bean
    public SharedData sharedData() {
        return SharedData.builder().setRootPath(properties.getString("archivist.path.shared"))
                .setModelPath(properties.getString("archivist.path.models"))
                .setOfsPath(properties.getString("archivist.path.ofs"))
                .setPluginPath(properties.getString("archivist.path.plugins"))
                .build();
    }

}
