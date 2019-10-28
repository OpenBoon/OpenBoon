package com.zorroa.archivist.search;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;


@ApiModel(value = "Keyword Confidence Filter", description = "Filter keywords based on their confidence values.")
public class KwConfFilter {

    @ApiModelProperty("Keywords to apply filter to.")
    private List<String> keywords;

    @ApiModelProperty("Confidence value range to filter on. Example: [0.75, 0.86]")
    private List<Double> range;

    public KwConfFilter() { }
    public KwConfFilter(List<String> keywords,  List<Double> range) {
        this.keywords = keywords;
        this.range = range;
    }

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
