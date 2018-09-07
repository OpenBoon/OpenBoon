package com.zorroa.archivist.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chambers on 12/6/16.
 */
public class SimilarityFilter {

    private List<SimilarityHash> hashes;

    /**
     * A value between 1 and 100 which represents the minimum percent match.
     */
    private int minScore = 75;

    public SimilarityFilter() { }

    public SimilarityFilter(String hash, int minScore) {
        this.hashes = new ArrayList();
        this.hashes.add(new SimilarityHash(hash, 1.0f));
        this.minScore = minScore;
    }

    public SimilarityFilter(String hash, float weight, int minScore) {
        this.hashes = new ArrayList();
        this.hashes.add(new SimilarityHash(hash, weight));
        this.minScore = minScore;
    }

    public List<SimilarityHash> getHashes() {
        return hashes;
    }

    public SimilarityFilter setHashes(List<SimilarityHash> hashes) {
        this.hashes = hashes;
        return this;
    }

    public int getMinScore() {
        return minScore;
    }

    public SimilarityFilter setMinScore(int minScore) {
        this.minScore = minScore;
        return this;
    }

    public static class SimilarityHash {
        private String hash;
        private Float weight;
        private Integer order;

        public SimilarityHash() {  }

        public SimilarityHash(String hash, float weight) {
            this.hash = hash;
            this.weight = weight;
        }

        public String getHash() {
            return hash;
        }

        public SimilarityHash setHash(String hash) {
            this.hash = hash;
            return this;
        }

        public Float getWeight() {
            return weight;
        }

        public SimilarityHash setWeight(Float weight) {
            this.weight = weight;
            return this;
        }

        public Integer getOrder() {
            return order;
        }

        public SimilarityHash setOrder(Integer order) {
            this.order = order;
            return this;
        }
    }
}
