package com.zorroa.zmlp.client.domain.similarity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BatchSimilarityFilter {

    private List<SimilarityFilter> batch;

    public BatchSimilarityFilter() {
        batch = new ArrayList();
    }

    public BatchSimilarityFilter add(SimilarityFilter similarityFilter) {
        this.batch.add(similarityFilter);
        return this;
    }

    public BatchSimilarityFilter clear() {
        this.batch.clear();
        return this;
    }

    public List<SimilarityFilter> getBatch() {
        return batch;
    }

    @Override
    public String toString() {
        return this.batch
                .stream()
                .map(filters -> filters.toMap())
                .collect(Collectors.toList())
                .toString();
    }
}
