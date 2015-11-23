package com.zorroa.archivist.sdk.processor.export;

import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ExportOutput;
import com.zorroa.archivist.sdk.domain.ExportedAsset;
import com.zorroa.archivist.sdk.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by chambers on 11/12/15.
 */
public class ZipFileExport extends ExportProcessor {

   /**
    * The ZipOutputStream is opened during init() and closed at teardown().  When process()
    * is called, the source file is added.
    */
    private ZipOutputStream zipFile = null;

    /**
     * The zip file's internal path.  By default we set this to the name of the
     * file, so when you unzip the file the contents appear in a directory
     * with the same name.
     */
    private String zipEntryPath;

    public String getMimeType() {
        return "application/zip";
    }

    public String getFileExtension() {
        return "zip";
    }

    @Override
    public void teardown() {
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (Exception e) {
                logger.warn("Failed to close zip file, {}" + e.getMessage(), e);
            }
        }
    }

    @Override
    public void init(Export export, ExportOutput output) throws Exception {

        if (zipFile != null) {
            // handle someone calling multiple times.
            logger.warn("The export processor '{}' is already initialized", this);
        }

        logger.info("Initializing zip file: '{}'", output.getPath());

        /*
         * If no entry path is specified as an argument, then the basename of of the directory
         * becomes the name.  This value is basically a directory in the zip, so the files
         * go into their own folder when unzipped.
         *
         * Setting this to an empty strin
         *
         */
        zipEntryPath = (String) args.getOrDefault("zipEntryPath", FileUtils.basename(output.getFileName()));
        zipFile = new ZipOutputStream(new FileOutputStream(output.getPath()));
    }


    @Override
    public void process(ExportedAsset asset) throws Exception {
        byte[] buffer = new byte[1024];

        logger.info("Adding {} to zip {}", asset.getCurrentPath(), zipEntryPath);

        ZipEntry ze = new ZipEntry(String.format("%s/%s", zipEntryPath, asset.getCurrentPath()));
        zipFile.putNextEntry(ze);

        FileInputStream stream = new FileInputStream(asset.getCurrentPath());
        int len;
        while((len = stream.read(buffer)) > 0) {
            zipFile.write(buffer, 0, len);
        }
        stream.close();
        zipFile.closeEntry();

    }
}

