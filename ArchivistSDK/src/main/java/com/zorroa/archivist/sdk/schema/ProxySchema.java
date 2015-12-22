package com.zorroa.archivist.sdk.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zorroa.archivist.sdk.domain.Proxy;

/**
 * The ProxySchema is a list of available proxy objects.
 */
public class ProxySchema extends ListSchema<Proxy> {

    @Override
    @JsonIgnore
    public String getNamespace() {
        return "proxies";
    }
}
