package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.LfsRequest;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.client.exception.MissingElementException;
import com.zorroa.sdk.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LocalFileSystemImpl implements LocalFileSystem {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileSystemImpl.class);

    @Autowired
    ApplicationProperties properties;

    List<String> pathSuggestFilter = Lists.newArrayList();

    @PostConstruct
    public void init() {
        List<String> paths = properties.getList("archivist.lfs.paths");
        if (paths != null) {
            for (String entry: paths) {
                String path = FileUtils.normalize(entry);
                pathSuggestFilter.add(path);
                logger.info("Allowing Imports from '{}'", path);
            }
        }
    }

    @Override
    public Map<String, List<String>> listFiles(LfsRequest req) {
        if (!SecurityUtils.hasPermission(properties.getList("archivist.lfs.permissions"))){
            throw new MissingElementException("The path does not exist");
        }
        return _listFiles(req);
    }

    @Override
    public Boolean exists(LfsRequest req) {
        permissionCheck();
        String path = FileUtils.normalize(req.getPath());
        if (!isLocalPathAllowed(FileUtils.normalize(path))) {
            return false;
        }
        return Files.exists(Paths.get(path));
    }

    @Override
    public List<String> suggest(LfsRequest req) {
        permissionCheck();
        Map<String, List<String>> files = _listFiles(req);

        List<String> result = Lists.newArrayList();
        result.addAll(files.get("dirs").stream().map(f->f.concat("/")).collect(Collectors.toList()));
        result.addAll(files.get("files"));
        Collections.sort(result);
        return result;
    }

    public Map<String, List<String>> _listFiles(LfsRequest req) {
        Map<String, List<String>> result = ImmutableMap.of(
                "dirs", Lists.newArrayList(),
                "files", Lists.newArrayList());

        /*
         * Gotta normalize it since we allow relative paths for testing purposes.
         */
        String path = FileUtils.normalize(req.getPath());
        if (!isLocalPathAllowed(path)) {
            logger.warn("User {} attempted to list files in: {}",
                 SecurityUtils.getUsername(), path);
            return result;
        }

        try {
            for (File f : new File(path).listFiles()) {
                if (f.isHidden()) {
                    continue;
                }
                if (req.getPrefix() != null) {
                    if (!f.getName().startsWith(req.getPrefix())) {
                        continue;
                    }
                }

                String t = f.isDirectory() ? "dirs" : "files";

                if (t.equals("files") && !req.getTypes().isEmpty()) {
                    if (!req.getTypes().contains(FileUtils.extension(f.getName()))) {
                        continue;
                    }
                }

                result.get(t).add(f.getName());
            }
        } catch (Exception e) {
            return result;
        }

        Collections.sort(result.get("dirs"));
        Collections.sort(result.get("files"));
        return result;
    }

    @Override
    public boolean isLocalPathAllowed(String path) {
        if (pathSuggestFilter.isEmpty()) {
            return false;
        }
        else {
            boolean matched = false;
            for (String filter: pathSuggestFilter) {
                if (path.startsWith(filter)) {
                    matched = true;
                    break;
                }
            }
            return matched;
        }
    }

    public void permissionCheck() {
        if (!SecurityUtils.hasPermission(properties.getList("archivist.lfs.permissions"))){
            throw new MissingElementException("The path does not exist");
        }

    }
}
