package com.zorroa.archivist.search;

import java.util.List;

public class KwConfFilter {

    public KwConfFilter() { }
    public KwConfFilter(List<String> keywords,  List<Double> range) {
        this.keywords = keywords;
        this.range = range;
    }

    private List<String> keywords;
    private List<Double> range;

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<Double> getRange() {
        return range;
    }

    public void setRange(List<Double> range) {
        this.range = range;
    }
}
