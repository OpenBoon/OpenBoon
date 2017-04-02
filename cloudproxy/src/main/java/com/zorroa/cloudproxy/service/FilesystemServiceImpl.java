package com.zorroa.cloudproxy.service;

import com.google.common.collect.Lists;
import com.zorroa.cloudproxy.domain.FilesystemEntry;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wex on 4/1/17.
 */
@Component
public class FilesystemServiceImpl implements FilesystemService {
    public List<FilesystemEntry> get(String path) {
        File folder = new File(path);
        File[] files = folder.listFiles();
        ArrayList<FilesystemEntry> entries = Lists.newArrayList();
        for (int i = 0; i < files.length; i++) {
            entries.add(new FilesystemEntry(files[i]));
        }
        return entries;
    }
}
