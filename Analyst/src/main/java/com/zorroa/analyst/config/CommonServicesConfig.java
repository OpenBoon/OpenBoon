package com.zorroa.analyst.config;

import com.zorroa.common.service.EventLogService;
import com.zorroa.common.service.EventLogServiceImpl;
import org.springframework.context.annotation.Bean;

/**
 * Created by chambers on 2/16/16.
 */
public class CommonServicesConfig {

    @Bean
    public EventLogService eventLogService() {
        return new EventLogServiceImpl();
    }

}
