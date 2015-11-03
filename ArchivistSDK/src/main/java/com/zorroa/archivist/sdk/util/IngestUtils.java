package com.zorroa.archivist.sdk.util;

import com.google.common.collect.ImmutableSet;

import javax.imageio.ImageIO;
import java.util.Arrays;

/**
 * Created by chambers on 10/30/15.
 *
 * These can be static utils, if we need them.
 *
 */
public class IngestUtils {

    public static final ImmutableSet<String> SUPPORTED_IMG_FORMATS = ImmutableSet.<String>builder()
            .addAll(Arrays.asList(ImageIO.getReaderFormatNames())).build();

    /*
    public static File getResourceFile(String path) throws FileNotFoundException {
        URL url = ResourceUtils.getURL(path);
        Path resourcePath = Paths.get(url.toURI());
        return new File(resourcePath.toUri());

    }

    public static File getProxyFile(String filename, String extension) throws FileNotFoundException {
        File proxyFile = getResourceFile("/proxies");
        return new File(proxyFile.getAbsoluteFile() + "/" + filename + "." + extension);
    }
    */

}
