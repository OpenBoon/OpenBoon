package com.zorroa.archivist.web;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.LfsRequest;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.service.ImageService;
import com.zorroa.archivist.service.JobService;
import com.zorroa.archivist.service.LocalFileSystem;
import com.zorroa.sdk.filesystem.ObjectFile;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import com.zorroa.sdk.util.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Provides all endpoints for downloading binary data like proxies, assets, export
 *
 * @author chambers
 *
 */
@RestController
@Component
public class FileSystemController {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemController.class);

    @Autowired
    AssetDao assetDao;

    @Autowired
    ImageService imageService;

    @Autowired
    ObjectFileSystem objectFileSystem;

    @Autowired
    LocalFileSystem localFileSystem;

    @Autowired
    JobService jobService;

    @RequestMapping(value="/api/v1/ofs/_exists", method = RequestMethod.POST)
    public Object fileExists(@RequestBody Map<String, String> path) throws IOException {
        String file = path.get("path");
        if (file == null) {
            return ImmutableMap.of("result", false);
        }
        else {
            File f = new File(FileUtils.normalize(file));
            return ImmutableMap.of("result", f.exists());
        }
    }

    @RequestMapping(value = "/api/v1/ofs/{type}/**", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<InputStreamResource> getFile(@PathVariable String type, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "public");

        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        AntPathMatcher apm = new AntPathMatcher();
        String id = type + "/" + FileUtils.filename(apm.extractPathWithinPattern(bestMatchPattern, path));
        ObjectFile file = objectFileSystem.get(id);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(file.getFile().toPath()))
                .body(new InputStreamResource(new FileInputStream(file.getFile())));
    }

    public static class ProxyUpload {
        public List<MultipartFile> files;

        public List<MultipartFile> getFiles() {
            return files;
        }

        public ProxyUpload setFiles(List<MultipartFile> files) {
            this.files = files;
            return this;
        }
    }

    /**
     * This method is used by cloud proxy
     *
     * @param upload
     * @return
     * @throws IOException
     */
    @RequestMapping(value="/api/v1/ofs/{type}", method = RequestMethod.POST)
    public Object proxyUpload(@PathVariable String type, ProxyUpload upload) throws IOException {

        if (upload.getFiles() == null || upload.getFiles().isEmpty()) {
            return HttpUtils.status("ofs", "upload", false);
        }

        if (!StringUtils.isAlphanumeric(type)) {
            return HttpUtils.status("ofs", "upload", false);
        }

        try {
            for (MultipartFile file : upload.getFiles()) {
                String id = type + "/" + file.getOriginalFilename();
                ObjectFile of = objectFileSystem.get(id);
                of.mkdirs();

                File dstFile = of.getFile();
                if (!dstFile.exists()) {
                    Files.copy(file.getInputStream(), dstFile.toPath());
                }
            }
            return HttpUtils.status("proxy", "upload", true);
        } catch (Exception e) {
            logger.warn("Failed to upload proxies", e);
        }
        return HttpUtils.status("proxy", "upload", false);
    }

    @RequestMapping(value="/api/v1/lfs", method = RequestMethod.POST)
    public Object localFiles(@RequestBody LfsRequest req) throws IOException {
        return localFileSystem.listFiles(req);
    }

    @RequestMapping(value="/api/v1/lfs/_suggest", method = RequestMethod.POST)
    public List<String> localFilesSuggest(@RequestBody LfsRequest req) throws IOException {
        return localFileSystem.suggest(req);
    }

    @RequestMapping(value="/api/v1/lfs/_exist", method = RequestMethod.POST)
    public Object localFileExists(@RequestBody LfsRequest req) throws IOException {
       return  HttpUtils.exists(req.getPath(), localFileSystem.exists(req));
    }
}
