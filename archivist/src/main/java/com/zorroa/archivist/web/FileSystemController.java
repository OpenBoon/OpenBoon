package com.zorroa.archivist.web;

import com.zorroa.archivist.service.ImageService;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import com.zorroa.sdk.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Provides endpoints for downloading proxy images.
 *
 * @author chambers
 *
 */
@Controller
@Component
public class FileSystemController {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemController.class);

    @Autowired
    AssetDao assetDao;

    @Autowired
    ImageService imageService;

    @Autowired
    ObjectFileSystem objectFileSystem;

    /**
     * A special endpoint for streaming proxies from OFS.
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/ofs/proxy/**", method = RequestMethod.GET, produces = { MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE })
    @ResponseBody
    public byte[] getProxy(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "public");

        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        AntPathMatcher apm = new AntPathMatcher();
        String id = "proxy/" + FileUtils.filename(apm.extractPathWithinPattern(bestMatchPattern, path));
        BufferedImage image = ImageIO.read(objectFileSystem.get(id).getFile());
        String ext = FileUtils.extension(path);
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        ImageIO.write(imageService.watermark(image), ext, bao);
        return bao.toByteArray();
    }
}
