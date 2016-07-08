package com.zorroa.common.config;

import com.zorroa.common.repository.*;
import com.zorroa.common.service.EventLogService;
import com.zorroa.common.service.EventLogServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by chambers on 2/16/16.
 */
@Configuration
public class CommonBeanConfig {

    @Bean
    public EventLogService eventLogService() {
        return new EventLogServiceImpl();
    }

    @Bean
    public AssetDao assetDao() {
        return new AssetDaoImpl();
    }

    @Bean
    public AnalystDao analystDao() {
        return new AnalystDaoImpl();
    }

    @Bean
    public PluginDao pluginDao() {
        return new PluginDaoImpl();
    }

    @Bean
    public ModuleDao moduleDao() {
        return new ModuleDaoImpl();
    }

    @Bean
    public ClusterSettingsDao clusterConfigDao() { return new ClusterSettingsDaoImpl(); }

    @Bean
    public TaskDao taskDao() { return new TaskDaoImpl(); }
}
