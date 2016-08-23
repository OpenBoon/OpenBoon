package com.zorroa.analyst.service;

import com.zorroa.analyst.AbstractTest;
import com.zorroa.common.repository.AnalystDao;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.util.Json;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by chambers on 4/28/16.
 */
public class RegisterServiceTests extends AbstractTest {

    @Autowired
    RegisterService registerService;

    @Autowired
    AnalystDao analystDao;

    @Test
    public void testRegister() {
        String id = registerService.register();
        refreshIndex();

        Analyst analyst = analystDao.get(id);
        logger.info("{}", Json.serializeToString(analyst));
    }
}
