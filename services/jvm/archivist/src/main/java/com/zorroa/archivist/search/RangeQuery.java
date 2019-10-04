package com.zorroa.archivist.search;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Created by chambers on 7/26/16.
 */
@ApiModel(value = "Range Query", description = "Query within a set range.")
public class RangeQuery {

    @ApiModelProperty("Values must be greater than this.")
    public  Object gt;

    @ApiModelProperty("Values must be less than this.")
    public Object lt;

    @ApiModelProperty("Values must be greater than or equal to this.")
    public Object gte;

    @ApiModelProperty("Values must be less than or equal to this.")
    public Object lte;

    @ApiModelProperty("")
    public Object from;

    @ApiModelProperty("")
    public Object to;

    @ApiModelProperty("Optional date formats to use. Example: \"dd/MM/yyyy||yyyy\"")
    public String format;

    @ApiModelProperty("ES Boost value of the query.")
    public float boost = 1.0f;

    public RangeQuery() { }

    public Object getGt() {
        return gt;
    }

    public RangeQuery setGt(Object gt) {
        this.gt = gt;
        return this;
    }

    public Object getLt() {
        return lt;
    }

    public RangeQuery setLt(Object lt) {
        this.lt = lt;
        return this;
    }

    public Object getGte() {
        return gte;
    }

    public RangeQuery setGte(Object gte) {
        this.gte = gte;
        return this;
    }

    public Object getLte() {
        return lte;
    }

    public RangeQuery setLte(Object lte) {
        this.lte = lte;
        return this;
    }

    public Object getFrom() {
        return from;
    }

    public RangeQuery setFrom(Object from) {
        this.from = from;
        return this;
    }

    public Object getTo() {
        return to;
    }

    public RangeQuery setTo(Object to) {
        this.to = to;
        return this;
    }

    public String getFormat() {
        return format;
    }

    public RangeQuery setFormat(String format) {
        this.format = format;
        return this;
    }

    public float getBoost() {
        return boost;
    }

    public RangeQuery setBoost(float boost) {
        this.boost = boost;
        return this;
    }
}
