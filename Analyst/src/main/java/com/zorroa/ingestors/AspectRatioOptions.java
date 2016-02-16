package com.zorroa.ingestors;

import java.io.Serializable;

/**
 * Created by jbuhler on 2/12/16.
 */

public class AspectRatioOptions implements Serializable {

    private Float minAspect, maxAspect;
    private String keyword;

    public AspectRatioOptions() {
    }

    public AspectRatioOptions(Float minAspect, Float maxAspect, String keyword) {
        this.minAspect = minAspect;
        this.maxAspect = maxAspect;
        this.keyword = keyword;
    }

    public boolean isWithin(double aspect) {
        if (aspect > minAspect && aspect < maxAspect) {
            return true;
        }
        return false;
    }
    public String getKeyword() {
        return keyword;
    }

    public Float getMinAspect() {
        return minAspect;
    }

    public void setMinAspect(Float minAspect) {
        this.minAspect = minAspect;
    }

    public Float getMaxAspect() {
        return maxAspect;
    }

    public void setMaxAspect(Float maxAspect) {
        this.maxAspect = maxAspect;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
