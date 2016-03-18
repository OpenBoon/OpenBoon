package com.zorroa.archivist.sdk.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    private static final Pattern URI_PATTERN = Pattern.compile("^\\w+://");

    public static boolean isURI(String path) {
        return URI_PATTERN.matcher(path).find();
    }

    public static List<String> superSplit(String path) {
        List<String> result = Lists.newArrayList();
        StringBuilder builder = new StringBuilder(path.length());
        builder.append("/");

        for (String e: Splitter.on("/").trimResults().omitEmptyStrings().split(path)) {
            builder.append(e);
            result.add(builder.toString());
            builder.append("/");
        }
        return result;
    }

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

    /**
     * Return the basename of a file file in a path.  The basename is the file name without
     * the file extension.
     *
     * In this example "file" is the base name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    public static String basename(String path) {
        String filename = filename(path);
        if (filename.contains(".")) {
            return filename.substring(0, filename.lastIndexOf("."));
        }
        else {
            return path;
        }
    }

    /**
     * Return the filenane of the file in a given path.
     *
     * In this example "file.ext" is the file name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    public static String filename(String path) {
        if (!path.contains("/")) {
            return path;
        }
        else {
            return path.substring(path.lastIndexOf("/") + 1);
        }
    }

    /**
     * Return the directory name portion of a path.
     *
     * In this example /test/example is the directory name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    public static String dirname(String path) {
        return path.substring(0, path.lastIndexOf("/"));
    }

    /**
     * Make all the given directories in the given path. If they already exist, return.
     *
     * @param path
     */
    public static void makedirs(String path) {
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    /**
     * Join string elements into file path.
     * @param e
     * @return
     */
    public static String join(String ... e) {
        return StringUtil.join("/", e);
    }

    private static final String UNITS = "KMGTPE";

    /**
     * Converts from human readable file size to numeric bytes.
     *
     * @return byte value
     */
    public static long readbleSizeToBytes(String value)
    {
        String identifier = value.substring(value.length()-1);
        int index = UNITS.indexOf(identifier);
        long number;

        if (index!=-1) {
            number = Long.parseLong(value.substring(0, value.length()-2));
            for (int i = 0; i <= index; i++) {
                number = number * 1000;
            }
        }
        else {
            number = Long.parseLong(value);
        }
        return number;
    }

    /**
     * Quietly close the given closable.
     *
     * @param c
     */
    public static final void close(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        }
        catch (IOException e) {
            // ignore
        }
    }
}
