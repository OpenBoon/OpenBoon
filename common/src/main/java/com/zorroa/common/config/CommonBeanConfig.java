package com.zorroa.common.config;

import com.zorroa.common.repository.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by chambers on 2/16/16.
 */
@Configuration
public class CommonBeanConfig {

    @Bean
    public AssetDao assetDao() {
        return new AssetDaoImpl();
    }

    @Bean
    public AnalystDao analystDao() {
        return new AnalystDaoImpl();
    }

}
