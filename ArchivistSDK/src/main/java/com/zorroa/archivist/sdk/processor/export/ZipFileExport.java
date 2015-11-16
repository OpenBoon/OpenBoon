package com.zorroa.archivist.sdk.processor.export;

import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ExportOutput;
import com.zorroa.archivist.sdk.util.FileUtils;

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
    public void init(Export export, ExportOutput output, String outputDir) throws Exception {
        if (zipFile != null) {
            // handle someone calling multiple times.
            logger.warn("The export processor '{}' is already initialized", this);
            return;
        }


        zipEntryPath = FileUtils.filename(outputDir);

        String filename = String.format("%s/%s.zip", outputDir, zipEntryPath);
        logger.info("Initializing zip file: '{}'", filename);
        zipFile = new ZipOutputStream(new FileOutputStream(filename));
    }


    @Override
    public void process(Asset asset) throws Exception {
        byte[] buffer = new byte[1024];

        logger.info("Adding {} to zip {}", asset.getValue("source.path"), zipEntryPath);

        ZipEntry ze = new ZipEntry(String.format("%s/%s", zipEntryPath, asset.getValue("source.filename")));
        zipFile.putNextEntry(ze);

        FileInputStream stream = new FileInputStream((String)asset.getValue("source.path"));
        int len;
        while((len = stream.read(buffer)) > 0) {
            zipFile.write(buffer, 0, len);
        }
        stream.close();
        zipFile.closeEntry();
    }
}

