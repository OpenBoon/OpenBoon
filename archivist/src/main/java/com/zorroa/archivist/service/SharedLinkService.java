package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.SharedLink;
import com.zorroa.archivist.domain.SharedLinkSpec;

/**
 * Created by chambers on 7/7/17.
 */
public interface SharedLinkService {
    SharedLink create(SharedLinkSpec spec);

    SharedLink get(int id);
}
