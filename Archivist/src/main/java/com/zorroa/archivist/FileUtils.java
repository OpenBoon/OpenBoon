package com.zorroa.archivist;

import java.nio.file.Path;
import java.util.Set;

import javax.imageio.ImageIO;

import com.google.common.collect.ImmutableSet;

public class FileUtils {

    public static String extension(Path path) {
        try {
            String strPath = path.toString();
                return strPath.substring(strPath.lastIndexOf('.')+1);
        }
        catch (IndexOutOfBoundsException ignore) {
            //
        }
        return "";
    }

    public static Set<String> getSupportedImageFormats() {
        ImmutableSet.Builder<String> builder = ImmutableSet.<String>builder();
        for (String name: ImageIO.getReaderFormatNames()) {
            builder.add(name);
        }
        return builder.build();
    }

}
