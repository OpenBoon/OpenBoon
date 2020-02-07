package com.zorroa.zmlp.client.domain.similarity;

import java.util.HashMap;
import java.util.Map;

public class SimilarityFilter {

    private String hash;
    private Double weight;

    public SimilarityFilter(String hash, Double weight) {
        this.hash = hash;
        this.weight = weight;
    }


    public SimilarityFilter(String hash) {
        this.hash = hash;
        this.weight = 1.0;
    }

    public String getHash() {
        return hash;
    }

    public SimilarityFilter setHash(String hash) {
        this.hash = hash;
        return this;
    }

    public Double getWeight() {
        return weight;
    }

    public SimilarityFilter setWeight(Double weight) {
        this.weight = weight;
        return this;
    }

    public Map toMap(){
        Map ret = new HashMap();
        ret.put("hash", this.hash);
        ret.put("weight", this.weight);
        return ret;
    }


    @Override
    public String toString() {
        return toMap().toString();
    }
}
