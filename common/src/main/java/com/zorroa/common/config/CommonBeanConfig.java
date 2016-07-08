package com.zorroa.common.config;

import com.zorroa.common.repository.*;
import com.zorroa.common.service.EventLogService;
import com.zorroa.common.service.EventLogServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by chambers on 2/16/16.
 */
@Configuration
public class CommonBeanConfig {

    @Value("${zorroa.cluster.index.alias}")
    private String alias;

    @Bean
    public EventLogService eventLogService() {
        return new EventLogServiceImpl();
    }

    @Bean
    public AssetDao assetDao() {
        return new AssetDaoImpl(alias);
    }

    @Bean
    public AnalystDao analystDao() {
        return new AnalystDaoImpl(alias);
    }

    @Bean
    public PluginDao pluginDao() {
        return new PluginDaoImpl(alias);
    }

    @Bean
    public ModuleDao moduleDao() {
        return new ModuleDaoImpl(alias);
    }
}
