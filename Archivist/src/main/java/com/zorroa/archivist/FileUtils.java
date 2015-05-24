package com.zorroa.archivist;

import java.nio.file.Path;

public class FileUtils {

    public static String extension(String path) {
        try {
            return path.substring(path.lastIndexOf('.')+1).toLowerCase();
        }
        catch (IndexOutOfBoundsException ignore) {
            //
        }
        return "";
    }

    public static String extension(Path path) {
        return FileUtils.extension(path.toString());
    }
}
