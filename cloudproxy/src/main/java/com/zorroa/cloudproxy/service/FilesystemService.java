package com.zorroa.cloudproxy.service;

import com.zorroa.cloudproxy.domain.FilesystemEntry;

import java.util.List;

/**
 * Created by wex on 4/1/17.
 */
public interface FilesystemService {
    List<FilesystemEntry> get(String path, List<String> hidden);
}
