package com.zorroa.archivist.service;

import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.Document;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Created by chambers on 11/4/15.
 */
public class ScanAndScrollAssetIterator implements Iterable<Document> {

    private final SearchResponse rsp;
    private final Client client;

    public ScanAndScrollAssetIterator(Client client, SearchResponse rsp) {
        this.rsp = rsp;
        this.client = client;
    }

    @Override
    public Iterator<Document> iterator() {
        Iterator<Document> it = new Iterator<Document>() {

            SearchHit[] hits = rsp.getHits().getHits();
            private int index = 0;

            @Override
            public boolean hasNext() {
                if (index >= hits.length) {
                    hits = client.prepareSearchScroll(rsp.getScrollId()).setScroll("5m")
                            .get().getHits().getHits();
                    index = 0;
                }

                boolean hasMore =  index < hits.length;
                if (!hasMore) {
                    client.prepareClearScroll().addScrollId(rsp.getScrollId()).execute();
                }
                return hasMore;
            }

            @Override
            public Asset next() {
                Asset asset = new Asset();
                SearchHit hit = hits[index++];

                asset.setDocument(hit.getSource());
                asset.setId(hit.getId());

                return asset;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void forEachRemaining(Consumer<? super Document> action) {
                throw new UnsupportedOperationException();
            }
        };
        return it;
    }

    @Override
    public void forEach(Consumer<? super Document> action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Spliterator<Document> spliterator() {
        throw new UnsupportedOperationException();
    }
}
