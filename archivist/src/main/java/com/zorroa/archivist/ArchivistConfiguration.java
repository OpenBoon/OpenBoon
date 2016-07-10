package com.zorroa.archivist;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zorroa.archivist.security.JdbcSessionRegistry;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.elastic.ArchivistDateScriptPlugin;
import com.zorroa.common.elastic.ZorroaNode;
import com.zorroa.sdk.config.ApplicationProperties;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.flywaydb.core.Flyway;
import org.h2.server.web.WebServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass=true)
public class ArchivistConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistConfiguration.class);

    private String nodeName;
    private String hostName;

    public static boolean unittest = false;

    @Autowired
    ApplicationProperties properties;

    @PostConstruct
    public void init() throws UnknownHostException {
        hostName = InetAddress.getLocalHost().getHostName();
        nodeName = String.format("%s_master", hostName);
    }

    @Bean
    public ServletRegistrationBean h2servletRegistration() {
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(new WebServlet());
        registrationBean.addUrlMappings("/console/*");
        return registrationBean;
    }

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
    public Client elastic() throws IOException {

        org.elasticsearch.common.settings.Settings.Builder builder =
                Settings.settingsBuilder()
                .put("path.data", properties.getString("archivist.path.index"))
                .put("path.home", properties.getString("archivist.path.home"))
                .put("cluster.name", "zorroa")
                .put("node.name", nodeName)
                .put("client.transport.sniff", true)
                .put("transport.tcp.port", properties.getInt("zorroa.cluster.index.port"))
                .put("discovery.zen.ping.multicast.enabled", false)
                .put("discovery.zen.fd.ping_timeout", "3s")
                .put("discovery.zen.fd.ping_retries", 10)
                .put("script.engine.groovy.indexed.update", true)
                .put("node.data", properties.getBoolean("zorroa.cluster.index.data"))
                .put("node.master", properties.getBoolean("zorroa.cluster.index.master"))
                .put("path.plugins", "{path.home}/es-plugins");

        if (unittest) {
            builder.put("index.refresh_interval", "1s");
            builder.put("index.translog.disable_flush", true);
            builder.put("node.local", true);
        }

        Node node = new ZorroaNode(builder.build(), ImmutableSet.of(ArchivistDateScriptPlugin.class));
        node.start();
        return node.client();
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

    public String getName() {
        return nodeName;
    }

    public String getHostName() {
        return hostName;
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
}
