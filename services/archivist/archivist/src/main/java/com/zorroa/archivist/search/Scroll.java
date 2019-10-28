package com.zorroa.archivist.search;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Created by chambers on 9/29/16.
 */
@ApiModel(value = "Scroll", description = "Describes an Elasticsearch (ES) scroll that allows for paging through search results.")
public class Scroll {

    @ApiModelProperty("How long to keep the scroll alive. \"1m\" would keep it alive for 1 minute.")
    private String timeout = "1m";

    @ApiModelProperty("Unique ID of the ES scroll.")
    private String id;

    public Scroll() { }

    public Scroll(String id) {
        this.id = id;
    }

    public String getTimeout() {
        return timeout;
    }

    public Scroll setTimeout(String timeout) {
        this.timeout = timeout;
        return this;
    }

    public String getId() {
        return id;
    }

    public Scroll setId(String id) {
        this.id = id;
        return this;
    }
}
