package com.zorroa.archivist.crawlers;

import com.zorroa.archivist.sdk.filesystem.ObjectFileSystem;
import com.zorroa.archivist.sdk.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;

/**
 * Created by chambers on 2/1/16.
 */
public class FileCrawler extends AbstractCrawler {

    public FileCrawler(ObjectFileSystem objectFileSystem) {
        super(objectFileSystem);
    }

    @Override
    public void start(URI uri, Consumer<File> consumer) throws IOException {
        Path start = new File(uri).toPath();

        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                    throws IOException {

                final File file = path.toFile();

                if (!file.isFile()) {
                    return FileVisitResult.CONTINUE;
                }

                if (path.getFileName().toString().startsWith(".")) {
                    return FileVisitResult.CONTINUE;
                }

                if (!targetFileFormats.contains(FileUtils.extension(path).toLowerCase())
                        && !targetFileFormats.isEmpty()) {
                    return FileVisitResult.CONTINUE;
                }

                if (ignoredPaths.contains(file.getAbsolutePath())) {
                    return FileVisitResult.CONTINUE;
                }
                consumer.accept(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
