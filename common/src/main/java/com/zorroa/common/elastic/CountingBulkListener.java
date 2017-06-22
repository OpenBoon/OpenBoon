package com.zorroa.common.elastic;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;

import java.util.concurrent.atomic.LongAdder;

/**
 * Created by chambers on 6/22/17.
 */
public class CountingBulkListener implements  BulkProcessor.Listener {

    private final LongAdder successCount = new LongAdder();
    private final LongAdder errorCount = new LongAdder();

    @Override
    public void beforeBulk(long l, BulkRequest bulkRequest) {

    }

    @Override
    public void afterBulk(long l, BulkRequest bulkRequest, BulkResponse bulkResponse) {
        successCount.add(bulkResponse.getItems().length);
    }

    @Override
    public void afterBulk(long l, BulkRequest bulkRequest, Throwable throwable) {
        errorCount.increment();
    }

    public long getSuccessCount() {
        return successCount.longValue();
    }

    public long getErrorCount() {
        return errorCount.longValue();
    }
}
