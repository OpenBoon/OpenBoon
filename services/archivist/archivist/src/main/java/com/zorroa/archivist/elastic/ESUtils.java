package com.zorroa.archivist.elastic;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.client.RestHighLevelClient;

public class ESUtils {

    public static BulkProcessor.Builder create(RestHighLevelClient client, BulkProcessor.Listener listener) {
        return BulkProcessor.builder(client::bulkAsync, listener);
    }
}
