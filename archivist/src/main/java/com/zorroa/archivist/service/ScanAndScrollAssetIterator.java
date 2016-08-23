package com.zorroa.archivist.service;

import com.zorroa.sdk.domain.Asset;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Created by chambers on 11/4/15.
 */
public class ScanAndScrollAssetIterator implements Iterable<Asset> {

    private final SearchResponse rsp;
    private final Client client;

    public ScanAndScrollAssetIterator(Client client, SearchResponse rsp) {
        this.rsp = rsp;
        this.client = client;
    }

    @Override
    public Iterator<Asset> iterator() {
        Iterator<Asset> it = new Iterator<Asset>() {

            SearchHit[] hits = rsp.getHits().getHits();
            private int index = 0;

            @Override
            public boolean hasNext() {
                if (index >= hits.length) {
                    hits = client.prepareSearchScroll(rsp.getScrollId()).setScroll("10m")
                            .get().getHits().getHits();
                    index = 0;
                }

                return index < hits.length;
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
            public void forEachRemaining(Consumer<? super Asset> action) {
                throw new UnsupportedOperationException();
            }
        };
        return it;
    }

    @Override
    public void forEach(Consumer<? super Asset> action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Spliterator<Asset> spliterator() {
        throw new UnsupportedOperationException();
    }
}
