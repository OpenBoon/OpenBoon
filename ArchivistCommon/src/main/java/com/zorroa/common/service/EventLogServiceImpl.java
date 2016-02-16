package com.zorroa.common.service;

import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.EventLogMessage;
import com.zorroa.archivist.sdk.domain.Id;
import com.zorroa.archivist.sdk.util.Json;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created by chambers on 12/29/15.
 */
public class EventLogServiceImpl implements EventLogService {

    protected final Logger logger = LoggerFactory.getLogger(EventLogServiceImpl.class);

    @Autowired
    Client client;

    private String hostname;
    private boolean synchronous = false;

    @PostConstruct
    public void init() {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
    }

    @Override
    public void log(EventLogMessage logMessageBuilder) {
        if (logMessageBuilder.getException()!= null) {
            logger.warn(logMessageBuilder.toString(), logMessageBuilder.getException());
        }
        else {
            logger.info(logMessageBuilder.toString());
        }

        log(logMessageBuilder);
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

    private void create(EventLogMessage event, ActionListener<IndexResponse> listener) {
        Map<String, Object> source = Maps.newHashMap();
        source.put("id", event.getId());
        source.put("type", event.getType());
        source.put("timestamp", event.getTimestamp());
        source.put("message", event.getMessage());
        source.put("tags", event.getTags());
        source.put("stack", getStackTrace(event.getException()));
        source.put("path", event.getPath());
        source.put("host", hostname);

        String date = new SimpleDateFormat("yyyy_MM_dd").format(new Date(event.getTimestamp()));
        String str = Json.serializeToString(source);

        IndexRequestBuilder builder = client.prepareIndex("eventlog_" + date, "event")
                .setOpType(IndexRequest.OpType.INDEX).setSource(str);
        if (listener != null) {
            builder.execute(listener);
        }
        else if (synchronous) {
            builder.get();
        } else {
            builder.execute();
        }
    }

    static String[] getStackTrace(Throwable ex) {
        if (ex == null) {
            return null;
        }

        StackTraceElement[] e = ex.getStackTrace();
        final int length = e.length;

        String[] stack = new String[length];

        for (int i = 0; i < length; i++) {
            stack[i] = MessageFormatter.arrayFormat("{}.{} {}(line:{})",
                    new Object[]{
                            e[i].getClassName(),
                            e[i].getFileName(),
                            e[i].getMethodName(),
                            e[i].getLineNumber()
                    }).getMessage();
        }
        return stack;
    }
}
