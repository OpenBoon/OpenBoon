package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.EventLogMessage;
import com.zorroa.archivist.sdk.domain.EventLogSearch;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.slf4j.helpers.MessageFormatter;

/**
 * Created by chambers on 12/28/15.
 */
public interface EventLogDao {

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

    /**
     * Create a message in the event log, async communication.
     *
     * @param message
     */
    void log(EventLogMessage message);

    SearchResponse getAll();

    /**
     * Execute a search in the event log.
     *
     * @param search
     * @return
     */
    SearchResponse getAll(EventLogSearch search);

    /**
     * Execute a count query in the event log.
     *
     * @param search
     * @return
     */
    CountResponse getCount(EventLogSearch search);

    /**
     * Only set true for unit tests.  When set to true, all of the log
     * functions are synchronous.
     *
     * @param value
     */
    void setSynchronous(boolean value);
}
