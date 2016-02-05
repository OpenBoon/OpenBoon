package com.zorroa.archivist.sdk.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by chambers on 11/23/15.
 */
public interface Schema {

    @JsonIgnore
    String getNamespace();
}
