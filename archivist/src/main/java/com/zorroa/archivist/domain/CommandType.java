package com.zorroa.archivist.domain;

import com.zorroa.sdk.search.AssetSearch;

/**
 * Created by chambers on 4/24/17.
 */
public enum CommandType {

    UpdateAssetPermissions(AssetSearch.class, Acl.class),
    Sleep(Long.class);

    Class[] argTypes;

    CommandType(Class ... argTypes) {
        this.argTypes = argTypes;
    }

    public Class[] getArgTypes() {
        return argTypes;
    }
}
