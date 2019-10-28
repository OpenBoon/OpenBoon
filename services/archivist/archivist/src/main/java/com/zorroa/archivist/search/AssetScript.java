/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.search;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Map;

@ApiModel(value = "Asset Script", description = "Script to use for querying or filtering Assets.")
public class AssetScript {

    @ApiModelProperty("Script to execute.")
    private String script;

    @ApiModelProperty("Parameters to pass to the script.")
    private Map<String, Object> params;

    @ApiModelProperty("Type of script this is.")
    private String type = "expression";

    public AssetScript() { }

    public AssetScript(String script, Map<String, Object> params) {
        this.script = script;
        this.params = params;
    }

    public String getScript() {
        return script;
    }

    public AssetScript setScript(String script) {
        this.script = script;
        return this;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public AssetScript setParams(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    public String getType() {
        return type;
    }

    public AssetScript setType(String type) {
        this.type = type;
        return this;
    }
}
