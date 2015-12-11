package com.zorroa.archivist.sdk.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zorroa.archivist.sdk.domain.Proxy;

/**
 * Created by chambers on 12/11/15.
 */
public class ProxySchema extends ListSchema<Proxy> {

    @Override
    @JsonIgnore
    public String getNamespace() {
        return "proxies";
    }
}
