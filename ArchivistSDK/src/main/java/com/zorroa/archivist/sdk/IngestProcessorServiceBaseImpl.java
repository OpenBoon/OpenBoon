/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by wex on 7/4/15.
 */
public class IngestProcessorServiceBaseImpl implements IngestProcessorService {
    private static ClassLoader classLoader = null;

    @Override
    public ClassLoader getSiteClassLoader() {
        if (classLoader == null) {
            // Create an array of URLs to search as a classpath when loading processors
            URL[] zorroaJarURLs = null;
            Map<String, String> env = System.getenv();
            String sitePath = env.get("ZORROA_SITE_PATH");
            if (sitePath == null) {
//                System.out.println("ZORROA_SITE_PATH is not set.");
                classLoader = AssetBuilder.class.getClassLoader();
            } else {
                File folder = new File(sitePath);
                if (!folder.exists()) {
//                    System.out.println("Invalid ZORROA_SITE_PATH: " + sitePath);
                    classLoader = AssetBuilder.class.getClassLoader();
                } else {
                    File[] listOfFiles = folder.listFiles();
                    ArrayList<URL> urls = new ArrayList<URL>();
                    for (int i = 0; i < listOfFiles.length; i++) {
                        if (listOfFiles[i].isFile()) {
                            String path = listOfFiles[i].getAbsolutePath();
                            try {
                                String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
                                if (ext.equals("jar")) {
                                    try {
                                        URL url = new File(path).toURI().toURL();
                                        urls.add(url);
                                    } catch (IOException e) {
                                        System.out.println("Cannot create URL to Zorroa jar file " + path);
                                        e.printStackTrace();
                                    }
                                }
                            } catch (java.lang.StringIndexOutOfBoundsException e) {
                            }
                        }
                    }
                    zorroaJarURLs = urls.toArray(new URL[urls.size()]);
                    classLoader = new URLClassLoader(zorroaJarURLs, AssetBuilder.class.getClassLoader());
                }
            }
        }
        return classLoader;
    }

    @Override
    public File getResourceFile(String path) {
        URL resourceUrl = getClass().getResource(path);
        try {
            Path resourcePath = Paths.get(resourceUrl.toURI());
            return new File(resourcePath.toUri());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public File getProxyFile(String filename, String extension) {
        File proxyFile = getResourceFile("/proxies");
        return new File(proxyFile.getAbsoluteFile() + "/" + filename + "." + extension);
    }

}
