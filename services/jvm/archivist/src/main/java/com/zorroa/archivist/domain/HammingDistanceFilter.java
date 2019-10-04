package com.zorroa.archivist.domain;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Old hamming distance class.
 */
@Deprecated
public class HammingDistanceFilter {

    private List<Object> hashes;
    private List<Float> weights;    // must match hashes.size() or is ignored
    private List<String> assetIds;  // unused but retained for client UI
    private String field;
    private int minScore = 4;

    public HammingDistanceFilter() { }

    public HammingDistanceFilter(List<Object> hash, String field, int minScore) {
        this.hashes = hash;
        this.field = field;
        this.minScore = minScore;
    }

    public HammingDistanceFilter(String hash, String field, int minScore) {
        this.hashes = Lists.newArrayList(hash);
        this.field = field;
        this.minScore = minScore;
    }

    public List<Object> getHashes() {
        return hashes;
    }

    public HammingDistanceFilter setHashes(List<Object> hashes) {
        this.hashes = hashes;
        return this;
    }

    public List<Float> getWeights() {
        return weights;
    }

    public HammingDistanceFilter setWeights(List<Float> weights) {
        this.weights = weights;
        return this;
    }

    public List<String> getAssetIds() {
        return assetIds;
    }

    public HammingDistanceFilter setAssetIds(List<String> assetIds) {
        this.assetIds = assetIds;
        return this;
    }

    public String getField() {
        return field;
    }

    public HammingDistanceFilter setField(String field) {
        this.field = field;
        return this;
    }

    public int getMinScore() {
        return minScore;
    }

    public HammingDistanceFilter setMinScore(int minScore) {
        this.minScore = minScore;
        return this;
    }
}

