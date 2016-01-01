package com.zorroa.archivist.sdk.domain;

import com.zorroa.archivist.sdk.schema.SourceSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Represents an asset that is in the process of or has been exported.
 */
public class ExportedAsset {

    private static final Logger logger = LoggerFactory.getLogger(ExportedAsset.class);

    private final Asset asset;
    private final ExportOutput exportOutput;
    private final Export export;

    private final LinkedList<String> paths = new LinkedList();

    public ExportedAsset(Export export, ExportOutput exportOutput, Asset asset) {
        this.asset = asset;
        this.export = export;
        this.exportOutput = exportOutput;
        addPath(asset.getSchema("source", SourceSchema.class).getPath());
    }

    /**
     * Returns the new path where an exported asset should be written.  If the source asset needs
     * to be modified in any way, this function will return a location where the modified asset
     * can be written to.
     *
     * @param basename
     * @param ext
     * @return
     */
    public String nextPath(String basename, String ext) {
        String dst = String.join("/", exportOutput.getDirName(), String.format("%s.%s", basename, ext));
        addPath(dst);
        return dst;
    }

    public File getCurrentFile() {
        return new File(getCurrentPath());
    }

    public String getCurrentPath() {
        return paths.getFirst();
    }

    public Asset getAsset() {
        return asset;
    }

    public ExportOutput getExportOutput() {
        return exportOutput;
    }

    /**
     * Prepends a path to the LIFO list.  The head of the list is the current
     * working file.
     *
     * @param path
     */
    private void addPath(String path) {
        this.paths.addFirst(path);
    }
}
