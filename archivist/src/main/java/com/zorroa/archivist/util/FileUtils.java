package com.zorroa.archivist.util;


import org.apache.tika.Tika;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.regex.Pattern;

public class FileUtils {

    private static final Pattern URI_PATTERN = Pattern.compile("^\\w+://");

    private static final Tika tika = new Tika();

    public static String getMediaType(String path) {
        return tika.detect(path);
    }

    public static String getMediaType(Path path) {
        return getMediaType(path.toString());
    }

    public static String getMediaType(File path) {
        return getMediaType(path.getAbsolutePath());
    }


    public static final String getHostname() {
        String hostname = System.getenv("HOSTNAME");
        boolean ipAddr = false;
        if (hostname == null) {
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (Exception ignore1) {
                try {
                    hostname = InetAddress.getLocalHost().getHostAddress();
                    ipAddr = true;
                } catch (Exception ignore2) {

                }
            }
        }
        if (hostname == null) {
            hostname = "UnknownHost";
        }

        if (ipAddr) {
            return hostname;
        }
        else {
            return hostname.split("\\.")[0];
        }
    }

    /**
     * Converts a URI or file path to a URI.
     *
     * @param path
     * @return
     */
    public static URI toUri(String path) {
        if (!path.startsWith("/")) {
            return URI.create(path);
        }
        else {
            return new File(path).toURI();
        }
    }

    /**
     * Return true of the giving sting is a URI.
     *
     * @param path
     * @return
     */
    public static boolean isURI(String path) {
        return URI_PATTERN.matcher(path).find();
    }

    /**
     * Returns an absolute normalized path for the given file path.
     *
     * @param path
     * @return
     */
    public static String normalize(String path) {
        return path == null ? null : normalize(Paths.get(path)).toString();
    }

    /**
     * Returns an absolute normalized path for the given file path.
     *
     * @param path
     * @return
     */
    public static File normalize(File path) {
        return path == null ? null : normalize(path.toPath()).toFile();
    }

    /**
     * Returns an absolute normalized path for the given file path.
     *
     * @param path
     * @return
     */
    public static Path normalize(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }

    /**
     * Return the file extension for the given path.
     *
     * @param path
     * @return
     */
    public static String extension(String path) {
        try {
            return path.substring(path.lastIndexOf('.')+1).toLowerCase();
        }
        catch (IndexOutOfBoundsException ignore) {
            //
        }
        return "";
    }

    /**
     * Return the file extension for the given path.
     *
     * @param path
     * @return
     */
    public static String extension(File path) {
        return FileUtils.extension(path.getName());
    }

    /**
     * Return the file extension for the given path.
     *
     * @param path
     * @return
     */
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
     * Return the basename of a file file in a path.  The basename is the file name without
     * the file extension.
     *
     * In this example "file" is the base name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    public static String basename(File path) {
        return FileUtils.basename(path.getName());
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
    public static String basename(Path path) {
        return basename(path.toString());
    }

    /**
     * Return the filename of the file in a given path.
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
     * Return the filename of the file in a given path.
     *
     * In this example "file.ext" is the file name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    public static String filename(File path) {
        return path.getName();
    }

    /**
     * Return the filename of the file in a given path.
     *
     * In this example "file.ext" is the file name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    public static String filename(Path path) {
        return filename(path.toString());
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
     * Return the directory name portion of a path.
     *
     * In this example /test/example is the directory name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    public static String dirname(File path) {
        return dirname(path.getAbsolutePath());
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
    public static String dirname(Path path) {
        return dirname(path.toAbsolutePath().toString());
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
     * Make all the given directories in the given path. If they already exist, return.
     *
     * @param path
     */
    public static void makedirs(Path path) {
        File f = path.toFile();
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    private static final String UNITS = "KMGTPE";

    /**
     * Converts from human readable file size to numeric bytes.
     *
     * @return byte value
     */
    public static long displaySizeToByteCount(String value)
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
     * Returns a human readable display for a number of bytes.
     *
     * @param bytes
     * @return
     */
    public static String byteCountToDisplaySize(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = "kMGTPE".charAt(exp-1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
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

    /**
     * Look for a particular version of a file.  Expects versions
     * to end with _X.ext
     *
     * @return
     */
    public static File findVersion(String path, int start, int max) {

        File original = new File(path);
        if (original.exists()) {
            return original;
        }

        String ext = FileUtils.extension(path);
        for (int i=start; i<max; i++) {
            String newPath = path.replace("." + ext, String.format("_%d.%s", i, ext));
            File newFile = new File(newPath);
            if (newFile.exists()) {
                return newFile;
            }
        }
        return null;
    }
    /**
     * Recursively delete a directory.
     *
     * @param file
     * @return
     * @throws FileNotFoundException
     */
    public static boolean deleteRecursive(File file, org.slf4j.Logger logger) {
        return deleteRecursive(file.toPath(), logger);
    }

    /**
     * Recursively delete a directory.
     *
     * @param path
     * @return
     * @throws FileNotFoundException
     */
    public static boolean deleteRecursive(Path path, org.slf4j.Logger logger) {
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .peek(f-> { if (logger != null) { logger.info("removing: {}", f); }})
                    .forEach(File::delete);
        } catch (IOException e) {
            logger.warn("Failed to delete {}", path, e);
            return false;
        }
        return true;
    }
}
