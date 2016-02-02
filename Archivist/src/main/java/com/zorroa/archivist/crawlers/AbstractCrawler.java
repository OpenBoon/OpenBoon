package com.zorroa.archivist.crawlers;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.zorroa.archivist.service.ObjectFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by chambers on 1/28/16.
 */
public abstract class AbstractCrawler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * A place to store any files not local to the archivist.
     */
    protected  ObjectFileSystem fileSystem;

    /**
     * The file formats the crawler should target.
     */
    protected Set<String> targetFileFormats = Sets.newHashSet();

    /**
     * Paths that the crawler should skip over.
     */
    protected Set<String> ignoredPaths = Sets.newHashSet();

    /**
     * Starts the crawler.
     *
     * @param uri
     */
    public abstract void start(URI uri, Consumer<File> consumer) throws IOException;

    public AbstractCrawler(ObjectFileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public AbstractCrawler setTargetFileFormats(Collection<String> formats) {
        targetFileFormats = ImmutableSet.copyOf(formats);
        return this;
    }

    public Set<String> getTargetFileFormats() {
        return targetFileFormats;
    }

    public AbstractCrawler setIgnoredPaths(Set<String> ignoredPaths) {
        this.ignoredPaths = ignoredPaths;
        return this;
    }

    public Set<String> getIgnoredPaths() {
        return ignoredPaths;
    }
}
