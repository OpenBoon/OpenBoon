package com.zorroa.archivist.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.UniqueTaskExecutor;
import com.zorroa.archivist.security.UserDetailsPluginWrapper;
import com.zorroa.archivist.security.UserDetailsPopulator;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import com.zorroa.sdk.filesystem.UUIDFileSystem;
import com.zorroa.sdk.processor.SharedData;
import com.zorroa.security.UserDetailsPlugin;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
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
    public InfoEndpoint infoEndpoint() throws IOException {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("description", "Zorroa Archivist Server");
        map.put("project", "zorroa-archivist");

        Properties props = new Properties();
        props.load(new ClassPathResource("version.properties").getInputStream());
        map.put("version", props.getProperty("build.version"));
        map.put("date", props.getProperty("build.date"));
        map.put("user", props.getProperty("build.user"));
        map.put("commit",props.getProperty("build.id"));
        map.put("branch",props.getProperty("build.branch"));
        map.put("dirty",props.getProperty("build.dirty"));
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
    public ObjectFileSystem ofs() {
        UUIDFileSystem ufs = new UUIDFileSystem(new File(properties.getString("archivist.path.ofs")));
        ufs.init();
        return ufs;
    }

    @Bean
    public SharedData sharedData() {
        return new SharedData(properties.getString("archivist.path.shared"));
    }

    /**
     * Handles conversion of LDAP properties to a UserAuthed object
     * as well as permission loading.
     * @return
     */
    @Bean
    public UserDetailsPopulator userDetailsPopulator() {
        return new UserDetailsPopulator();
    }

    @Bean
    public UserDetailsPluginWrapper userDetailsPluginWrapper() {
        String pluginClassName =
                properties.getString("archivist.security.ldap.userDetailsPlugin").trim();

        UserDetailsPlugin plugin = null;
        if (!StringUtils.isBlank(pluginClassName)) {
            plugin = instantiate(pluginClassName, UserDetailsPlugin.class);
        }
        return new UserDetailsPluginWrapper(plugin);
    }

    @Bean
    public UniqueTaskExecutor folderTaskExecutor() {
        /**
         * The unittest property makes tasks run inline.
         */
        UniqueTaskExecutor t = new UniqueTaskExecutor(unittest);
        return t;
    }


    public static <T> T instantiate(final String className, final Class<T> type){
        try{
            return type.cast(Class.forName(className).newInstance());
        } catch(InstantiationException
                | IllegalAccessException
                | ClassNotFoundException e){
            throw new IllegalStateException(e);
        }
    }

}
