package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.LfsRequest;

import java.util.List;
import java.util.Map;

public interface LocalFileSystem {
    Map<String, List<String>> listFiles(LfsRequest req);

    Boolean exists(LfsRequest req);

    List<String> suggest(LfsRequest req);

    boolean isLocalPathAllowed(String path);
}
