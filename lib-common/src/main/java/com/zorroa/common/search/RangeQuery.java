package com.zorroa.common.search;

/**
 * Created by chambers on 7/26/16.
 */
public class RangeQuery {

    public  Object gt;
    public Object lt;
    public Object gte;
    public Object lte;
    public Object from;
    public Object to;
    public String format;
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
