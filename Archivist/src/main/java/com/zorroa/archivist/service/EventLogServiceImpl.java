package com.zorroa.archivist.service;

import com.zorroa.archivist.repository.EventLogDao;
import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.EventLogMessage;
import com.zorroa.archivist.sdk.domain.Id;
import com.zorroa.archivist.sdk.service.EventLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by chambers on 12/29/15.
 */
@Service
public class EventLogServiceImpl implements EventLogService {

    protected final Logger logger = LoggerFactory.getLogger(EventLogServiceImpl.class);

    @Autowired
    EventLogDao eventLogDao;

    @Override
    public void log(EventLogMessage logMessageBuilder) {
        eventLogDao.log(logMessageBuilder);
        logger.info(logMessageBuilder.toString());
    }

    @Override
    public void log(Id object, String message, Object... args) {
        EventLogMessage event = new EventLogMessage(object, message, args);
        log(event);
    }

    @Override
    public void log(Id object, String message, Throwable ex, Object... args) {
        EventLogMessage event = new EventLogMessage(object, message, ex, args);
        log(event);
    }

    @Override
    public void log(Asset asset, String message, Object... args) {
        EventLogMessage event = new EventLogMessage(asset, message, args);
        log(event);
    }

    @Override
    public void log(Asset asset, String message, Throwable ex, Object... args) {
        EventLogMessage event = new EventLogMessage(asset, message, ex, args);
        log(event);
    }

    @Override
    public void log(String message, Object... args) {
        EventLogMessage event = new EventLogMessage(message, args);
        log(event);
    }
}
