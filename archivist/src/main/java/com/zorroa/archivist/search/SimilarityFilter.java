package com.zorroa.archivist.search;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chambers on 12/6/16.
 */
@ApiModel(value = "Similarity Filter", description = "Filter that returns Assets that have at least a minimum matching score.")
public class SimilarityFilter {

    @ApiModelProperty("Similarity Hashes to match against.")
    private List<SimilarityHash> hashes;

    @ApiModelProperty("Value between 1 and 100 which represents the minimum percent match.")
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

    @ApiModel(value = "Similarity Hash", description = "Quantized feature vector, used to compare how similar two assets are.")
    public static class SimilarityHash {

        @ApiModelProperty("Hash used for comparison.")
        private String hash;

        @ApiModelProperty("Weighting of this Hash.")
        private Float weight;

        @ApiModelProperty("Order of this Hash.")
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
