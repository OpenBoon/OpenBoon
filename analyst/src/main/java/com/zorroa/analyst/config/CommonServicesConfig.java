package com.zorroa.analyst.config;

import com.zorroa.common.repository.AnalystDao;
import com.zorroa.common.repository.AnalystDaoImpl;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.common.repository.AssetDaoImpl;
import com.zorroa.common.service.EventLogService;
import com.zorroa.common.service.EventLogServiceImpl;
import com.zorroa.sdk.config.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by chambers on 2/16/16.
 */
@Configuration
public class CommonServicesConfig {

    @Autowired
    ApplicationProperties applicationProperties;

    @Bean
    public EventLogService eventLogService() {
        return new EventLogServiceImpl();
    }

    @Bean
    public AssetDao assetDao() {
        return new AssetDaoImpl(applicationProperties.getString("zorroa.common.index.alias"));
    }

    @Bean
    public AnalystDao analystDao() {
        return new AnalystDaoImpl(applicationProperties.getString("zorroa.common.index.alias"));
    }
}
