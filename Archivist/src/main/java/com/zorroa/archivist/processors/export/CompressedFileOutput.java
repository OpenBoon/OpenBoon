package com.zorroa.archivist.processors.export;

import com.zorroa.archivist.FileUtils;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.processor.export.Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CompressedFileOutput extends ExportProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CompressedFileOutput.class);

    @Value("${archivist.export.outputPath}")
    String defaultOutputPath;

    public final Port<String> inputPath;
    public final Port<String> zipFileName;
    public final Port<String> zipEntryPath;
    public final Port<String> outputPath;

    private ZipOutputStream zipFile = null;

    public CompressedFileOutput() {
        inputPath = new Port<>("inputPath", Port.Type.Input, this);
        zipFileName = new Port<>("zipFileName", "archivist_export_%{export:id}.zip", Port.Type.Input, this);
        zipEntryPath = new Port<>("zipEntryPath", "/", Port.Type.Input, this);
        outputPath = new Port<>("outputPath", Port.Type.Output, this);
    }

    @Override
    public void process() throws Exception {

        if (zipFile == null) {
            outputPath.setValue(FileUtils.pathjoin(defaultOutputPath, zipFileName.getValue()));
            FileUtils.makedirs(FileUtils.dirname(outputPath.getValue()));
            zipFile = new ZipOutputStream(new FileOutputStream(outputPath.getValue()));
        }

        byte[] buffer = new byte[1024];
        for (String input: inputPath.getValues()) {

            ZipEntry ze = new ZipEntry(String.format("%s/%s", zipEntryPath.getValue(), FileUtils.filename(input)));
            zipFile.putNextEntry(ze);

            FileInputStream stream = new FileInputStream(input);
            int len;
            while((len = stream.read(buffer)) > 0) {
                zipFile.write(buffer, 0, len);
            }
            stream.close();
            zipFile.closeEntry();
        }
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
}