package com.zorroa.archivist;

import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.archivist.web.RestControllerAdvice;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass=true)
public class ArchivistConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistConfiguration.class);

    private String nodeName;
    private String hostName;

    public static boolean unittest = false;

    @PostConstruct
    public void init() throws UnknownHostException {
        Random rand = new Random(System.nanoTime());
        int number = rand.nextInt(99999);

        hostName = InetAddress.getLocalHost().getHostName();
        nodeName = String.format("%s_%05d", hostName, number);
    }

    @Bean
    public RestControllerAdvice restControllerAspect() {
        return new RestControllerAdvice();
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
    @Autowired
    public TransactionEventManager transactionEventManager() {
        return new TransactionEventManager();
    }

    @Bean
    public Client elastic() throws IOException {

        org.elasticsearch.common.settings.ImmutableSettings.Builder builder =
                ImmutableSettings.settingsBuilder()
                .put("cluster.name", "zorroa")
                .put("node.name", nodeName)
                .put("discovery.zen.ping.multicast.enabled", false)
                .put("cluster.routing.allocation.disk.threshold_enabled", false)
                .put("script.native.archivistDate.type", "com.zorroa.archivist.ArchivistDateScriptFactory")
                .put("script.indexed", true)
                .put("script.update", true)
                .put("script.engine.groovy.indexed.update", true);

        if (unittest) {
            builder.put("path.data", "unittest/data");
            builder.put("index.refresh_interval", "1s");
            builder.put("index.translog.disable_flush", true);
        }

        Node node = nodeBuilder()
                .data(true)
                .local(true)
                .settings(builder.build())
                .node();

        return node.client();
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
}
